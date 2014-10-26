package router

import Chisel._

class CrossBarIO(numPorts: Int) extends Bundle {
  val inData = Vec.fill(numPorts) { PacketData(INPUT) }
  val select = Vec.fill(numPorts) { UInt(INPUT, width = numPorts) }
  val outData = Vec.fill(numPorts) { PacketData(OUTPUT) }
}

class CrossBar(numPorts: Int) extends Module {
  val io = new CrossBarIO(numPorts)

  for (i <- 0 until numPorts) {
    io.outData(i) := UInt(0)

    when (io.select(i) != UInt(0)) {
      for (j <- 0 until numPorts) {
        when(io.select(i)(j)) {
          io.outData(i) := io.inData(j)
        }
      }
    }
  }
}

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
        expect(c.io.outData(i), value)
      } else {
        expect(c.io.outData(i), 0)
      }
    }
  }

  poke(c.io.inData(East.index), 1)
  poke(c.io.inData(North.index), 2)
  poke(c.io.inData(West.index), 3)
  poke(c.io.inData(South.index), 4)
  poke(c.io.inData(Local.index), 5)
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
  expect(c.io.outData(0), 4)
  expect(c.io.outData(1), 4)
  expect(c.io.outData(2), 4)
  expect(c.io.outData(3), 4)
  expect(c.io.outData(4), 4)

  step(1)

  // Test that all outputs read a different input
  poke(c.io.select(0), South.litValue)
  poke(c.io.select(1), Local.litValue)
  poke(c.io.select(2), North.litValue)
  poke(c.io.select(3), East.litValue)
  poke(c.io.select(4), West.litValue)
  expect(c.io.outData(0), 4)
  expect(c.io.outData(1), 5)
  expect(c.io.outData(2), 2)
  expect(c.io.outData(3), 1)
  expect(c.io.outData(4), 3)
}
