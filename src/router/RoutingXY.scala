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
    io.dest := East.value
  }.elsewhen(curXGreater) {
    io.dest := West.value
  }.elsewhen(curYSmaller) {
    io.dest := South.value
  }.elsewhen(curYGreater) {
    io.dest := North.value
  }.otherwise {
    io.dest := Local.value
  }
}

class RoutingXYTest(xy: RoutingXY) extends Tester(xy) {
  poke(xy.io.xDest, 1)
  poke(xy.io.yDest, 1)

  poke(xy.io.xCur, 0)
  poke(xy.io.yCur, 0)
  expect(xy.io.dest, East.value.litValue())

  poke(xy.io.xCur, 0)
  poke(xy.io.yCur, 2)
  expect(xy.io.dest, East.value.litValue())

  poke(xy.io.xCur, 2)
  poke(xy.io.yCur, 0)
  expect(xy.io.dest, West.value.litValue())

  poke(xy.io.xCur, 2)
  poke(xy.io.yCur, 2)
  expect(xy.io.dest, West.value.litValue())

  poke(xy.io.xCur, 1)
  poke(xy.io.yCur, 0)
  expect(xy.io.dest, South.value.litValue())

  poke(xy.io.xCur, 1)
  poke(xy.io.yCur, 2)
  expect(xy.io.dest, North.value.litValue())

  poke(xy.io.xCur, 1)
  poke(xy.io.yCur, 1)
  expect(xy.io.dest, Local.value.litValue())
}
