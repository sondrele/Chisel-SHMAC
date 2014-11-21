package test

import Chisel._
import router._

class InputPortTest(p: InputPort) extends Tester(p) {

  def testFifoIntegration() {
    poke(p.io.fifo.enq.valid, 1)
    poke(p.io.fifo.enq.bits.header.address, 10)
    step(1)
    expect(p.io.fifo.deq.bits.header.address, 10)
  }

  def testInputPortRequest() {
    poke(p.io.fifo.deq.ready, 0)

    // Set signals to start reading
    poke(p.io.fifo.enq.valid, 0)
    poke(p.io.fifo.deq.ready, 1)

    step(1)
    expect(p.io.fifo.deq.valid, 0)
  }

  def testXYPacketDestOfInputPackage() {
    poke(p.io.fifo.enq.valid, 1)
    poke(p.io.fifo.deq.ready, 0)

    poke(p.io.fifo.enq.bits.dest.y, 9)
    poke(p.io.fifo.enq.bits.dest.x, 15)
    poke(p.io.fifo.enq.bits.sender.y, 5)
    poke(p.io.fifo.enq.bits.sender.x, 3)
    step(1)
    expect(p.io.yDest, 9)
    expect(p.io.xDest, 15)
  }

  testFifoIntegration()
  testInputPortRequest()
  testXYPacketDestOfInputPackage()
}
