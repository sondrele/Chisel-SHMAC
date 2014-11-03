package router

import Chisel._

class InputPort(n: Int) extends Module {
  val io = new Bundle {
    val fifo = new QueueIO(new PacketBundle(), n)

    val xDest = UInt(OUTPUT, width = 4)
    val yDest = UInt(OUTPUT, width = 4)
  }

  io.fifo.deq <> Queue(io.fifo.enq, n)

  val packet = io.fifo.deq.bits
  when (packet.header.reply) {
    io.xDest := packet.sender.x
    io.yDest := packet.sender.y
  }.otherwise {
    io.xDest := packet.dest.x
    io.yDest := packet.dest.y
  }
}
