package router

import Chisel._

class CrossBar extends Module {
  val io = new Bundle {
    val inData  = Vec.fill(5) { UInt(INPUT, width = 32) }
    val fromDir = UInt(INPUT, width = 5)
    val toDir   = UInt(INPUT, width = 5)
    val outData = Vec.fill(5) { UInt(OUTPUT, width = 32) }
  }

  val inData = UInt(width = 32)
  when(io.fromDir === East.value) {
    inData := io.inData(East.index)
  }.elsewhen(io.fromDir === North.value) {
    inData := io.inData(North.index)
  }.elsewhen(io.fromDir === West.value) {
    inData := io.inData(West.index)
  }.elsewhen(io.fromDir === South.value) {
    inData := io.inData(South.index)
  }.elsewhen(io.fromDir === Local.value) {
    inData := io.inData(Local.index)
  }.otherwise {
    inData := UInt(0)
  }

  // Default all outputs to 0
  for (i <- 0 until 5) {
    io.outData(i) := UInt(0)
  }

  switch(io.toDir) {
    is(East.value)  { io.outData(East.index)  := inData }
    is(North.value) { io.outData(North.index) := inData }
    is(West.value)  { io.outData(West.index)  := inData }
    is(South.value) { io.outData(South.index) := inData }
    is(Local.value) { io.outData(Local.index) := inData }
  }
}

class CrossBarTest(c: CrossBar) extends Tester(c) {
  poke(c.io.inData(East.index), 1)
  poke(c.io.inData(North.index), 2)
  poke(c.io.inData(West.index), 3)
  poke(c.io.inData(South.index), 4)
  poke(c.io.inData(Local.index), 5)
  step(1)

  def testCrossBar(from: TileDir, to: TileDir, value: Int) = {
    poke(c.io.fromDir, from.value.litValue())
    poke(c.io.toDir,   to.value.litValue())
    expect(c.io.outData(to.index), value)
    expect(c.io.outData(from.index), 0)
  }

  testCrossBar(East, South, 1)
  testCrossBar(North, East,  2)
  testCrossBar(West,  North, 3)
  testCrossBar(South, West,  4)
  testCrossBar(Local, North, 5)
}
