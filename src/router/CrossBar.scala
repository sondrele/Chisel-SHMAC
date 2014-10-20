package router

import Chisel._

class CrossBarIO extends Bundle {
  val inData = Vec.fill(5) { PacketData(INPUT) }
  val select = Vec.fill(5) { UInt(INPUT, width = 5) }
  val outData = Vec.fill(5) { PacketData(OUTPUT) }
}

class CrossBar extends Module {
  val io = new CrossBarIO()

  def toDir(dir: TileDir): Bool = io.select(dir.index) != UInt(0)

  def fromDir(dir: TileDir): Bool = filter === dir.value

  // Find out which input ports sends data
  val filter = UInt() // port to read from
  when(toDir(East)) {
    filter := io.select(East.index)
  }.elsewhen(toDir(North)) {
    filter := io.select(North.index)
  }.elsewhen(toDir(West)) {
    filter := io.select(West.index)
  }.elsewhen(toDir(South)) {
    filter := io.select(South.index)
  }.elsewhen(toDir(Local)) {
    filter := io.select(Local.index)
  }.otherwise {
    filter := UInt(0)
  }

  val inData = PacketData()
  when (fromDir(East)) {
    inData := io.inData(East.index)
  }.elsewhen(fromDir(North)) {
    inData := io.inData(North.index)
  }.elsewhen(fromDir(West)) {
    inData := io.inData(West.index)
  }.elsewhen(fromDir(South)) {
    inData := io.inData(South.index)
  }.elsewhen(fromDir(Local)) {
    inData := io.inData(Local.index)
  }.otherwise {
    inData := UInt(0)
  }

  // Default all outputs to 0
  for (i <- 0 until 5) {
    io.outData(i) := UInt(0)

    when(io.select(i) != UInt(0)) {
      io.outData(i) := inData
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


  testCrossBar(East, East, 1)
  testCrossBar(East, North, 1)
  testCrossBar(East, South, 1)
  testCrossBar(North, East, 2)
  testCrossBar(West,  North, 3)
  testCrossBar(South, West,  4)
  testCrossBar(Local, North, 5)
}
