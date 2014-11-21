package router

import Chisel._
import main.scala.router._

class RouteComputationTest(xy: RouteComputation) extends Tester(xy) {
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
