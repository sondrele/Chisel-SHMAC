package router

import Chisel._

class InputPort(n: Int) extends Module {
  val io = new Bundle {
    val fifo = new DecoupledFifoIO(new PacketBundle())

    val xDest = UInt(OUTPUT, width = 4)
    val yDest = UInt(OUTPUT, width = 4)
  }

  io.fifo.out <> Queue(io.fifo.in, n)

  val packet = io.fifo.out.bits
  when (packet.header.reply) {
    io.xDest := packet.sender.x
    io.yDest := packet.sender.y
  }.otherwise {
    io.xDest := packet.dest.x
    io.yDest := packet.dest.y
  }
}
