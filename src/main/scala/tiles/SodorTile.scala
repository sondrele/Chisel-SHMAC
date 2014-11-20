package tiles

import Chisel._
import Common.{MemPortIo, HTIFIO, SodorConfiguration}
import router._
import Sodor._

class SodorTileIO(numPorts: Int) extends RouterIO(numPorts) {
  implicit val sodorConf = SodorConfiguration()
  val host = new HTIFIO()
  val reqReady = Bool(INPUT)
}

class SodorTile(x: Int, y: Int, numPorts: Int, numRecords: Int) extends Module {

  val io = new SodorTileIO(numPorts)

  val router = Module(new Router(x, y, numPorts + 1, numRecords)).io
  for (i <- 0 until numPorts) {
    io.ports(i) <> router.ports(i)
  }

  val unit = Module(new ShmacUnit())
  unit.io.host <> io.host

  val localPort = router.ports(numPorts)
  val packet = localPort.out.bits
  val address = packet.header.address
  val payload = packet.payload
  val isResponse = packet.header.reply

  localPort.out.ready := unit.io.mem.resp.ready
  unit.io.mem.resp.valid := localPort.out.valid && isResponse
  unit.io.mem.resp.bits.addr := address
  unit.io.mem.resp.bits.data := payload

  val outPacket = new Packet()
  outPacket.sender.y := UInt(y, width = 4)
  outPacket.sender.x := UInt(x, width = 4)

  // The instuction memory is currently hard coded to be at tile (2, 1)
  when (unit.io.mem.req.valid
    && unit.io.mem.req.bits.fcn === UInt(0, width = 1)
    && unit.io.mem.req.bits.typ === UInt(7, width = 3)
  ) {
    outPacket.dest.y := UInt(1, width = 4)
    outPacket.dest.x := UInt(2, width = 4)
  }.otherwise {
    outPacket.dest.y := UInt(0, width = 4)
    outPacket.dest.x := UInt(0, width = 4)
  }

  outPacket.payload := unit.io.mem.req.bits.data
  outPacket.header.writeReq := Bool(true)
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
