package router

import Chisel._

class DirectionRouterIO extends Bundle {
  val inRequest = Bool(INPUT)
  val inData = new PacketBundle().asInput
  val inRead = Bool(INPUT)
  val crossbarIn = new PacketBundle().asOutput
  val inReady = Bool(OUTPUT)
  val outRequest = Bool(OUTPUT)
  val outData = new PacketBundle().asOutput
  val outWrite = Bool(INPUT)
  val crossbarOut = new PacketBundle().asInput
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
  input.fifo.enq.valid := io.inRequest
  input.fifo.deq.ready := io.inRead
  input.fifo.enq.bits.assign(io.inData)
  io.inReady := input.fifo.enq.ready
  io.crossbarIn.assign(input.fifo.deq.bits)

  val output = Module(new OutputPort(numRecords)).io
  output.fifo.enq.valid := io.outWrite
  output.fifo.deq.ready := io.outReady
  output.fifo.enq.bits.assign(io.crossbarOut)
  io.outRequest := output.fifo.deq.valid
  io.outData.assign(output.fifo.deq.bits)

  val destRoute = Module(new RouteComputation()).io
  destRoute.xCur := tileX
  destRoute.yCur := tileY
  destRoute.xDest := input.xDest
  destRoute.yDest := input.yDest

  when (input.fifo.deq.valid) {
    io.direction := destRoute.dest
  }.otherwise {
    io.direction := UInt(0)
  }

  io.isEmpty := !input.fifo.deq.valid
  io.isFull := !output.fifo.enq.ready
}
