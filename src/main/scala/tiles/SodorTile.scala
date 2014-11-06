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

  val sodor = Module(new ShmacUnit()).io
  sodor.host <> io.sodor.host

  val localPort = router.ports(numPorts)
  val packet = localPort.out.bits
  val address = packet.header.address
  val payload = packet.payload
  val isReply = packet.header.reply

  // localPort.out.valid
  // localPort.out.ready := ram.reads.ready || ram.writes.ready

  // val outPacket = new Packet()
  // outPacket.assign(packet)
  // outPacket.header.reply := ram.out.valid
  // outPacket.payload := ram.out.bits

  // ram.out.ready := localPort.in.ready
  // localPort.in.valid := ram.out.valid
  // localPort.in.bits.assign(outPacket)
}
