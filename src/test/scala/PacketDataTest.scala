package test

import Chisel._
import router._

class PacketDataTestModule extends Module {
  val io = new Bundle() {
    val packet = PacketData(INPUT)
    val address = UInt(OUTPUT, width = 32)
    val xDest = UInt(OUTPUT, width = 4)
    val yDest = UInt(OUTPUT, width = 4)
    val xSender = UInt(OUTPUT, width = 4)
    val ySender = UInt(OUTPUT, width = 4)
  }

  when (io.packet != UInt(0)) {
    io.address := io.packet.address
    io.xDest := io.packet.xDest
    io.yDest := io.packet.yDest
    io.xSender := io.packet.xSender
    io.ySender := io.packet.ySender
  }.otherwise {
    io.address := UInt(0)
    io.xDest := UInt(0)
    io.yDest := UInt(0)
    io.xSender := UInt(0)
    io.ySender := UInt(0)
  }
}

class PacketDataTestModuleTest(m: PacketDataTestModule) extends Tester(m) {
  def testBasicGet() {
    expect(m.io.address, 0)
    poke(m.io.packet, 1)
    step(1)
    expect(m.io.address, 1)
  }

  def testGetXYDest() {
    val p = PacketData.create(yDest = 9, xDest = 6)
    poke(m.io.packet, p.litValue())
    step(1)
    expect(m.io.yDest, 9)
    expect(m.io.xDest, 6)
  }

  def testGetXYSend() {
    val p = PacketData.create(ySender = 6, xSender = 9)
    poke(m.io.packet, p.litValue())
    step(1)
    expect(m.io.ySender, 6)
    expect(m.io.xSender, 9)
  }

  testBasicGet()
  testGetXYDest()
  testGetXYSend()
}
