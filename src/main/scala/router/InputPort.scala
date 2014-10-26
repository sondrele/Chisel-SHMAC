package router

import Chisel._

class InputPort(n: Int) extends Module {
  val io = new Bundle {
    val fifo = new DecoupledFifoIO(PacketData())

    val xDest     = UInt(OUTPUT, width = 4)
    val yDest     = UInt(OUTPUT, width = 4)
  }

  io.fifo <> Module(new Fifo(PacketData(), n)).io

  io.xDest := io.fifo.out.bits.xDest
  io.yDest := io.fifo.out.bits.yDest
}
