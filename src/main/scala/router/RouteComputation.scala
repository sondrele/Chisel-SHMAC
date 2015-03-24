package main.scala.router

import Chisel._

class RouteComputation() extends Module {
  val io = new Bundle() {
    val xCur  = UInt(INPUT, width=4)
    val xDest = UInt(INPUT, width=4)
    val yCur  = UInt(INPUT, width=4)
    val yDest = UInt(INPUT, width=4)
    val dest  = UInt(OUTPUT, width=5)
  }

  when(io.xCur < io.xDest) {
    io.dest := East.value
  }.elsewhen(io.xCur > io.xDest) {
    io.dest := West.value
  }.elsewhen(io.xCur === io.xDest && io.yCur < io.yDest) {
    io.dest := South.value
  }.elsewhen(io.xCur === io.xDest && io.yCur > io.yDest) {
    io.dest := North.value
  }.elsewhen(io.xCur === io.xDest && io.yCur === io.yDest) {
    io.dest := Local.value
  }.otherwise {
    io.dest := UInt(0)
  }
}
