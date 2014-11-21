package router

import Chisel._
import main.scala.router.{North, East, Router}

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
    poke(r.io.ports(0).in.valid, 1)
    poke(r.io.ports(1).in.valid, 0)
    poke(r.io.ports(0).in.bits, packetFromEastToNorth)
    poke(r.io.ports(1).in.bits, packetFromNorthToEast)
    poke(r.io.ports(0).out.ready, 0)
    poke(r.io.ports(1).out.ready, 0)

    // Cycle 0: Data arrives router and input port
    expect(r.io.ports(0).in.ready, 1)
    expect(r.io.ports(1).in.ready, 1)
    expect(r.io.ports(0).out.valid, 0) // output port should be empty
    expect(r.io.ports(1).out.valid, 0)
    expect(r.io.ports(0).out.bits, emptyPacket)
    expect(r.io.ports(1).out.bits, emptyPacket)

    expect(r.arbiters(1).granted, 0)
    expect(r.arbiters(1).grantedReady, 0)
    expect(r.arbiters(0).granted, 0)
    expect(r.arbiters(0).grantedReady, 0)
    expect(r.routers(0).outWrite, 0)
    expect(r.routers(1).outWrite, 0)

    step(1)
    // Stop sending data
    poke(r.io.ports(0).in.valid, 0)
    poke(r.io.ports(1).in.valid, 1)
    poke(r.io.ports(0).in.bits, emptyPacket)
    poke(r.io.ports(1).in.bits, packetFromNorthToEast)

    poke(r.io.ports(0).out.ready, 1)
    poke(r.io.ports(1).out.ready, 1)

    // Cycle 1: Data is at head in input port and traverses through crossbar
    // The port granted to send over the crossbar should be east.in
    expect(r.io.ports(0).in.ready, 1)
    expect(r.io.ports(1).in.ready, 1)
    expect(r.io.ports(0).out.valid, 0) // output port should still be empty
    expect(r.io.ports(1).out.valid, 0)
    expect(r.io.ports(0).out.bits, emptyPacket)
    expect(r.io.ports(1).out.bits, emptyPacket)

    expect(r.arbiters(1).granted, East.litValue)
    expect(r.arbiters(1).grantedReady, 1)
    expect(r.arbiters(0).granted, 0)
    expect(r.arbiters(0).grantedReady, 0)
    expect(r.routers(0).outWrite, 0)
    expect(r.routers(1).outWrite, 1)

    step(1)

    poke(r.io.ports(0).in.valid, 0)
    poke(r.io.ports(1).in.valid, 0)
    poke(r.io.ports(0).in.bits, emptyPacket)
    poke(r.io.ports(1).in.bits, emptyPacket)
    poke(r.io.ports(0).out.ready, 1)
    poke(r.io.ports(1).out.ready, 1)

    // Cycle 2: Data reaches the output of the output port, to send it
    // further on to the network
    expect(r.io.ports(0).in.ready, 1)
    expect(r.io.ports(1).in.ready, 1)
    expect(r.io.ports(0).out.valid, 0)
    expect(r.io.ports(1).out.valid, 1)
    expect(r.io.ports(0).out.bits, emptyPacket)
    expect(r.io.ports(1).out.bits, packetFromEastToNorth)

    expect(r.arbiters(1).granted, 0)
    expect(r.arbiters(1).grantedReady, 0)
    expect(r.arbiters(0).granted, North.litValue)
    expect(r.arbiters(0).grantedReady, 1)
    expect(r.routers(0).outWrite, 1)
    expect(r.routers(1).outWrite, 0)

    step(1)

    poke(r.io.ports(0).in.valid, 0)
    poke(r.io.ports(1).in.valid, 0)
    poke(r.io.ports(0).in.bits, emptyPacket)
    poke(r.io.ports(1).in.bits, emptyPacket)
    poke(r.io.ports(0).out.ready, 1)
    poke(r.io.ports(1).out.ready, 1)

    expect(r.io.ports(0).in.ready, 1)
    expect(r.io.ports(1).in.ready, 1)
    expect(r.io.ports(0).out.valid, 1)
    expect(r.io.ports(1).out.valid, 0)
    expect(r.io.ports(0).out.bits, packetFromNorthToEast)
    expect(r.io.ports(1).out.bits, emptyPacket)

    step(1)
  }

  // Sends two packets at the same cycle, essentially the same test as the other one
  // One from the east input port to the north output port
  // One from the north input port to the east output port
  def testSendingTwoPacketsAtTheSameTime() {
    poke(r.io.ports(0).in.valid, 1)
    poke(r.io.ports(1).in.valid, 1)
    poke(r.io.ports(0).in.bits, packetFromEastToNorth)
    poke(r.io.ports(1).in.bits, packetFromNorthToEast)
    poke(r.io.ports(0).out.ready, 0)
    poke(r.io.ports(1).out.ready, 0)

    // Cycle 0: Data arrives router and input port
    expect(r.io.ports(0).in.ready, 1)
    expect(r.io.ports(1).in.ready, 1)
    expect(r.io.ports(0).out.valid, 0) // output port should be empty
    expect(r.io.ports(1).out.valid, 0)
    expect(r.io.ports(0).out.bits, emptyPacket)
    expect(r.io.ports(1).out.bits, emptyPacket)

    expect(r.arbiters(1).granted, 0)
    expect(r.arbiters(1).grantedReady, 0)
    expect(r.arbiters(0).granted, 0)
    expect(r.arbiters(0).grantedReady, 0)
    expect(r.routers(0).outWrite, 0)
    expect(r.routers(1).outWrite, 0)

    step(1)
    // Stop sending data
    poke(r.io.ports(0).in.valid, 0)
    poke(r.io.ports(1).in.valid, 0)
    poke(r.io.ports(0).in.bits, emptyPacket)
    poke(r.io.ports(1).in.bits, emptyPacket)
    poke(r.io.ports(0).out.ready, 1)
    poke(r.io.ports(1).out.ready, 1)

    // Cycle 1: Data is at head in input port and traverses through crossbar
    // The port granted to send over the crossbar should be east.in

    expect(r.io.ports(0).in.ready, 1)
    expect(r.io.ports(1).in.ready, 1)
    expect(r.io.ports(0).out.valid, 0) // output port should still be empty
    expect(r.io.ports(1).out.valid, 0)
    expect(r.io.ports(0).out.bits, emptyPacket)
    expect(r.io.ports(1).out.bits, emptyPacket)

    expect(r.arbiters(1).granted, East.litValue)
    expect(r.arbiters(1).grantedReady, 1)
    expect(r.arbiters(0).granted, North.litValue)
    expect(r.arbiters(0).grantedReady, 1)
    expect(r.routers(0).outWrite, 1)
    expect(r.routers(1).outWrite, 1)

    step(1)

    poke(r.io.ports(0).in.valid, 0)
    poke(r.io.ports(1).in.valid, 0)
    poke(r.io.ports(0).in.bits, emptyPacket)
    poke(r.io.ports(1).in.bits, emptyPacket)
    poke(r.io.ports(0).out.ready, 1)
    poke(r.io.ports(1).out.ready, 1)

    // Cycle 2: Data reaches the output of the output port, to send it
    // further on to the network
    expect(r.io.ports(0).in.ready, 1)
    expect(r.io.ports(1).in.ready, 1)
    expect(r.io.ports(0).out.valid, 1)
    expect(r.io.ports(1).out.valid, 1)
    expect(r.io.ports(0).out.bits, packetFromNorthToEast)
    expect(r.io.ports(1).out.bits, packetFromEastToNorth)

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
