package main.scala.tiles

import Chisel._
import main.scala.memory.Ram
import main.scala.router.{Local, Packet, Router}

case class RamTileConfig(tile: TileLoc, memDepth: Int = 0x4000) extends TileConfig

class RamTileIO(numPorts: Int) extends TileIO(numPorts)

class RamTile(implicit conf: RamTileConfig) extends Tile {
  val io = new RamTileIO(conf.ioPorts)

  val router = Module(new Router(conf.tile, conf.ioPorts + 1, conf.fifoSize)).io
  for (i <- 0 until conf.ioPorts) {
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
