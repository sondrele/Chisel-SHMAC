package router

import Chisel._

class InputPort(n: Int) extends Module {
  val io = new Bundle {
    val fifo = new DecoupledFifoIO(PacketData())

    val xDest = UInt(OUTPUT, width = 4)
    val yDest = UInt(OUTPUT, width = 4)
  }

  io.fifo <> Module(new Fifo(PacketData(), n)).io

  val packet = io.fifo.out.bits
  when (packet.isReply) {
    io.xDest := packet.xSender
    io.yDest := packet.ySender
  }.otherwise {
    io.xDest := packet.xDest
    io.yDest := packet.yDest
  }
}
