package router

import Chisel._

class Router extends Module {
  val numPorts = 1
  val io = new Bundle {
    val inRequest = Vec.fill(numPorts) { Bool(INPUT) } // Request to write
    val inData = Vec.fill(numPorts) { PacketData(INPUT) } // Data to write
    val inReady = Vec.fill(numPorts) { Bool(OUTPUT) } // True if input port is not full
    val outRequest = Vec.fill(numPorts) { Bool(OUTPUT) }
    val outData = Vec.fill(numPorts) { PacketData(OUTPUT) }
    val outReady = Vec.fill(numPorts) { Bool(INPUT) }
  }

  val inEast = Module(new InputPort(4))
  inEast.io.fifo.in.bits := io.inData(0)
  inEast.io.fifo.in.valid := io.inRequest(0)
  inEast.io.fifo.out.ready := Bool(true)
  io.inReady(0) := inEast.io.fifo.in.ready

  val outEast = Module(new OutputPort(4))
  outEast.io.fifo.in.bits := inEast.io.fifo.out.bits
  outEast.io.fifo.in.valid := Bool(true)
  outEast.io.fifo.out.ready := Bool(true)

  io.outData(0) := outEast.io.fifo.out.bits

  // val inNorth = Module(new InputPort(4))
  // inNorth.io.fifo.in.bits := io.inData(1)
  // val outNorth = Module(new OutputPort(4))
  // io.outData(1) := outNorth.io.fifo.out.bits
}

class RouterTest(r: Router) extends Tester(r) {
  // Test to see that data travels through the router in one cycle
  // Initialize router input data in east direction
  val packet = PacketData.create(address = 10).litValue()
  poke(r.io.inData(0), packet)
  poke(r.io.inRequest(0), 1)
  expect(r.io.inRequest(0), 1)

  // Cycle 0: Data arrives router and input port
  val routerIn = peek(r.io.inData(0))
  expect(r.inEast.io.fifo.in.bits, routerIn)
  expect(r.inEast.io.fifo.out.bits, 0)
  expect(routerIn == packet, "Packet matches inEast.in")
  step(1)
  // Cycle 1: Data is at head in input port and traverses to input of
  // the output port
  val inEastOut = peek(r.inEast.io.fifo.out.bits)
  expect(r.outEast.io.fifo.in.bits, inEastOut)
  expect(r.outEast.io.fifo.out.bits, 0)
  expect(inEastOut == packet, "Packet matches inEast.in")
  // Cycle 2: Data reaches the output of the output port (to send it
  // further on to the network)
  step(1)
  val outEastOut = peek(r.outEast.io.fifo.out.bits)
  expect(r.io.outData(0), outEastOut)
  expect(outEastOut == packet, "Packet matches outEast.out")
}
