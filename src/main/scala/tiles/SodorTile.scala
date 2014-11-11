package tiles

import Chisel._
import Common.SodorConfiguration
import router._
import Sodor._

class SodorTileIO(numPorts: Int) extends RouterIO(numPorts) {
  implicit val sodorConf = SodorConfiguration()
  val sodor = new ShmacIo()
}

class SodorTile(x: Int, y: Int, numPorts: Int, numRecords: Int) extends Module {

  val io = new SodorTileIO(numPorts)

  val router = Module(new Router(x, y, numPorts + 1, numRecords)).io
  for (i <- 0 until numPorts) {
    io.ports(i) <> router.ports(i)
  }

  val sodor = Module(new ShmacUnit())
  sodor.io.host <> io.sodor.host

  val localPort = router.ports(numPorts)
  val packet = localPort.out.bits
  val address = packet.header.address
  val payload = packet.payload
  val isResponse = packet.header.reply

  localPort.out.ready := sodor.io.mem.resp.ready
  sodor.io.mem.resp.valid := localPort.out.valid && isResponse
  sodor.io.mem.resp.bits.addr := address
  sodor.io.mem.resp.bits.data := payload

  val outPacket = new Packet()
  outPacket.sender.y := UInt(y)
  outPacket.sender.x := UInt(x)
  outPacket.payload := sodor.io.mem.req.bits.data
  outPacket.header.writeReq := Bool(true)
  outPacket.header.address := sodor.io.mem.req.bits.addr

  sodor.io.mem.req.ready := localPort.in.ready
  localPort.in.valid := sodor.io.mem.req.valid
  localPort.in.bits.assign(outPacket)
}

object elaborate {
  def main(args: Array[String]): Unit = {
    chiselMain(args, () => Module(new SodorTile(1, 1, 4, 1)))
  }
}
