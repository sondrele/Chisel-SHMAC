package router

import Chisel._

class DirectionRouterIO extends Bundle {
  val port = new RouterPortIO()
  val inRead = Bool(INPUT)
  val outWrite = Bool(INPUT)
  val crossbarIn = new Packet().asOutput
  val crossbarOut = new Packet().asInput
  // Destination of the packet
  val direction = UInt(OUTPUT, width = 5)
  // Signals to arbiter
  val isEmpty = Bool(OUTPUT)
  val isFull = Bool(OUTPUT)
}

class DirectionRouter(tileX: UInt, tileY: UInt, numRecords: Int) extends Module {
  val io = new DirectionRouterIO()

  val input = Module(new InputPort(numRecords)).io
  input.fifo.enq.valid := io.port.in.valid
  input.fifo.deq.ready := io.inRead
  input.fifo.enq.bits.assign(io.port.in.bits)
  io.port.in.ready := input.fifo.enq.ready
  io.crossbarIn.assign(input.fifo.deq.bits)

  val output = Module(new OutputPort(numRecords)).io
  output.fifo.enq.valid := io.outWrite
  output.fifo.deq.ready := io.port.out.ready
  output.fifo.enq.bits.assign(io.crossbarOut)
  io.port.out.valid := output.fifo.deq.valid
  io.port.out.bits.assign(output.fifo.deq.bits)

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
