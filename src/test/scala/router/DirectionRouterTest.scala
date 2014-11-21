package router

import Chisel._
import main.scala.router.{North, East, DirectionRouter}

class DirectionRouterTest(a: DirectionRouter) extends Tester(a) {
  // Act like this is input/output from/to North, but packet is going east
  def emptyPacket = Array[BigInt](0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

  def packetFromNorthToEast = Array[BigInt](10, 0, 0, 0, 0, 0, 0, 1, 0, 2, 1)

  def packetFromNorthToNorth = Array[BigInt](10, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0)

  // Packet comes in, no valid data in module
  poke(a.io.port.in.valid, 1)
  poke(a.io.port.in.bits, packetFromNorthToEast)
  poke(a.io.inRead, 0)

  expect(a.io.crossbarIn, emptyPacket)
  expect(a.io.port.in.ready, 1)
  expect(a.io.port.out.valid, 0)
  expect(a.io.port.out.bits, emptyPacket)

  poke(a.io.outWrite, 0)
  poke(a.io.crossbarOut, emptyPacket)
  poke(a.io.port.out.ready, 0)

  expect(a.io.direction, 0)
  expect(a.io.isEmpty, 1)
  expect(a.io.isFull, 0)

  step(1)

  poke(a.io.port.in.valid, 1)
  poke(a.io.port.in.bits, packetFromNorthToNorth)
  poke(a.io.inRead, 1)

  expect(a.input.fifo.deq.bits, packetFromNorthToEast)
  expect(a.io.crossbarIn, packetFromNorthToEast)
  expect(a.io.port.in.ready, 1)
  expect(a.io.port.out.valid, 0)
  expect(a.io.port.out.bits.header.address, 0)

  poke(a.io.outWrite, 0)
  poke(a.io.crossbarOut.header.address, 0)
  poke(a.io.port.out.ready, 0)

  expect(a.io.direction, East.litValue)
  expect(a.io.isEmpty, 0)
  expect(a.io.isFull, 0)

  step(1)

  poke(a.io.port.in.valid, 0)
  poke(a.io.port.in.bits.header.address, 0)
  poke(a.io.inRead, 1)

  expect(a.input.fifo.deq.bits, packetFromNorthToNorth)
  expect(a.io.crossbarIn, packetFromNorthToNorth)
  expect(a.io.port.in.ready, 1)
  expect(a.io.port.out.valid, 0)
  expect(a.io.port.out.bits.header.address, 0)

  poke(a.io.outWrite, 1)
  poke(a.io.crossbarOut, packetFromNorthToNorth)
  poke(a.io.port.out.ready, 1)

  expect(a.io.direction, North.litValue)
  expect(a.io.isEmpty, 0)
  expect(a.io.isFull, 0)

  step(1)

  poke(a.io.port.in.valid, 0)
  poke(a.io.port.in.bits.header.address, 0)
  poke(a.io.inRead, 0)
  expect(a.input.fifo.deq.bits, emptyPacket)
  expect(a.io.crossbarIn, emptyPacket)
  expect(a.io.port.in.ready, 1)
  expect(a.io.port.out.valid, 1)
  expect(a.io.port.out.bits, packetFromNorthToNorth)

  poke(a.io.outWrite, 0)
  poke(a.io.crossbarOut, emptyPacket)
  poke(a.io.port.out.ready, 0)

  expect(a.io.direction, 0)
  expect(a.io.isEmpty, 1)
  expect(a.io.isFull, 0)
}
