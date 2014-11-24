package main.scala.tiles

import Chisel._
import Common.{MemoryOpConstants, HTIFIO, SodorConfiguration}
import main.scala.router.{Local, Packet, Router}
import Sodor._

case class SodorTileConfig(tile: TileLoc, imem: TileLoc, dmem: TileLoc) extends TileConfig

class SodorTileIO(numPorts: Int) extends TileIO(numPorts) {
  implicit val sodorConf = SodorConfiguration()
  val host = new HTIFIO()
}

class SodorTile(implicit conf: SodorTileConfig) extends Tile with MemoryOpConstants {
  val io = new SodorTileIO(conf.ioPorts)

  val router = Module(new Router(conf.tile, conf.ioPorts + 1, conf.fifoSize)).io
  for (i <- 0 until conf.ioPorts) {
    io.ports(i) <> router.ports(i)
  }

  val unit = Module(new SodorUnit())
  unit.io.host <> io.host

  def isImemRequest: Bool = {
    unit.io.mem.req.valid && unit.io.mem.req.bits.fcn === M_XRD && unit.io.mem.req.bits.typ === MT_WU
  }

  def isDmemRequest: Bool = {
    unit.io.mem.req.valid && unit.io.mem.req.bits.typ === MT_W
  }

  val localPort = router.ports(Local.index)
  val packet = localPort.out.bits
  val address = packet.header.address
  val payload = packet.payload
  val isResponse = packet.header.reply

  // Set to true when the processor is writing to dmem, in order for it not to
  // stall and wait for a write-response which it will never receive
  val writeValidDmemReg = Reg(Bool(), next = Bool(false))
  // Is used to stall the processor from issuing a read-request for the next
  // instruction when it is waiting for data
  val waitingForDmemReg = Reg(Bool(), init = Bool(false))
  // Used in combination with waitingForDmemReg. Makes it possible for the
  // processor to read its Tile-location
  val readingTileLocReg = Reg(Bool(), init = Bool(false))
  val tileLocReg = Reg(UInt(), init = Cat(UInt(conf.tile.x), UInt(conf.tile.y)))

  localPort.out.ready := unit.io.mem.resp.ready
  unit.io.mem.resp.valid := (localPort.out.valid && isResponse) || writeValidDmemReg
  unit.io.mem.resp.bits.addr := address
  unit.io.mem.resp.bits.data := payload

  val outPacket = new Packet()
  outPacket.sender.y := UInt(conf.tile.y, width = 4)
  outPacket.sender.x := UInt(conf.tile.x, width = 4)

  outPacket.header.writeReq := unit.io.mem.req.bits.fcn

  outPacket.payload := unit.io.mem.req.bits.data
  outPacket.header.address := unit.io.mem.req.bits.addr

  localPort.in.valid := unit.io.mem.req.valid
  localPort.in.bits.assign(outPacket)

  // Set the destination of the packet, based on the kind of memory-request
  when (isImemRequest) {
    outPacket.dest.y := UInt(conf.imem.y, width = 4)
    outPacket.dest.x := UInt(conf.imem.x, width = 4)
  }.elsewhen (isDmemRequest) {
    // Read request
    when (unit.io.mem.req.bits.fcn === M_XRD) {
      waitingForDmemReg := Bool(true)
    }
    // Tile location read request
    when (unit.io.mem.req.bits.fcn === M_XRD && unit.io.mem.req.bits.addr === UInt(0x1)) {
      waitingForDmemReg := Bool(true)
      readingTileLocReg := Bool(true)
      // Don't send this packet off-tile
      localPort.in.valid := Bool(false)
    }
    // Write request
    when (unit.io.mem.req.bits.fcn === M_XWR) {
      writeValidDmemReg := Bool(true)
    }
    outPacket.dest.y := UInt(conf.dmem.y, width = 4)
    outPacket.dest.x := UInt(conf.dmem.x, width = 4)
  }.otherwise {
    outPacket.dest.y := UInt(0, width = 4)
    outPacket.dest.x := UInt(0, width = 4)
  }

  // When the processor is waiting for data after a dmem read request it will consume
  // the next package as data if mem.req.ready is 1
  // This fix stalls the processor until the first packet has arrived
  when (waitingForDmemReg) {
    unit.io.mem.req.ready := Bool(false)
    when (localPort.out.valid) {
      waitingForDmemReg := Bool(false)
    }
    when (readingTileLocReg) {
      readingTileLocReg := Bool(false)
      waitingForDmemReg := Bool(false)

      unit.io.mem.resp.valid := Bool(true)
      unit.io.mem.resp.bits.addr := UInt(0x1)
      unit.io.mem.resp.bits.data := tileLocReg
    }
  }.otherwise {
    unit.io.mem.req.ready := localPort.in.ready
  }

}
