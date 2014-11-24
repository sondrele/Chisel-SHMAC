package main.scala.tiles

import Chisel._
import main.scala.memory.Ram
import main.scala.router.{Local, Packet, Router, RouterIO}

case class RamTileConfig(memDepth: Int = 0x4000, fifoSize: Int = 4)

class RamTileIO(numPorts: Int) extends RouterIO(numPorts)

class RamTile(tile: TileLoc)(implicit conf: RamTileConfig) extends Module {
  val numIOPorts = 4
  val io = new RamTileIO(numIOPorts)

  val router = Module(new Router(tile.x, tile.y, numIOPorts + 1, conf.fifoSize)).io
  for (i <- 0 until numIOPorts) {
    io.ports(i) <> router.ports(i)
  }

  val localPort = router.ports(Local.index)
  val packet = localPort.out.bits
  val address = packet.header.address
  val payload = packet.payload
  val isWrite = packet.header.writeReq
  val isRead = !isWrite

  val ram = Module(new Ram(depth = conf.memDepth, dataWidth = Packet.DATA_WIDTH)).io

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
