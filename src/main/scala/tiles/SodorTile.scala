package main.scala.tiles

import Chisel._
import Common.{MemoryOpConstants, HTIFIO, SodorConfiguration}
import main.scala.router.{Local, Packet, Router, RouterIO}
import Sodor._

case class SodorTileConfig(imem: TileLoc, dmem: TileLoc, fifoSize: Int = 4)

class SodorTileIO(numPorts: Int) extends RouterIO(numPorts) {
  implicit val sodorConf = SodorConfiguration()
  val host = new HTIFIO()
}

class SodorTile(tile: TileLoc)(implicit conf: SodorTileConfig) extends Module with MemoryOpConstants {
  val numIOPorts = 4
  val io = new SodorTileIO(numIOPorts)

  val router = Module(new Router(tile.x, tile.y, numIOPorts + 1, conf.fifoSize)).io
  for (i <- 0 until numIOPorts) {
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

  val writeValidDmemReg = Reg(Bool(), next = Bool(false))
  val waitingForDmemReg = Reg(Bool(), init = Bool(false))

  localPort.out.ready := unit.io.mem.resp.ready
  unit.io.mem.resp.valid := (localPort.out.valid && isResponse) || writeValidDmemReg
  unit.io.mem.resp.bits.addr := address
  unit.io.mem.resp.bits.data := payload

  val outPacket = new Packet()
  outPacket.sender.y := UInt(tile.y, width = 4)
  outPacket.sender.x := UInt(tile.x, width = 4)

  when (isImemRequest) {
    outPacket.dest.y := UInt(conf.imem.y, width = 4)
    outPacket.dest.x := UInt(conf.imem.x, width = 4)
  }.elsewhen (isDmemRequest) {
    when (unit.io.mem.req.bits.fcn === M_XRD) {
      waitingForDmemReg := Bool(true)
    }
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
  }.otherwise {
    unit.io.mem.req.ready := localPort.in.ready
  }

  outPacket.header.writeReq := unit.io.mem.req.bits.fcn

  outPacket.payload := unit.io.mem.req.bits.data
  outPacket.header.address := unit.io.mem.req.bits.addr

  localPort.in.valid := unit.io.mem.req.valid
  localPort.in.bits.assign(outPacket)
}
