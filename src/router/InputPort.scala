package router

import Chisel._
import utils.{Fifo, DecoupledFifoIO}

class InputPort(n: Int) extends Module {
  val io = new Bundle {
    val fifo = new DecoupledFifoIO(Packet.length)

    val direction = UInt(INPUT, width = 5) // Direction enum
    val request   = UInt(OUTPUT, width = 5)
    // val xDir      = UInt(OUTPUT)
    // val yDir      = UInt(OUTPUT)
  }

  val queue = Module(new Fifo(n, Packet.length))
  io.fifo <> queue.io

  when(io.fifo.out.ready || !io.fifo.out.valid) {
    io.request := UInt(0)
  }.otherwise {
    io.request := io.direction
  }
}

class InputPortTest(p: InputPort) extends Tester(p) {

  def testFifoIntegration() = {
    poke(p.io.fifo.in.valid, 1)
    poke(p.io.fifo.in.bits, 10)
    step(1)
    expect(p.io.fifo.out.bits, 10)
  }

  def testInputPortRequest() = {
    poke(p.io.direction, Direction.south.litValue())
    poke(p.io.fifo.out.ready, 1)
    expect(p.io.request, 0)

    poke(p.io.fifo.out.ready, 0)
    expect(p.io.request, Direction.south.litValue())
  }

  testFifoIntegration()
  testInputPortRequest()
}
