package test

import Chisel._
import router._

class DirectionRouterTest(a: DirectionRouter) extends Tester(a) {
  // Act like this is input/output from/to North, but packet is going east
  val packetFromNorthToEast = PacketData.create(
    address = 10,
    xDest = 2,
    yDest = 1,
    xSender = 1,
    ySender = 0
  ).litValue()

  val packetFromNorthToNorth = PacketData.create(
    address = 10,
    xDest = 1,
    yDest = 0,
    xSender = 1,
    ySender = 0
  ).litValue()

  // Packet comes in, no valid data in module
  poke(a.io.inRequest, 1)
  poke(a.io.inData, packetFromNorthToEast)
  poke(a.io.inRead, 0)
  expect(a.io.crossbarIn, 0)
  expect(a.io.inReady, 1)
  expect(a.io.outRequest, 0)
  expect(a.io.outData, 0)
  poke(a.io.outWrite, 0)
  poke(a.io.crossbarOut, 0)
  poke(a.io.outReady, 0)
  expect(a.io.direction, 0)
  expect(a.io.isEmpty, 1)
  expect(a.io.isFull, 0)

  step(1)

  poke(a.io.inRequest, 1)
  poke(a.io.inData, packetFromNorthToNorth)
  poke(a.io.inRead, 1)
  expect(a.input.fifo.out.bits, packetFromNorthToEast)
  expect(a.io.crossbarIn, packetFromNorthToEast)
  expect(a.io.inReady, 1)
  expect(a.io.outRequest, 0)
  expect(a.io.outData, 0)
  poke(a.io.outWrite, 0)
  poke(a.io.crossbarOut, 0)
  poke(a.io.outReady, 0)
  expect(a.io.direction, East.litValue)
  expect(a.io.isEmpty, 0)
  expect(a.io.isFull, 0)

  step(1)

  poke(a.io.inRequest, 0)
  poke(a.io.inData, 0)
  poke(a.io.inRead, 1)
  expect(a.input.fifo.out.bits, packetFromNorthToNorth)
  expect(a.io.crossbarIn, packetFromNorthToNorth)
  expect(a.io.inReady, 1)
  expect(a.io.outRequest, 0)
  expect(a.io.outData, 0)
  poke(a.io.outWrite, 1)
  poke(a.io.crossbarOut, packetFromNorthToNorth)
  poke(a.io.outReady, 1)
  expect(a.io.direction, North.litValue)
  expect(a.io.isEmpty, 0)
  expect(a.io.isFull, 0)

  step(1)

  poke(a.io.inRequest, 0)
  poke(a.io.inData, 0)
  poke(a.io.inRead, 0)
  expect(a.input.fifo.out.bits, 0)
  expect(a.io.crossbarIn, 0)
  expect(a.io.inReady, 1)
  expect(a.io.outRequest, 1)
  expect(a.io.outData, packetFromNorthToNorth)
  poke(a.io.outWrite, 0)
  poke(a.io.crossbarOut, 0)
  poke(a.io.outReady, 0)
  expect(a.io.direction, 0)
  expect(a.io.isEmpty, 1)
  expect(a.io.isFull, 0)
}
