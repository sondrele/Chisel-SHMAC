package test

import Chisel._
import router._

class CrossBarTest(c: CrossBar) extends Tester(c) {

  def testCrossBar(from: TileDir, to: TileDir, value: Int) {
    for (i <- 0 until 5) {
      if (to.index == i) {
        // OutputPort[i] chooses to read data from InputPort 'from'
        poke(c.io.select(i), from.litValue)
      } else {
        poke(c.io.select(i), 0)
      }
    }

    for (i <- 0 until 5) {
      if (i == to.index) {
        expect(c.io.outData(i).payload, value)
      } else {
        expect(c.io.outData(i).payload, 0)
      }
    }
  }

  poke(c.io.inData(East.index).payload, 1)
  poke(c.io.inData(North.index).payload, 2)
  poke(c.io.inData(West.index).payload, 3)
  poke(c.io.inData(South.index).payload, 4)
  poke(c.io.inData(Local.index).payload, 5)
  step(1)

  // One output can read from one input
  testCrossBar(East, East, 1)
  testCrossBar(East, North, 1)
  testCrossBar(East, South, 1)
  testCrossBar(North, East, 2)
  testCrossBar(West,  North, 3)
  testCrossBar(South, West,  4)
  testCrossBar(Local, North, 5)

  step(1)

  // Test that all outputs reads the same input
  poke(c.io.select(0), South.litValue)
  poke(c.io.select(1), South.litValue)
  poke(c.io.select(2), South.litValue)
  poke(c.io.select(3), South.litValue)
  poke(c.io.select(4), South.litValue)
  expect(c.io.outData(0).payload, 4)
  expect(c.io.outData(1).payload, 4)
  expect(c.io.outData(2).payload, 4)
  expect(c.io.outData(3).payload, 4)
  expect(c.io.outData(4).payload, 4)

  step(1)

  // Test that all outputs read a different input
  poke(c.io.select(0), South.litValue)
  poke(c.io.select(1), Local.litValue)
  poke(c.io.select(2), North.litValue)
  poke(c.io.select(3), East.litValue)
  poke(c.io.select(4), West.litValue)
  expect(c.io.outData(0).payload, 4)
  expect(c.io.outData(1).payload, 5)
  expect(c.io.outData(2).payload, 2)
  expect(c.io.outData(3).payload, 1)
  expect(c.io.outData(4).payload, 3)
}
