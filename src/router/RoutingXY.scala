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
    io.dest := Direction.east
  }.elsewhen(curXGreater) {
    io.dest := Direction.west
  }.elsewhen(curYSmaller) {
    io.dest := Direction.south
  }.elsewhen(curYGreater) {
    io.dest := Direction.north
  }.otherwise {
    io.dest := Direction.local
  }
}

class RoutingXYTest(xy: RoutingXY) extends Tester(xy) {
  poke(xy.io.xDest, 1)
  poke(xy.io.yDest, 1)

  poke(xy.io.xCur, 0)
  poke(xy.io.yCur, 0)
  expect(xy.io.dest, Direction.east.litValue())

  poke(xy.io.xCur, 0)
  poke(xy.io.yCur, 2)
  expect(xy.io.dest, Direction.east.litValue())

  poke(xy.io.xCur, 2)
  poke(xy.io.yCur, 0)
  expect(xy.io.dest, Direction.west.litValue())

  poke(xy.io.xCur, 2)
  poke(xy.io.yCur, 2)
  expect(xy.io.dest, Direction.west.litValue())

  poke(xy.io.xCur, 1)
  poke(xy.io.yCur, 0)
  expect(xy.io.dest, Direction.south.litValue())

  poke(xy.io.xCur, 1)
  poke(xy.io.yCur, 2)
  expect(xy.io.dest, Direction.north.litValue())

  poke(xy.io.xCur, 1)
  poke(xy.io.yCur, 1)
  expect(xy.io.dest, Direction.local.litValue())
}
