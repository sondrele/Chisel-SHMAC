package test

import Chisel._
import router._

class OutputPortTest(p: OutputPort) extends Tester(p) {
  def testFifoIntegration(): Unit = {
    poke(p.io.fifo.in.valid, 1)
    expect(p.io.fifo.in.ready, 1)
    poke(p.io.fifo.in.bits.payload(0), 10)
    step(1)
    poke(p.io.fifo.in.bits.payload(1), 15)
    expect(p.io.fifo.out.bits.payload(0), 10)
    step(1)
    expect(p.io.fifo.in.ready, 0)
    poke(p.io.fifo.out.ready, 1)
    expect(p.io.fifo.out.bits.payload(0), 10)
    step(1)
    expect(p.io.fifo.out.bits.payload(1), 15)
  }

  testFifoIntegration()
}
