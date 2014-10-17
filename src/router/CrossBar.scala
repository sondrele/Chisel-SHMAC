package router

import Chisel._

class CrossBarIO extends Bundle {
  val inData = Vec.fill(5) { PacketData(INPUT) }
  val select = Vec.fill(5) { UInt(INPUT, width = 5) }
  val outData = Vec.fill(5) { PacketData(OUTPUT) }
}

class CrossBar extends Module {
  val io = new CrossBarIO()

  val fromEast  = io.select(East.index) != UInt(0)
  val fromNorth = io.select(North.index) != UInt(0)
  val fromWest  = io.select(West.index) != UInt(0)
  val fromSouth = io.select(South.index) != UInt(0)
  val fromLocal = io.select(Local.index) != UInt(0)

  // Find out which input ports sends data
  val filter = UInt()
  val inData = PacketData()
  when(fromEast) {
    filter := io.select(East.index)
    inData := io.inData(East.index)
  }.elsewhen(fromNorth) {
    filter := io.select(North.index)
    inData := io.inData(North.index)
  }.elsewhen(fromWest) {
    filter := io.select(West.index)
    inData := io.inData(West.index)
  }.elsewhen(fromSouth) {
    filter := io.select(South.index)
    inData := io.inData(South.index)
  }.elsewhen(fromLocal) {
    filter := io.select(Local.index)
    inData := io.inData(Local.index)
  }.otherwise {
    filter := UInt(0)
    inData := UInt(0)
  }

  // Default all outputs to 0
  for (i <- 0 until 5) {
    io.outData(i) := UInt(0)

    when(filter(i) === UInt(1)) {
      io.outData(i) := inData
    }
  }
}

class CrossBarTest(c: CrossBar) extends Tester(c) {

  def testCrossBar(from: TileDir, to: TileDir, value: Int) {
    for (i <- 0 until 5) {
      if (i == from.index) {
        poke(c.io.select(i), to.litValue)
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

  testCrossBar(East, East, 1)
  testCrossBar(North, East, 2)
  testCrossBar(East, South, 1)
  testCrossBar(North, East,  2)
  testCrossBar(West,  North, 3)
  testCrossBar(South, West,  4)
  testCrossBar(Local, North, 5)
}
