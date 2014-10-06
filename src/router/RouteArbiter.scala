package router

import Chisel._

class RouteArbiter extends Module {
  val io = new Bundle() {
    val inDirections  = Vec.fill(5) { Bool(INPUT) }
    val outReady      = Vec.fill(5) { Bool(INPUT) }
    val outDir        = UInt(OUTPUT, width = 5)
  }

  val inIO = Decoupled(Bool(INPUT))

  // val arbiter = Module(new RRArbiter(UInt(width=1), 1))
  // arbiter.io.in(0) <> inIO
  // arbiter.io.in(1) <> io.inDirections(1)
  // arbiter.io.in(2) <> io.inDirections(2)
  // arbiter.io.in(3) <> io.inDirections(3)
  // arbiter.io.in(4) <> io.inDirections(4)
  io.outDir := UInt(0)
}

class RouteArbiterTest(a: RouteArbiter) extends Tester(a) {
  expect(a.io.outDir, 0)
}
