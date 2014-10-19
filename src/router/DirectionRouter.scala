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
  val crossbarOut = PacketData(INPUT)
  val outReady = Bool(INPUT)
  // Destination of the packet
  val destTile = UInt(OUTPUT, width = 5)
  // Signals to arbiter
  val isEmpty = Bool(OUTPUT)
  val requesting = Bool(OUTPUT)
  val isFull = Bool(OUTPUT)
}

class DirectionRouter(tileX: UInt, tileY: UInt, numRecords: Int) extends Module {
  val io = new DirectionRouterIO()

  val input = Module(new InputPort(numRecords))
  input.io.fifo.in.valid := io.inRequest
  input.io.fifo.out.ready := io.inRead // Bool(true) // read only when grt_to_out_ports for this instance is true
  input.io.fifo.in.bits := io.inData
  io.inReady := input.io.fifo.in.ready
  io.crossbarIn := input.io.fifo.out.bits

  val output = Module(new OutputPort(numRecords))
  output.io.fifo.in.valid := Bool(true) // Router instance always writing output
  output.io.fifo.out.ready := io.outReady
  output.io.fifo.in.bits := io.crossbarOut
  io.outRequest := output.io.fifo.out.valid
  io.outData := output.io.fifo.out.bits

  val destRoute = Module(new RouteComputation())
  destRoute.io.xCur := tileX
  destRoute.io.yCur := tileY
  destRoute.io.xDest := input.io.xDest
  destRoute.io.yDest := input.io.yDest
  io.destTile := destRoute.io.dest

  io.isEmpty := !input.io.fifo.out.valid
  io.requesting := input.io.fifo.out.valid && input.io.fifo.out.ready
  io.isFull := !output.io.fifo.in.ready
}

class DirectionRouterTest(a: DirectionRouter) extends Tester(a) {
}
