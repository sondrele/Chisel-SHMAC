package main.scala.tiles

import Chisel._
import main.scala.memory.Ram
import main.scala.router.{Packet, Router, RouterIO}
import router._

class RamTileIO(numPorts: Int) extends RouterIO(numPorts)

class RamTile(x: Int, y: Int, numPorts: Int, numRecords: Int, memDepth: Int) extends Module {
  val io = new RamTileIO(numPorts)

  val router = Module(new Router(x, y, numPorts + 1, numRecords)).io
  for (i <- 0 until numPorts) {
    io.ports(i) <> router.ports(i)
  }

  val localPort = router.ports(numPorts)
  val packet = localPort.out.bits
  val address = packet.header.address
  val payload = packet.payload
  val isWrite = packet.header.writeReq
  val isRead = !isWrite

  val ram = Module(new Ram(depth = memDepth, dataWidth = Packet.DATA_WIDTH)).io

  ram.reads.valid := localPort.out.valid && isRead
  ram.reads.bits.address := address

  ram.writes.valid := localPort.out.valid && isWrite
  ram.writes.bits.address := address
  ram.writes.bits.data := payload.toBits()

  localPort.out.ready := ram.reads.ready || ram.writes.ready

  val outPacket = new Packet()
  outPacket.assign(packet)
  outPacket.header.reply := ram.out.valid
  outPacket.payload := ram.out.bits

  ram.out.ready := localPort.in.ready
  localPort.in.valid := ram.out.valid
  localPort.in.bits.assign(outPacket)
}
