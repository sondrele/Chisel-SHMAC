package router

import Chisel._

class RoutingXY() extends Module {
  val io = new Bundle() {
    val xCur  = UInt(INPUT, width=4)
    val xDest = UInt(INPUT, width=4)
    val yCur  = UInt(INPUT, width=4)
    val yDest = UInt(INPUT, width=4)
    val dest  = UInt(OUTPUT, width=5)
  }

  val curXSmaller = (io.xCur < io.xDest)
  val curXGreater = (io.xCur > io.xDest)
  val curYSmaller = (io.yCur < io.yDest)
  val curYGreater = (io.yCur > io.yDest)

  when(curXSmaller) {
    io.dest := UInt(0) // East
  }.elsewhen(curXGreater) {
    io.dest := UInt(2) // West
  }.elsewhen(curYSmaller) {
    io.dest := UInt(3) // South
  }.elsewhen(curYGreater) {
    io.dest := UInt(1) // North
  }.otherwise {
    io.dest := UInt(4) // Local
  }
}

class RoutingXYTest(xy: RoutingXY) extends Tester(xy) {
  poke(xy.io.xDest, 1)
  poke(xy.io.yDest, 1)

  poke(xy.io.xCur, 0)
  poke(xy.io.yCur, 0)
  expect(xy.io.dest, 0) // Go east

  poke(xy.io.xCur, 0)
  poke(xy.io.yCur, 2)
  expect(xy.io.dest, 0) // Go east

  poke(xy.io.xCur, 2)
  poke(xy.io.yCur, 0)
  expect(xy.io.dest, 2) // Go west

  poke(xy.io.xCur, 2)
  poke(xy.io.yCur, 2)
  expect(xy.io.dest, 2) // Go west

  poke(xy.io.xCur, 1)
  poke(xy.io.yCur, 0)
  expect(xy.io.dest, 3) // Go south

  poke(xy.io.xCur, 1)
  poke(xy.io.yCur, 2)
  expect(xy.io.dest, 1) // Go north

  poke(xy.io.xCur, 1)
  poke(xy.io.yCur, 1)
  expect(xy.io.dest, 4) // Go local
}
