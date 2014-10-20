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
  east.inRead := grantedPortNorth(0) | grantedPortEast(0) // fix signal
  crossbar.inData(0) := east.crossbarIn
  io.inReady(0) := east.inReady
  io.outRequest(0) := east.outRequest
  io.outData(0) := east.outData
  east.outWrite := grantedPortEast > UInt(0) && grantedPortEastReady
  east.crossbarOut := crossbar.outData(0)
  east.outReady := io.outReady(0)
  when (grantedPortEastReady) {
    crossbar.select(0) := grantedPortEast
  }.otherwise {
    crossbar.select(0) := UInt(0)
  }

  north.inRequest := io.inRequest(1)
  north.inData := io.inData(1)
  north.inRead := grantedPortNorth(1) | grantedPortEast(1) // fix signal
  crossbar.inData(1) := north.crossbarIn
  io.inReady(1) := north.inReady
  io.outRequest(1) := north.outRequest
  io.outData(1) := north.outData
  north.outWrite := grantedPortNorth > UInt(0) && grantedPortNorthReady
  north.crossbarOut := crossbar.outData(1)
  north.outReady := io.outReady(1)
  when (grantedPortNorthReady) {
    crossbar.select(1) := grantedPortNorth
  }.otherwise {
    crossbar.select(1) := UInt(0)
  }

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

  def testDataPathFromEastToNorth() {
    val packetFromEastToNorth = PacketData.create(
      address = 15,
      xDest = 1,
      yDest = 0,
      xSender = 2,
      ySender = 1
    ).litValue()

    poke(r.io.inRequest(0), 1)
    poke(r.io.inRequest(1), 0)
    poke(r.io.inData(0), packetFromEastToNorth)
    poke(r.io.inData(1), 0)
    poke(r.io.outReady(0), 0)
    poke(r.io.outReady(1), 0)

    // Cycle 0: Data arrives router and input port
    val routerIn = peek(r.io.inData(0))
    expect(routerIn == packetFromEastToNorth, "Packet matches inEast.in")

    expect(r.io.inReady(0), 1)
    expect(r.io.inReady(1), 1)
    expect(r.io.outRequest(0), 0) // output port should be empty
    expect(r.io.outRequest(1), 0)
    expect(r.io.outData(0), 0)
    expect(r.io.outData(0), 0)

    peek(r.grantedPortNorth)
    expect(r.grantedPortNorthReady, 0)
    peek(r.grantedPortEast)
    expect(r.grantedPortEastReady, 0)
    expect(r.east.outWrite, 0)
    expect(r.north.outWrite, 0)

    step(1)
    // Stop sending data
    poke(r.io.inRequest(0), 0)
    poke(r.io.inRequest(1), 0)
    poke(r.io.inData(0), 0)
    poke(r.io.inData(1), 0)
    poke(r.io.outReady(0), 0)
    poke(r.io.outReady(1), 1)

    // Cycle 1: Data is at head in input port and traverses through crossbar
    // The port granted to send over the crossbar should be east.in

    expect(r.io.inReady(0), 1)
    expect(r.io.inReady(1), 1)
    expect(r.io.outRequest(0), 0) // output port should still be empty
    expect(r.io.outRequest(1), 0)
    expect(r.io.outData(0), 0)
    expect(r.io.outData(0), 0)

    expect(r.grantedPortNorth, East.litValue)
    expect(r.grantedPortNorthReady, 1)
    peek(r.grantedPortEast)
    expect(r.grantedPortEastReady, 0)
    expect(r.east.outWrite, 0)
    expect(r.north.outWrite, 1)

    step(1)

    // Cycle 2: Data reaches the output of the output port, to send it
    // further on to the network
    peek(r.grantedPortEast)
    peek(r.grantedPortNorth)
    peek(r.io.outData)
    expect(r.io.outData(1), packetFromEastToNorth)
    expect(r.io.outRequest(1), 1)
  }

  testDataPathFromEastToNorth()
}
