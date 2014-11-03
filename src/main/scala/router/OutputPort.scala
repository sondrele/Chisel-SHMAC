package router

import Chisel._

class OutputPort(n: Int) extends Module {
  val io = new Bundle {
    val fifo = new QueueIO(new PacketBundle(), n)
  }

  io.fifo.deq <> Queue(io.fifo.enq, n)
}
