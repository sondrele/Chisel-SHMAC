package tiles

import Chisel._
import router._

class RamTileIO(numPorts: Int) extends RouterIO(numPorts)

class RamTile(x: Int, y: Int, numPorts: Int, numRecords: Int, memDepth: Int) extends Module {
  val io = new RamTileIO(numPorts)

  val router = Module(new Router(x, y, numPorts + 1, numRecords)).io
  for (i <- 0 until numPorts) {
    io.ports(i) <> router.ports(i)
  }

  val localPort = router.ports(numPorts)
  val packet = localPort.outData
  val address = packet.header.address
  val payload = packet.payload
  val isWrite = packet.header.writeReq
  val isRead = !isWrite

  val ram = Module(new Ram(depth = memDepth, dataWidth = 128)).io

  ram.reads.valid := localPort.outRequest && isRead
  ram.reads.bits.address := address

  ram.writes.valid := localPort.outRequest && isWrite
  ram.writes.bits.address := address
  ram.writes.bits.data := payload.toBits()

  localPort.outReady := ram.reads.ready || ram.writes.ready

  val outPacket = new Packet()
  outPacket.assign(packet)
  outPacket.header.reply := ram.out.valid
  outPacket.payload := ram.out.bits

  ram.out.ready := localPort.inReady
  localPort.inRequest := ram.out.valid
  localPort.inData.assign(outPacket)
}
