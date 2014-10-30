package router

import Chisel._

class OutputPort(n: Int) extends Module {
  val io = new Bundle {
    val fifo = new DecoupledFifoIO(new PacketBundle())
  }

  io.fifo.out <> Queue(io.fifo.in, n)
}
