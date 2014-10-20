package router

import Chisel._

class DirectionRouterIO extends Bundle {
  val inRequest = Bool(INPUT)
  val inData = PacketData(INPUT)
  val inRead = Bool(INPUT)
  val crossbarIn = PacketData(OUTPUT)
  val inReady = Bool(OUTPUT)
  val outRequest = Bool(OUTPUT)
  val outData = PacketData(OUTPUT)
  val outWrite = Bool(INPUT)
  val crossbarOut = PacketData(INPUT)
  val outReady = Bool(INPUT)
  // Destination of the packet
  val direction = UInt(OUTPUT, width = 5)
  // Signals to arbiter
  val isEmpty = Bool(OUTPUT)
  // val requesting = Bool(OUTPUT)
  val isFull = Bool(OUTPUT)
}

class DirectionRouter(tileX: UInt, tileY: UInt, numRecords: Int) extends Module {
  val io = new DirectionRouterIO()

  val input = Module(new InputPort(numRecords)).io
  input.fifo.in.valid := io.inRequest
  input.fifo.out.ready := io.inRead // Bool(true) // read only when grt_to_out_ports for this instance is true
  input.fifo.in.bits := io.inData
  io.inReady := input.fifo.in.ready
  io.crossbarIn := input.fifo.out.bits

  val output = Module(new OutputPort(numRecords)).io
  output.fifo.in.valid := io.outWrite // Bool(true) // Router instance always writing output
  output.fifo.out.ready := io.outReady
  output.fifo.in.bits := io.crossbarOut
  io.outRequest := output.fifo.out.valid
  io.outData := output.fifo.out.bits

  val destRoute = Module(new RouteComputation()).io
  destRoute.xCur := tileX
  destRoute.yCur := tileY
  destRoute.xDest := input.xDest
  destRoute.yDest := input.yDest
  io.direction := destRoute.dest

  io.isEmpty := !input.fifo.out.valid
  // io.requesting := input.fifo.out.valid && input.fifo.out.ready
  io.isFull := !output.fifo.in.ready
}

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
  // Direction is west because location is (1, 1) and input.fifo has 0-value
  expect(a.io.direction, West.litValue)
  expect(a.io.isEmpty, 1)
  expect(a.io.isFull, 0)

  step(1)

  poke(a.io.inRequest, 1)
  poke(a.io.inData, packetFromNorthToNorth)
  poke(a.io.inRead, 1)
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
  expect(a.io.crossbarIn, 0)
  expect(a.io.inReady, 1)
  expect(a.io.outRequest, 1)
  expect(a.io.outData, packetFromNorthToNorth)
  poke(a.io.outWrite, 0)
  poke(a.io.crossbarOut, 0)
  poke(a.io.outReady, 0)
  expect(a.io.direction, West.litValue)
  expect(a.io.isEmpty, 1)
  expect(a.io.isFull, 0)
}
