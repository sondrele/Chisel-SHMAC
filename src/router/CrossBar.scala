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
  when(io.fromDir === Direction.east) {
    inData := io.inData(Direction.east)
  }.elsewhen(io.fromDir === Direction.north) {
    inData := io.inData(Direction.north)
  }.elsewhen(io.fromDir === Direction.west) {
    inData := io.inData(Direction.west)
  }.elsewhen(io.fromDir === Direction.south) {
    inData := io.inData(Direction.south)
  }.otherwise { //elsewhen(io.fromDir === Direction.local) {
    inData := io.inData(Direction.local)
  }

  // Default all outputs to 0
  for (i <- 0 until 5) {
    io.outData(i) := UInt(0)
  }

  switch(io.toDir) {
    is(Direction.east)  { io.outData(Direction.east)  := inData }
    is(Direction.north) { io.outData(Direction.north) := inData }
    is(Direction.west)  { io.outData(Direction.west)  := inData }
    is(Direction.south) { io.outData(Direction.south) := inData }
    is(Direction.local) { io.outData(Direction.local) := inData }
  }
}

class CrossBarTest(c: CrossBar) extends Tester(c) {
  poke(c.io.inData(Direction.east.litValue().toInt),  1)
  poke(c.io.inData(Direction.north.litValue().toInt), 2)
  poke(c.io.inData(Direction.west.litValue().toInt),  3)
  poke(c.io.inData(Direction.south.litValue().toInt), 4)
  poke(c.io.inData(Direction.local.litValue().toInt), 5)
  step(1)

  def testCrossBar(from: UInt, to: UInt, value: Int) = {
    poke(c.io.fromDir, from.litValue())
    poke(c.io.toDir,   to.litValue())
    expect(c.io.outData(to.litValue().toInt), value)
    expect(c.io.outData(from.litValue().toInt), 0)
  }

  testCrossBar(Direction.east,  Direction.south, 1)
  testCrossBar(Direction.north, Direction.east,  2)
  testCrossBar(Direction.west,  Direction.north, 3)
  testCrossBar(Direction.south, Direction.west,  4)
  testCrossBar(Direction.local, Direction.north, 5)
}
