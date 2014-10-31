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

  def emptyPacket = Array[BigInt](0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

  def packetFromEastToNorth = Array[BigInt](15, 0, 0, 0, 0, 0, 0, 2, 1, 1, 0)

  def packetFromNorthToEast = Array[BigInt](10, 0, 0, 0, 0, 0, 0, 1, 0, 2, 1)

  // Sends a packet from east input port, to the north output port,
  // and a packet from the north input port to the east output port the next cycle
  def testDataPathBetweenEastToNorth() {
    poke(r.io.ports(0).inRequest, 1)
    poke(r.io.ports(1).inRequest, 0)
    poke(r.io.ports(0).inData, packetFromEastToNorth)
    poke(r.io.ports(1).inData, packetFromNorthToEast)
    poke(r.io.ports(0).outReady, 0)
    poke(r.io.ports(1).outReady, 0)

    // Cycle 0: Data arrives router and input port
    expect(r.io.ports(0).inReady, 1)
    expect(r.io.ports(1).inReady, 1)
    expect(r.io.ports(0).outRequest, 0) // output port should be empty
    expect(r.io.ports(1).outRequest, 0)
    expect(r.io.ports(0).outData, emptyPacket)
    expect(r.io.ports(1).outData, emptyPacket)

    expect(r.arbiters(1).granted, 0)
    expect(r.arbiters(1).grantedReady, 0)
    expect(r.arbiters(0).granted, 0)
    expect(r.arbiters(0).grantedReady, 0)
    expect(r.routers(0).outWrite, 0)
    expect(r.routers(1).outWrite, 0)

    step(1)
    // Stop sending data
    poke(r.io.ports(0).inRequest, 0)
    poke(r.io.ports(1).inRequest, 1)
    poke(r.io.ports(0).inData, emptyPacket)
    poke(r.io.ports(1).inData, packetFromNorthToEast)

    poke(r.io.ports(0).outReady, 1)
    poke(r.io.ports(1).outReady, 1)

    // Cycle 1: Data is at head in input port and traverses through crossbar
    // The port granted to send over the crossbar should be east.in
    expect(r.io.ports(0).inReady, 1)
    expect(r.io.ports(1).inReady, 1)
    expect(r.io.ports(0).outRequest, 0) // output port should still be empty
    expect(r.io.ports(1).outRequest, 0)
    expect(r.io.ports(0).outData, emptyPacket)
    expect(r.io.ports(1).outData, emptyPacket)

    expect(r.arbiters(1).granted, East.litValue)
    expect(r.arbiters(1).grantedReady, 1)
    expect(r.arbiters(0).granted, 0)
    expect(r.arbiters(0).grantedReady, 0)
    expect(r.routers(0).outWrite, 0)
    expect(r.routers(1).outWrite, 1)

    step(1)

    poke(r.io.ports(0).inRequest, 0)
    poke(r.io.ports(1).inRequest, 0)
    poke(r.io.ports(0).inData, emptyPacket)
    poke(r.io.ports(1).inData, emptyPacket)
    poke(r.io.ports(0).outReady, 1)
    poke(r.io.ports(1).outReady, 1)

    // Cycle 2: Data reaches the output of the output port, to send it
    // further on to the network
    expect(r.io.ports(0).inReady, 1)
    expect(r.io.ports(1).inReady, 1)
    expect(r.io.ports(0).outRequest, 0)
    expect(r.io.ports(1).outRequest, 1)
    expect(r.io.ports(0).outData, emptyPacket)
    expect(r.io.ports(1).outData, packetFromEastToNorth)

    expect(r.arbiters(1).granted, 0)
    expect(r.arbiters(1).grantedReady, 0)
    expect(r.arbiters(0).granted, North.litValue)
    expect(r.arbiters(0).grantedReady, 1)
    expect(r.routers(0).outWrite, 1)
    expect(r.routers(1).outWrite, 0)

    step(1)

    poke(r.io.ports(0).inRequest, 0)
    poke(r.io.ports(1).inRequest, 0)
    poke(r.io.ports(0).inData, emptyPacket)
    poke(r.io.ports(1).inData, emptyPacket)
    poke(r.io.ports(0).outReady, 1)
    poke(r.io.ports(1).outReady, 1)

    expect(r.io.ports(0).inReady, 1)
    expect(r.io.ports(1).inReady, 1)
    expect(r.io.ports(0).outRequest, 1)
    expect(r.io.ports(1).outRequest, 0)
    expect(r.io.ports(0).outData, packetFromNorthToEast)
    expect(r.io.ports(1).outData, emptyPacket)

    step(1)
  }

  // Sends two packets at the same cycle, essentially the same test as the other one
  // One from the east input port to the north output port
  // One from the north input port to the east output port
  def testSendingTwoPacketsAtTheSameTime() {
    poke(r.io.ports(0).inRequest, 1)
    poke(r.io.ports(1).inRequest, 1)
    poke(r.io.ports(0).inData, packetFromEastToNorth)
    poke(r.io.ports(1).inData, packetFromNorthToEast)
    poke(r.io.ports(0).outReady, 0)
    poke(r.io.ports(1).outReady, 0)

    // Cycle 0: Data arrives router and input port
    expect(r.io.ports(0).inReady, 1)
    expect(r.io.ports(1).inReady, 1)
    expect(r.io.ports(0).outRequest, 0) // output port should be empty
    expect(r.io.ports(1).outRequest, 0)
    expect(r.io.ports(0).outData, emptyPacket)
    expect(r.io.ports(1).outData, emptyPacket)

    expect(r.arbiters(1).granted, 0)
    expect(r.arbiters(1).grantedReady, 0)
    expect(r.arbiters(0).granted, 0)
    expect(r.arbiters(0).grantedReady, 0)
    expect(r.routers(0).outWrite, 0)
    expect(r.routers(1).outWrite, 0)

    step(1)
    // Stop sending data
    poke(r.io.ports(0).inRequest, 0)
    poke(r.io.ports(1).inRequest, 0)
    poke(r.io.ports(0).inData, emptyPacket)
    poke(r.io.ports(1).inData, emptyPacket)
    poke(r.io.ports(0).outReady, 1)
    poke(r.io.ports(1).outReady, 1)

    // Cycle 1: Data is at head in input port and traverses through crossbar
    // The port granted to send over the crossbar should be east.in

    expect(r.io.ports(0).inReady, 1)
    expect(r.io.ports(1).inReady, 1)
    expect(r.io.ports(0).outRequest, 0) // output port should still be empty
    expect(r.io.ports(1).outRequest, 0)
    expect(r.io.ports(0).outData, emptyPacket)
    expect(r.io.ports(1).outData, emptyPacket)

    expect(r.arbiters(1).granted, East.litValue)
    expect(r.arbiters(1).grantedReady, 1)
    expect(r.arbiters(0).granted, North.litValue)
    expect(r.arbiters(0).grantedReady, 1)
    expect(r.routers(0).outWrite, 1)
    expect(r.routers(1).outWrite, 1)

    step(1)

    poke(r.io.ports(0).inRequest, 0)
    poke(r.io.ports(1).inRequest, 0)
    poke(r.io.ports(0).inData, emptyPacket)
    poke(r.io.ports(1).inData, emptyPacket)
    poke(r.io.ports(0).outReady, 1)
    poke(r.io.ports(1).outReady, 1)

    // Cycle 2: Data reaches the output of the output port, to send it
    // further on to the network
    expect(r.io.ports(0).inReady, 1)
    expect(r.io.ports(1).inReady, 1)
    expect(r.io.ports(0).outRequest, 1)
    expect(r.io.ports(1).outRequest, 1)
    expect(r.io.ports(0).outData, packetFromNorthToEast)
    expect(r.io.ports(1).outData, packetFromEastToNorth)

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
