package tiles

import Chisel._
import router._

class RamTileIO(numPorts: Int) extends RouterIO(numPorts)

class RamTile(x: Int, y: Int, numPorts: Int, numRecords: Int) extends Module {
  val io = new RamTileIO(numPorts)

  val router = Module(new Router(x, y, numPorts + 1, numRecords)).io
  for (i <- 0 until numPorts) {
    io.ports(i) <> router.ports(i)
  }

  val localPort = router.ports(numPorts)
  val data = localPort.outData
  val address = data.address
  val payload = data.payload
  val isWrite = data.isWriteReq
  val isRead = !isWrite

  val ram = Module(new Ram(depth = 8, dataWidth = 128)).io

  ram.reads.valid := localPort.outRequest && isRead
  ram.reads.bits.address := address

  ram.writes.valid := localPort.outRequest && isWrite
  ram.writes.bits.address := address
  ram.writes.bits.data := payload

  localPort.outReady := ram.reads.ready && ram.writes.ready

  val outPacket = PacketData.create(payload = ram.out.bits.toBits().toUInt().litValue().toInt)

  ram.out.ready := localPort.inReady
  localPort.inRequest := ram.out.valid
  localPort.inData := outPacket
}
