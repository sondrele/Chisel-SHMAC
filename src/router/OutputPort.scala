package router

import Chisel._
import utils.{Fifo, FifoIO}

class OutputPort(n: Int) extends Module {
  val io = new Bundle {
    val fifo = new FifoIO()
  }

  val queue = Module(new Fifo(n))
  io.fifo <> queue.io
}

class OutputPortTest(p: OutputPort) extends Tester(p) {

  def testFifoIntegration() = {
    poke(p.io.fifo.write, 1)
    expect(p.io.fifo.canWrite, 1)
    poke(p.io.fifo.inData, 10)
    step(1)
    poke(p.io.fifo.inData, 100)
    expect(p.io.fifo.outData, 10)
    step(1)
    expect(p.io.fifo.canWrite, 0)
    poke(p.io.fifo.read, 1)
    expect(p.io.fifo.outData, 10)
    step(1)
    expect(p.io.fifo.outData, 100)
  }

  testFifoIntegration()
}
