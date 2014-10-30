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

  // poke(a.io.inData, packetFromNorthToEast)
  poke(a.io.inData.header.address, 10)
  poke(a.io.inData.dest.x, 2)
  poke(a.io.inData.dest.y, 1)
  poke(a.io.inData.sender.x, 1)
  poke(a.io.inData.sender.y, 0)

  poke(a.io.inRead, 0)
  expect(a.io.crossbarIn.header.address, 0)
  expect(a.io.inReady, 1)
  expect(a.io.outRequest, 0)
  expect(a.io.outData.header.address, 0)
  poke(a.io.outWrite, 0)
  poke(a.io.crossbarOut.header.address, 0)
  poke(a.io.outReady, 0)
  expect(a.io.direction, 0)
  expect(a.io.isEmpty, 1)
  expect(a.io.isFull, 0)

  step(1)

  poke(a.io.inRequest, 1)
  // poke(a.io.inData, packetFromNorthToNorth)
  poke(a.io.inData.header.address, 10)
  poke(a.io.inData.dest.x, 1)
  poke(a.io.inData.dest.y, 0)
  poke(a.io.inData.sender.x, 1)
  poke(a.io.inData.sender.y, 0)

  poke(a.io.inRead, 1)

  // expect(a.input.fifo.out.bits, packetFromNorthToEast)
  expect(a.input.fifo.out.bits.header.address, 10)
  expect(a.input.fifo.out.bits.dest.x, 2)
  expect(a.input.fifo.out.bits.dest.y, 1)
  expect(a.input.fifo.out.bits.sender.x, 1)
  expect(a.input.fifo.out.bits.sender.y, 0)

  // expect(a.io.crossbarIn, packetFromNorthToEast)
  expect(a.io.crossbarIn.header.address, 10)
  expect(a.io.crossbarIn.dest.x, 2)
  expect(a.io.crossbarIn.dest.y, 1)
  expect(a.io.crossbarIn.sender.x, 1)
  expect(a.io.crossbarIn.sender.y, 0)

  expect(a.io.inReady, 1)
  expect(a.io.outRequest, 0)
  expect(a.io.outData.header.address, 0)
  poke(a.io.outWrite, 0)
  poke(a.io.crossbarOut.header.address, 0)
  poke(a.io.outReady, 0)
  expect(a.io.direction, East.litValue)
  expect(a.io.isEmpty, 0)
  expect(a.io.isFull, 0)

  step(1)

  poke(a.io.inRequest, 0)
  poke(a.io.inData.header.address, 0)
  poke(a.io.inRead, 1)


  // expect(a.input.fifo.out.bits, packetFromNorthToNorth)
  expect(a.input.fifo.out.bits.header.address, 10)
  expect(a.input.fifo.out.bits.dest.x, 1)
  expect(a.input.fifo.out.bits.dest.y, 0)
  expect(a.input.fifo.out.bits.sender.x, 1)
  expect(a.input.fifo.out.bits.sender.y, 0)

  // expect(a.io.crossbarIn, packetFromNorthToNorth)
  expect(a.io.crossbarIn.header.address, 10)
  expect(a.io.crossbarIn.dest.x, 1)
  expect(a.io.crossbarIn.dest.y, 0)
  expect(a.io.crossbarIn.sender.x, 1)
  expect(a.io.crossbarIn.sender.y, 0)

  expect(a.io.inReady, 1)
  expect(a.io.outRequest, 0)
  expect(a.io.outData.header.address, 0)
  poke(a.io.outWrite, 1)

  // poke(a.io.crossbarOut, packetFromNorthToNorth)
  poke(a.io.crossbarOut.header.address, 10)
  poke(a.io.crossbarOut.dest.x, 1)
  poke(a.io.crossbarOut.dest.y, 0)
  poke(a.io.crossbarOut.sender.x, 1)
  poke(a.io.crossbarOut.sender.y, 0)

  poke(a.io.outReady, 1)
  expect(a.io.direction, North.litValue)
  expect(a.io.isEmpty, 0)
  expect(a.io.isFull, 0)

  step(1)

  poke(a.io.inRequest, 0)
  poke(a.io.inData.header.address, 0)
  poke(a.io.inRead, 0)
  expect(a.input.fifo.out.bits.header.address, 0)
  expect(a.io.crossbarIn.header.address, 0)
  expect(a.io.inReady, 1)
  expect(a.io.outRequest, 1)

  // expect(a.io.outData, packetFromNorthToNorth)
  expect(a.io.outData.header.address, 10)
  expect(a.io.outData.dest.x, 1)
  expect(a.io.outData.dest.y, 0)
  expect(a.io.outData.sender.x, 1)
  expect(a.io.outData.sender.y, 0)

  poke(a.io.outWrite, 0)
  poke(a.io.crossbarOut.header.address, 0)
  poke(a.io.outReady, 0)
  expect(a.io.direction, 0)
  expect(a.io.isEmpty, 1)
  expect(a.io.isFull, 0)
}
