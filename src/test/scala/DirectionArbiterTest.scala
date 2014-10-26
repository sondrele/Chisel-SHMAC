package test

import Chisel._
import router._

class DirectionArbiterTest(a: DirectionArbiter) extends Tester(a) {
  def initArbiterIO() {
    poke(a.io.isEmpty(East.index), 0)
    poke(a.io.requesting(East.index), 1)
    poke(a.io.isEmpty(North.index), 0)
    poke(a.io.requesting(North.index), 1)
    poke(a.io.isEmpty(West.index), 1)
    poke(a.io.requesting(West.index), 1)
    poke(a.io.isEmpty(South.index), 0)
    poke(a.io.requesting(South.index), 0)
    poke(a.io.isEmpty(Local.index), 0)
    poke(a.io.requesting(Local.index), 1)
    poke(a.io.isFull, 0)
  }

  initArbiterIO()

  step(1)
  expect(a.io.granted, Local.value.litValue())
  step(1)
  expect(a.io.granted, East.value.litValue())
  step(1)
  expect(a.io.granted, North.value.litValue())
  step(1)
  poke(a.io.isEmpty(East.index), 1)
  expect(a.io.granted, Local.value.litValue())
  step(1)
  expect(a.io.granted, North.value.litValue())
  step(1)
  expect(a.io.granted, Local.value.litValue())
}
