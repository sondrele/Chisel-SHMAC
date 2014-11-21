package router

import Chisel._
import main.scala.router._

class CrossBarTest(c: CrossBar) extends Tester(c) {

  def testCrossBar(from: TileDir, to: TileDir, value: Int) {
    for (i <- 0 until 5) {
      if (to.index == i) {
        // OutputPort[i] chooses to read data from InputPort 'from'
        poke(c.io.port(i).select, from.litValue)
      } else {
        poke(c.io.port(i).select, 0)
      }
    }

    for (i <- 0 until 5) {
      if (i == to.index) {
        expect(c.io.port(i).outData.payload, value)
      } else {
        expect(c.io.port(i).outData.payload, 0)
      }
    }
  }

  poke(c.io.port(East.index).inData.payload, 1)
  poke(c.io.port(North.index).inData.payload, 2)
  poke(c.io.port(West.index).inData.payload, 3)
  poke(c.io.port(South.index).inData.payload, 4)
  poke(c.io.port(Local.index).inData.payload, 5)
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
  poke(c.io.port(0).select, South.litValue)
  poke(c.io.port(1).select, South.litValue)
  poke(c.io.port(2).select, South.litValue)
  poke(c.io.port(3).select, South.litValue)
  poke(c.io.port(4).select, South.litValue)
  expect(c.io.port(0).outData.payload, 4)
  expect(c.io.port(1).outData.payload, 4)
  expect(c.io.port(2).outData.payload, 4)
  expect(c.io.port(3).outData.payload, 4)
  expect(c.io.port(4).outData.payload, 4)

  step(1)

  // Test that all outputs read a different input
  poke(c.io.port(0).select, South.litValue)
  poke(c.io.port(1).select, Local.litValue)
  poke(c.io.port(2).select, North.litValue)
  poke(c.io.port(3).select, East.litValue)
  poke(c.io.port(4).select, West.litValue)
  expect(c.io.port(0).outData.payload, 4)
  expect(c.io.port(1).outData.payload, 5)
  expect(c.io.port(2).outData.payload, 2)
  expect(c.io.port(3).outData.payload, 1)
  expect(c.io.port(4).outData.payload, 3)
}
