package router

import Chisel._
import main.scala.router.OutputPort

class OutputPortTest(p: OutputPort) extends Tester(p) {
  def testFifoIntegration(): Unit = {
    poke(p.io.fifo.enq.valid, 1)
    expect(p.io.fifo.enq.ready, 1)
    poke(p.io.fifo.enq.bits.payload, 10)
    step(1)
    poke(p.io.fifo.enq.bits.payload, 15)
    expect(p.io.fifo.deq.bits.payload, 10)
    step(1)
    expect(p.io.fifo.enq.ready, 0)
    poke(p.io.fifo.deq.ready, 1)
    expect(p.io.fifo.deq.bits.payload, 10)
    step(1)
    expect(p.io.fifo.deq.bits.payload, 15)
  }

  testFifoIntegration()
}
