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

  val numPorts = 2
  val numRecords = 4

  val io = new RouterIO(numPorts)
  val crossbar = Module(new CrossBar()).io

  val arbiterEast = Module(new DirectionArbiter(numPorts)).io
  val grantedPortEast = arbiterEast.granted
  val grantedPortEastReady = arbiterEast.grantedReady
  val arbiterNorth = Module(new DirectionArbiter(numPorts)).io
  val grantedPortNorth = arbiterNorth.granted
  val grantedPortNorthReady = arbiterNorth.grantedReady

  val east = Module(new DirectionRouter(tileX, tileY, numRecords)).io
  val north = Module(new DirectionRouter(tileX, tileY, numRecords)).io

  east.inRequest := io.inRequest(0)
  east.inData := io.inData(0)
  east.inRead := grantedPortNorth(0) | grantedPortEast(0) // Verify that signal is right (seams to be)
  crossbar.inData(0) := east.crossbarIn
  io.inReady(0) := east.inReady
  io.outRequest(0) := east.outRequest
  io.outData(0) := east.outData
  east.outWrite := grantedPortEastReady
  east.crossbarOut := crossbar.outData(0)
  east.outReady := io.outReady(0)
  crossbar.select(0) := grantedPortEast

  north.inRequest := io.inRequest(1)
  north.inData := io.inData(1)
  north.inRead := grantedPortNorth(1) | grantedPortEast(1) // Verify that signal is right (seams to be)
  crossbar.inData(1) := north.crossbarIn
  io.inReady(1) := north.inReady
  io.outRequest(1) := north.outRequest
  io.outData(1) := north.outData
  north.outWrite := grantedPortNorthReady
  north.crossbarOut := crossbar.outData(1)
  north.outReady := io.outReady(1)
  crossbar.select(1) := grantedPortNorth

  arbiterEast.isEmpty(0) := east.isEmpty
  arbiterEast.isEmpty(1) := north.isEmpty
  arbiterEast.requesting(0) := east.direction(0) // && east.requesting <- combinational path
  arbiterEast.requesting(1) := north.direction(0) // && north.requesting
  arbiterEast.isFull := east.isFull

  arbiterNorth.isEmpty(0) := east.isEmpty
  arbiterNorth.isEmpty(1) := north.isEmpty
  arbiterNorth.requesting(0) := east.direction(1) // && east.requesting
  arbiterNorth.requesting(1) := north.direction(1) // && north.requesting
  arbiterNorth.isFull := north.isFull
}

class RouterTest(r: Router) extends Tester(r) {

  def peekArRouter() {
    peek(r.east)
    peek(r.north)
    peek(r.arbiterEast)
    peek(r.arbiterNorth)
    peek(r.crossbar)
  }

  val packetFromEastToNorth = PacketData.create(
    address = 15,
    xDest = 1,
    yDest = 0,
    xSender = 2,
    ySender = 1
  ).litValue()

  val packetFromNorthToEast = PacketData.create(
    address = 10,
    xDest = 2,
    yDest = 1,
    xSender = 1,
    ySender = 0
  ).litValue()

  // Sends a packet from east input port, to the north output port,
  // and a packet from the north input port to the east output port the next cycle
  def testDataPathBetweenEastToNorth() {
    poke(r.io.inRequest(0), 1)
    poke(r.io.inRequest(1), 0)
    poke(r.io.inData(0), packetFromEastToNorth)
    poke(r.io.inData(1), packetFromNorthToEast)
    poke(r.io.outReady(0), 0)
    poke(r.io.outReady(1), 0)

    // Cycle 0: Data arrives router and input port
    val eastIn = peek(r.io.inData(0))
    expect(eastIn == packetFromEastToNorth, "Packet matches east.in")

    expect(r.io.inReady(0), 1)
    expect(r.io.inReady(1), 1)
    expect(r.io.outRequest(0), 0) // output port should be empty
    expect(r.io.outRequest(1), 0)
    expect(r.io.outData(0), 0)
    expect(r.io.outData(1), 0)

    expect(r.grantedPortNorth, 0)
    expect(r.grantedPortNorthReady, 0)
    expect(r.grantedPortEast, 0)
    expect(r.grantedPortEastReady, 0)
    expect(r.east.outWrite, 0)
    expect(r.north.outWrite, 0)

    step(1)
    // Stop sending data
    poke(r.io.inRequest(0), 0)
    poke(r.io.inRequest(1), 1)
    poke(r.io.inData(0), 0)
    poke(r.io.inData(1), packetFromNorthToEast)
    poke(r.io.outReady(0), 1)
    poke(r.io.outReady(1), 1)

    // Cycle 1: Data is at head in input port and traverses through crossbar
    // The port granted to send over the crossbar should be east.in

    expect(r.io.inReady(0), 1)
    expect(r.io.inReady(1), 1)
    expect(r.io.outRequest(0), 0) // output port should still be empty
    expect(r.io.outRequest(1), 0)
    expect(r.io.outData(0), 0)
    expect(r.io.outData(1), 0)

    expect(r.grantedPortNorth, East.litValue)
    expect(r.grantedPortNorthReady, 1)
    expect(r.grantedPortEast, 0)
    expect(r.grantedPortEastReady, 0)
    expect(r.east.outWrite, 0)
    expect(r.north.outWrite, 1)

    step(1)

    poke(r.io.inRequest(0), 0)
    poke(r.io.inRequest(1), 0)
    poke(r.io.inData(0), 0)
    poke(r.io.inData(1), 0)
    poke(r.io.outReady(0), 1)
    poke(r.io.outReady(1), 1)

    // Cycle 2: Data reaches the output of the output port, to send it
    // further on to the network
    expect(r.io.inReady(0), 1)
    expect(r.io.inReady(1), 1)
    expect(r.io.outRequest(0), 0)
    expect(r.io.outRequest(1), 1)
    expect(r.io.outData(0), 0)
    expect(r.io.outData(1), packetFromEastToNorth)

    expect(r.grantedPortNorth, 0)
    expect(r.grantedPortNorthReady, 0)
    expect(r.grantedPortEast, North.litValue)
    expect(r.grantedPortEastReady, 1)
    expect(r.east.outWrite, 1)
    expect(r.north.outWrite, 0)

    step(1)

    poke(r.io.inRequest(0), 0)
    poke(r.io.inRequest(1), 0)
    poke(r.io.inData(0), 0)
    poke(r.io.inData(1), 0)
    poke(r.io.outReady(0), 1)
    poke(r.io.outReady(1), 1)

    expect(r.io.inReady(0), 1)
    expect(r.io.inReady(1), 1)
    expect(r.io.outRequest(0), 1)
    expect(r.io.outRequest(1), 0)
    expect(r.io.outData(0), packetFromNorthToEast)
    expect(r.io.outData(1), 0)

    step(1)
  }

  // Sends two packets at the same cycle, essentially the same test as the other one
  // One from the east input port to the north output port
  // One from the north input port to the east output port
  def testSendingTwoPacketsAtTheSameTime() {
    poke(r.io.inRequest(0), 1)
    poke(r.io.inRequest(1), 1)
    poke(r.io.inData(0), packetFromEastToNorth)
    poke(r.io.inData(1), packetFromNorthToEast)
    poke(r.io.outReady(0), 0)
    poke(r.io.outReady(1), 0)

    // Cycle 0: Data arrives router and input port
    val eastIn = peek(r.io.inData(0))
    expect(eastIn == packetFromEastToNorth, "Packet matches east.in")
    val northIn = peek(r.io.inData(1))
    expect(northIn == packetFromNorthToEast, "Packet matches north.in")

    expect(r.io.inReady(0), 1)
    expect(r.io.inReady(1), 1)
    expect(r.io.outRequest(0), 0) // output port should be empty
    expect(r.io.outRequest(1), 0)
    expect(r.io.outData(0), 0)
    expect(r.io.outData(1), 0)

    expect(r.grantedPortNorth, 0)
    expect(r.grantedPortNorthReady, 0)
    expect(r.grantedPortEast, 0)
    expect(r.grantedPortEastReady, 0)
    expect(r.east.outWrite, 0)
    expect(r.north.outWrite, 0)

    step(1)
    // Stop sending data
    poke(r.io.inRequest(0), 0)
    poke(r.io.inRequest(1), 0)
    poke(r.io.inData(0), 0)
    poke(r.io.inData(1), 0)
    poke(r.io.outReady(0), 1)
    poke(r.io.outReady(1), 1)

    // Cycle 1: Data is at head in input port and traverses through crossbar
    // The port granted to send over the crossbar should be east.in

    expect(r.io.inReady(0), 1)
    expect(r.io.inReady(1), 1)
    expect(r.io.outRequest(0), 0) // output port should still be empty
    expect(r.io.outRequest(1), 0)
    expect(r.io.outData(0), 0)
    expect(r.io.outData(1), 0)

    expect(r.grantedPortNorth, East.litValue)
    expect(r.grantedPortNorthReady, 1)
    expect(r.grantedPortEast, North.litValue)
    expect(r.grantedPortEastReady, 1)
    expect(r.east.outWrite, 1)
    expect(r.north.outWrite, 1)

    step(1)

    poke(r.io.inRequest(0), 0)
    poke(r.io.inRequest(1), 0)
    poke(r.io.inData(0), 0)
    poke(r.io.inData(1), 0)
    poke(r.io.outReady(0), 1)
    poke(r.io.outReady(1), 1)

    // Cycle 2: Data reaches the output of the output port, to send it
    // further on to the network
    expect(r.io.inReady(0), 1)
    expect(r.io.inReady(1), 1)
    expect(r.io.outRequest(0), 1)
    expect(r.io.outRequest(1), 1)
    expect(r.io.outData(0), packetFromNorthToEast)
    expect(r.io.outData(1), packetFromEastToNorth)

    expect(r.grantedPortNorth, 0)
    expect(r.grantedPortNorthReady, 0)
    expect(r.grantedPortEast, 0)
    expect(r.grantedPortEastReady, 0)
    expect(r.east.outWrite, 0)
    expect(r.north.outWrite, 0)

    step(1)
  }


  testDataPathBetweenEastToNorth()
  testSendingTwoPacketsAtTheSameTime()
}
