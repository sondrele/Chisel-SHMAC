package test

import Chisel._
import router._

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
