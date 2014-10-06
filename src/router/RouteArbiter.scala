package router

import Chisel._

/*
  Toplevel arbiter, wrapping around arbiters for earch of the
  five directions
*/

class RoutingArbiter extends Module {
  val io = new Bundle() {
    val in  = Vec.fill(5) { Decoupled(UInt(width = 32)).flip() }
    val granted = Decoupled(UInt(width = 32))
  }

  val arbiter = Module(new RRArbiter(UInt(), 5))
  for (i <- 0 until 5) {
    arbiter.io.in(i) <> io.in(i)
  }

  io.granted <> arbiter.io.out
}

class RoutingArbiterTest(a: RoutingArbiter) extends Tester(a) {
  // Init input
  for (i <- 0 until 5) {
    // Set input data
    poke(a.io.in(i).bits, i)
    // Set input data to valid
    poke(a.io.in(i).valid, 1)
  }
  // Start arbiter
  poke(a.io.granted.ready, 1)

  for (i <- 1 until 10) {
    // Verify round-robin
    expect(a.io.granted.bits, i % 5)
    step(1)
  }
}
