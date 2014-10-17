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
  val crossbar = Module(new CrossBar()).io

  val arbiterEast = Module(new DirectionArbiter(numPorts)).io
  val grantedPortEast = arbiterEast.granted

  val east = Module(new DirectionRouter(tileX, tileY, numRecords)).io
  east.inRequest := io.inRequest(0)
  east.inData := io.inData(0)
  crossbar.inData(0) := east.crossbarIn
  io.inReady(0) := east.inReady
  io.outRequest(0) := east.outRequest
  io.outData(0) := east.outData
  east.crossbarOut := crossbar.outData(0)
  east.outReady := io.outReady(0)
  crossbar.select(0) := east.destTile
  arbiterEast.isEmpty(0) := east.isEmpty
  arbiterEast.requesting(0) := east.requesting
  arbiterEast.isFull := east.isFull
}


class RouterTest(r: Router) extends Tester(r) {

  def testDataPathFromEastToEastWithOnePort() {
    // Test to see that data travels through the router in one cycle
    // Initialize router input data in east direction
    val packet = PacketData.create(address = 10, xDest = 1, xSender = 1).litValue()
    poke(r.io.inData(0), packet)
    poke(r.io.inRequest(0), 1)
    poke(r.io.outReady(0), 1)

    // Cycle 0: Data arrives router and input port
    val routerIn = peek(r.io.inData(0))
    // expect(r.inEast.io.fifo.in.bits, routerIn)
    // expect(r.inEast.io.fifo.out.bits, 0)
    expect(routerIn == packet, "Packet matches inEast.in")
    expect(r.io.outRequest(0), 0) // output port shoule be empty
    step(1)
    // Stop sending data
    poke(r.io.inRequest(0), 0)

    // Cycle 1: Data is at head in input port and traverses through crossbar
    // val inEastOut = peek(r.inEast.io.fifo.out.bits)
    // expect(r.outEast.io.fifo.in.bits, inEastOut)
    // expect(r.outEast.io.fifo.out.bits, 0)
    // expect(inEastOut == packet, "Packet matches inEast.in")
    // expect(r.io.outRequest(0), 0) // output port shoule be empty

    // Check that the RouteComputation module has calculated the right
    // direction tile for this packet
    // expect(r.eastOutputDir, East.litValue)

    // The port granted to send over the crossbar should be inEast
    expect(r.grantedPortEast, East.litValue)
    step(1)

    // Cycle 2: Data reaches the output of the output port (to send it
    // further on to the network)
    // val outEastOut = peek(r.outEast.io.fifo.out.bits)
    expect(r.io.outData(0), packet)//outEastOut)
    // expect(outEastOut == packet, "Packet matches outEast.out")
    expect(r.io.outRequest(0), 1)
    // peek(r.grantedPortEast)
  }

  testDataPathFromEastToEastWithOnePort()
}
