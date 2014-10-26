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
  val isFull = Bool(OUTPUT)
}

class DirectionRouter(tileX: UInt, tileY: UInt, numRecords: Int) extends Module {
  val io = new DirectionRouterIO()

  val input = Module(new InputPort(numRecords)).io
  input.fifo.in.valid := io.inRequest
  input.fifo.out.ready := io.inRead
  input.fifo.in.bits := io.inData
  io.inReady := input.fifo.in.ready
  io.crossbarIn := input.fifo.out.bits

  val output = Module(new OutputPort(numRecords)).io
  output.fifo.in.valid := io.outWrite
  output.fifo.out.ready := io.outReady
  output.fifo.in.bits := io.crossbarOut
  io.outRequest := output.fifo.out.valid
  io.outData := output.fifo.out.bits

  val destRoute = Module(new RouteComputation()).io
  destRoute.xCur := tileX
  destRoute.yCur := tileY
  destRoute.xDest := input.xDest
  destRoute.yDest := input.yDest

  when (input.fifo.out.valid) {
    io.direction := destRoute.dest
  }.otherwise {
    io.direction := UInt(0)
  }

  io.isEmpty := !input.fifo.out.valid
  io.isFull := !output.fifo.in.ready
}
