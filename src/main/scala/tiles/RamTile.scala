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

  // TODO: Take packet-reply into consideration, and create a response-packet
  // that copies request-packet, but changes reply-fields etc.

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

  localPort.outReady := ram.reads.ready || ram.writes.ready

  val ramData = ram.out.bits

  // val outPacket = PacketData.update(payload = ram.out.bits)

  val outPacket = Cat(
    data.yDest,
    data.xDest,
    data.ySender,
    data.xSender,
    ramData,
    data.isError,
    data.isExop,
    data.writeMask,
    data.isWriteReq,
    Bool(true), // isReply - only if there's a read-request
    data.address
  )

  ram.out.ready := localPort.inReady
  localPort.inRequest := ram.out.valid
  localPort.inData := outPacket
}
