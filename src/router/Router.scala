package router

import Chisel._

class RouterIO(numPorts: Int) extends Bundle {
  val inRequest = Vec.fill(numPorts) { Bool(INPUT) } // Request to write into router
  val inData = Vec.fill(numPorts) { PacketData(INPUT) } // Data to write
  val inReady = Vec.fill(numPorts) { Bool(OUTPUT) } // True if input port is not full
  val outRequest = Vec.fill(numPorts) { Bool(OUTPUT) } // Router requesting to send data
  val outData = Vec.fill(numPorts) { PacketData(OUTPUT) } // Data to send
  val outReady = Vec.fill(numPorts) { Bool(INPUT) } // True to request output to send data
}

class Router(x: Int, y: Int) extends Module {
  val tileX = UInt(x, width = 4)
  val tileY = UInt(y, width = 4)

  val numPorts = 1
  val numRecords = 4
  val io = new RouterIO(numPorts)

  val inEast = Module(new InputPort(numRecords))
  inEast.io.fifo.in.bits := io.inData(0)
  inEast.io.fifo.in.valid := io.inRequest(0)
  inEast.io.fifo.out.ready := Bool(true) // Router instance always ready to read input

  val outEast = Module(new OutputPort(numRecords))
  outEast.io.fifo.in.bits := inEast.io.fifo.out.bits
  outEast.io.fifo.in.valid := Bool(true) // Router instance always writing output
  outEast.io.fifo.out.ready := io.outReady(0)

  io.inReady(0) := inEast.io.fifo.in.ready
  io.outData(0) := outEast.io.fifo.out.bits

  val destRoute = Module(new RouteComputation())
  destRoute.io.xCur := tileX
  destRoute.io.yCur := tileY
  destRoute.io.xDest := inEast.io.xDest
  destRoute.io.yDest := inEast.io.yDest
  val destTile = destRoute.io.dest

  val srcRoute = Module(new RouteComputation())
  srcRoute.io.xCur := tileX
  srcRoute.io.yCur := tileY
  srcRoute.io.xDest := inEast.io.xSender
  srcRoute.io.yDest := inEast.io.ySender
  val srcTile = srcRoute.io.dest

  // val inNorth = Module(new InputPort(4))
  // inNorth.io.fifo.in.bits := io.inData(1)
  // val outNorth = Module(new OutputPort(4))
  // io.outData(1) := outNorth.io.fifo.out.bits
}

class RouterTest(r: Router) extends Tester(r) {
  // Test to see that data travels through the router in one cycle
  // Initialize router input data in east direction
  val packet = PacketData.create(address = 10, xDest = 1).litValue()
  poke(r.io.inData(0), packet)
  poke(r.io.inRequest(0), 1)
  poke(r.io.outReady(0), 1)

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

  // Check that the RouteComputation module calculates the right
  // destination and source tile for this packet
  expect(r.destTile, East.litValue)
  expect(r.srcTile, Local.litValue)
  // Cycle 2: Data reaches the output of the output port (to send it
  // further on to the network)
  step(1)
  val outEastOut = peek(r.outEast.io.fifo.out.bits)
  expect(r.io.outData(0), outEastOut)
  expect(outEastOut == packet, "Packet matches outEast.out")
}
