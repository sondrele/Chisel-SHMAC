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
  val arbiterNorth = Module(new DirectionArbiter(numPorts)).io
  val arbiters = Vec(arbiterEast, arbiterNorth)

  val east = Module(new DirectionRouter(tileX, tileY, numRecords)).io
  val north = Module(new DirectionRouter(tileX, tileY, numRecords)).io
  val routers = Vec(east, north)

  routers(0).inRequest := io.inRequest(0)
  routers(0).inData := io.inData(0)
  routers(0).inRead := arbiters(1).granted(0) | arbiters(0).granted(0) // Verify that signal is right (seams to be)
  crossbar.inData(0) := routers(0).crossbarIn
  io.inReady(0) := routers(0).inReady
  io.outRequest(0) := routers(0).outRequest
  io.outData(0) := routers(0).outData
  routers(0).outWrite := arbiters(0).grantedReady
  routers(0).crossbarOut := crossbar.outData(0)
  routers(0).outReady := io.outReady(0)
  crossbar.select(0) := arbiters(0).granted

  routers(1).inRequest := io.inRequest(1)
  routers(1).inData := io.inData(1)
  routers(1).inRead := arbiters(1).granted(1) | arbiters(0).granted(1) // Verify that signal is right (seams to be)
  crossbar.inData(1) := routers(1).crossbarIn
  io.inReady(1) := routers(1).inReady
  io.outRequest(1) := routers(1).outRequest
  io.outData(1) := routers(1).outData
  routers(1).outWrite := arbiters(1).grantedReady
  routers(1).crossbarOut := crossbar.outData(1)
  routers(1).outReady := io.outReady(1)
  crossbar.select(1) := arbiters(1).granted

  arbiters(0).isEmpty(0) := routers(0).isEmpty
  arbiters(0).isEmpty(1) := routers(1).isEmpty
  arbiters(0).requesting(0) := routers(0).direction(0)
  arbiters(0).requesting(1) := routers(1).direction(0)
  arbiters(0).isFull := routers(0).isFull

  arbiters(1).isEmpty(0) := routers(0).isEmpty
  arbiters(1).isEmpty(1) := routers(1).isEmpty
  arbiters(1).requesting(0) := routers(0).direction(1)
  arbiters(1).requesting(1) := routers(1).direction(1)
  arbiters(1).isFull := routers(1).isFull
}

class RouterTest(r: Router) extends Tester(r) {

  def peekArRouter() {
    peek(r.routers(0))
    peek(r.routers(1))
    peek(r.arbiters(0))
    peek(r.arbiters(1))
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

    expect(r.arbiters(1).granted, 0)
    expect(r.arbiters(1).grantedReady, 0)
    expect(r.arbiters(0).granted, 0)
    expect(r.arbiters(0).grantedReady, 0)
    expect(r.routers(0).outWrite, 0)
    expect(r.routers(1).outWrite, 0)

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

    expect(r.arbiters(1).granted, East.litValue)
    expect(r.arbiters(1).grantedReady, 1)
    expect(r.arbiters(0).granted, 0)
    expect(r.arbiters(0).grantedReady, 0)
    expect(r.routers(0).outWrite, 0)
    expect(r.routers(1).outWrite, 1)

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

    expect(r.arbiters(1).granted, 0)
    expect(r.arbiters(1).grantedReady, 0)
    expect(r.arbiters(0).granted, North.litValue)
    expect(r.arbiters(0).grantedReady, 1)
    expect(r.routers(0).outWrite, 1)
    expect(r.routers(1).outWrite, 0)

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

    expect(r.arbiters(1).granted, 0)
    expect(r.arbiters(1).grantedReady, 0)
    expect(r.arbiters(0).granted, 0)
    expect(r.arbiters(0).grantedReady, 0)
    expect(r.routers(0).outWrite, 0)
    expect(r.routers(1).outWrite, 0)

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

    expect(r.arbiters(1).granted, East.litValue)
    expect(r.arbiters(1).grantedReady, 1)
    expect(r.arbiters(0).granted, North.litValue)
    expect(r.arbiters(0).grantedReady, 1)
    expect(r.routers(0).outWrite, 1)
    expect(r.routers(1).outWrite, 1)

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

    expect(r.arbiters(1).granted, 0)
    expect(r.arbiters(1).grantedReady, 0)
    expect(r.arbiters(0).granted, 0)
    expect(r.arbiters(0).grantedReady, 0)
    expect(r.routers(0).outWrite, 0)
    expect(r.routers(1).outWrite, 0)

    step(1)
  }

  testDataPathBetweenEastToNorth()
  testSendingTwoPacketsAtTheSameTime()
}
