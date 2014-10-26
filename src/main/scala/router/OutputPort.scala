package router

import Chisel._

class OutputPort(n: Int) extends Module {
  val io = new Bundle {
    val fifo = new DecoupledFifoIO(PacketData())
  }

  io.fifo <> Module(new Fifo(PacketData(), n)).io
}
