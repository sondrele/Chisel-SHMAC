package router

import Chisel._

class ArbiterIO(numPorts: Int) extends Bundle {
  val isEmpty = Vec.fill(numPorts) { Bool(INPUT) } // Whether InputPort(i) is empty or not
  val requesting = Vec.fill(numPorts) { Bool(INPUT) } // Whether InputPort(i) wants to send or not
  val isFull = Bool(INPUT) // Whether the OutputPort for this arbiter is full or not
  val granted = UInt(OUTPUT, width = 5) // The value of the InputPort that is granted
  val grantedReady = Bool(OUTPUT) // Can be used to verify that that the granted direction is valid
}

class DirectionArbiter(numPorts: Int) extends Module {
  val io = new ArbiterIO(numPorts)

  // Reorganize the io-signals to internal, decoupled signals used with the RRArbiter-module
  // These signals can be part of the io-bundle instead, if it's better suited for the Router
  val requestingInputPorts = Vec.fill(numPorts) { Decoupled(UInt(width = 5)).flip() }
  val granted = Decoupled(UInt(width = 5))

  val readyRequests = Vec.fill(numPorts) { Bool() }
  for (i <- 0 until numPorts) {
    // check whether or not the input_ports are requesting to send a packet
    // to the output port corresponding to this arbiter
    // Set the valid request accordingly, only one of these requests will at any time be granted
    requestingInputPorts(i).valid := io.requesting(i) & !io.isEmpty(i) & !io.isFull
    requestingInputPorts(i).bits := TileDir.getDirection(i)
    readyRequests(i) := requestingInputPorts(i).ready
  }

  granted.ready := Bool(true)
  io.grantedReady := granted.valid
  io.granted := granted.bits

  // This arbiter is the consumer of the RRAbiter, which is the producer of
  // the granted 'bits', i.e. the granted input_port that can send to the
  // output port corresponding to this DirectionArbiter-instance
  val arbiter = Module(new RRArbiter(UInt(), numPorts))
  arbiter.io.in <> requestingInputPorts
  arbiter.io.out <> granted
}

class DirectionArbiterTest(a: DirectionArbiter) extends Tester(a) {
  def initArbiterIO() {
    poke(a.io.isEmpty(East.index), 0)
    poke(a.io.requesting(East.index), 1)
    poke(a.io.isEmpty(North.index), 0)
    poke(a.io.requesting(North.index), 1)
    poke(a.io.isEmpty(West.index), 1)
    poke(a.io.requesting(West.index), 1)
    poke(a.io.isEmpty(South.index), 0)
    poke(a.io.requesting(South.index), 0)
    poke(a.io.isEmpty(Local.index), 0)
    poke(a.io.requesting(Local.index), 1)
    poke(a.io.isFull, 0)
  }

  initArbiterIO()

  step(1)
  expect(a.io.granted, Local.value.litValue())
  step(1)
  expect(a.io.granted, East.value.litValue())
  step(1)
  expect(a.io.granted, North.value.litValue())
  step(1)
  poke(a.io.isEmpty(East.index), 1)
  expect(a.io.granted, Local.value.litValue())
  step(1)
  expect(a.io.granted, North.value.litValue())
  step(1)
  expect(a.io.granted, Local.value.litValue())
}
