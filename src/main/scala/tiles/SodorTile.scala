package tiles

import Chisel._
import Common.{MemoryOpConstants, MemPortIo, HTIFIO, SodorConfiguration}
import router._
import Sodor._

class SodorTileIO(numPorts: Int) extends RouterIO(numPorts) {
  implicit val sodorConf = SodorConfiguration()
  val host = new HTIFIO()
  val reqReady = Bool(INPUT)
}

class SodorTile(x: Int, y: Int, numPorts: Int, numRecords: Int) extends Module with MemoryOpConstants {

  val io = new SodorTileIO(numPorts)

  val router = Module(new Router(x, y, numPorts + 1, numRecords)).io
  for (i <- 0 until numPorts) {
    io.ports(i) <> router.ports(i)
  }

  val unit = Module(new ShmacUnit())
  unit.io.host <> io.host

  def isImemRequest: Bool = {
    unit.io.mem.req.valid && unit.io.mem.req.bits.fcn === M_XRD && unit.io.mem.req.bits.typ === MT_WU
  }

  def isDmemRequest: Bool = {
    unit.io.mem.req.valid && unit.io.mem.req.bits.fcn === M_XWR && unit.io.mem.req.bits.typ === MT_W
  }

  val localPort = router.ports(numPorts)
  val packet = localPort.out.bits
  val address = packet.header.address
  val payload = packet.payload
  val isResponse = packet.header.reply
  val writeValidReg = Reg(Bool(), next=Bool(false))

  localPort.out.ready := unit.io.mem.resp.ready
  unit.io.mem.resp.valid := (localPort.out.valid && isResponse) || writeValidReg
  unit.io.mem.resp.bits.addr := address
  unit.io.mem.resp.bits.data := payload

  val outPacket = new Packet()
  outPacket.sender.y := UInt(y, width = 4)
  outPacket.sender.x := UInt(x, width = 4)

  // The instruction memory is currently hard coded to be at tile (2, 1)
  when (isImemRequest) {
    outPacket.dest.y := UInt(1, width = 4)
    outPacket.dest.x := UInt(2, width = 4)
  }.elsewhen (isDmemRequest) {
    writeValidReg := Bool(true)
    outPacket.dest.y := UInt(0, width = 4)
    outPacket.dest.x := UInt(0, width = 4)
  }.otherwise {
    outPacket.dest.y := UInt(0, width = 4)
    outPacket.dest.x := UInt(0, width = 4)
  }

  outPacket.header.writeReq := unit.io.mem.req.bits.fcn

  outPacket.payload := unit.io.mem.req.bits.data
  outPacket.header.address := unit.io.mem.req.bits.addr

  unit.io.mem.req.ready := localPort.in.ready && io.reqReady
  localPort.in.valid := unit.io.mem.req.valid
  localPort.in.bits.assign(outPacket)
}

object elaborate {
  def main(args: Array[String]): Unit = {
    chiselMain(args, () => Module(new SodorTile(1, 1, 4, 1)))
  }
}
