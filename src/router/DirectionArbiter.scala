package router

import Chisel._

class ArbiterIO(numPorts: Int) extends Bundle {
  val emptyInputPorts = Vec.fill(numPorts) { Bool(INPUT) }
  val requestingInputPorts = Vec.fill(numPorts) { Bool(INPUT) }
  val outputIsFull = Bool(INPUT)
  val grantedInputPort = UInt(OUTPUT, width = 5)
  val grantedReady = Bool(OUTPUT) // Can be used to verify that that the granted direction is valid
}

class DirectionArbiter(numPorts: Int) extends Module {
  val io = new ArbiterIO(numPorts)

  // Reorganize the io-signals to internal, decoupled signals used with the RRArbiter-module
  // These signals can later be part of the io-bundle if it's better suited
  val in = Vec.fill(numPorts) { Decoupled(UInt(width = 5)).flip() }
  val granted = Decoupled(UInt(width = 5))

  val validRequests = Vec.fill(numPorts) { Bool() }
  val readyRequests = Vec.fill(numPorts) { Bool() }
  for (i <- 0 until numPorts) {
    // check whether or not the input_ports are requesting to send a packet
    // to the output port corresponding to this arbiter
    validRequests(i) := io.requestingInputPorts(i) & !io.emptyInputPorts(i) & !io.outputIsFull

    in(i).valid := validRequests(i)
    in(i).bits := East.value << UInt(i)
    readyRequests(i) := in(i).ready
  }

  granted.ready := Bool(true)
  io.grantedReady := granted.valid
  io.grantedInputPort := granted.bits

  // This arbiter is the consumer of the RRAbiter, which is the producer of
  // the granted 'bits', i.e. the granted input_port that can send to the
  // output port corresponding to this DirectionArbiter-instance
  val arbiter = Module(new RRArbiter(UInt(), numPorts))
  arbiter.io.in <> in
  arbiter.io.out <> granted
}

class DirectionArbiterTest(a: DirectionArbiter) extends Tester(a) {
  // Two ports, east and north
  // inEast and inNorth wants to send to outEast
  def initArbiterIO() {
    poke(a.io.emptyInputPorts(East.index), 0)
    poke(a.io.requestingInputPorts(East.index), 1)
    poke(a.io.emptyInputPorts(North.index), 0)
    poke(a.io.requestingInputPorts(North.index), 1)
    poke(a.io.outputIsFull, 0)
  }

  initArbiterIO()

  step(1)
  expect(a.io.grantedInputPort, East.value.litValue())
  step(1)
  expect(a.io.grantedInputPort, North.value.litValue())
  step(1)
  expect(a.io.grantedInputPort, East.value.litValue())
  step(1)
  expect(a.io.grantedInputPort, North.value.litValue())
}
