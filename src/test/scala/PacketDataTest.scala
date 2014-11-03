package test

import Chisel._
import router._

class PacketTestModule extends Module {
  val io = new Bundle() {
    val packet = new Packet().asInput
    val ySender = UInt(OUTPUT, width = 4)
    val xSender = UInt(OUTPUT, width = 4)
    val yDest = UInt(OUTPUT, width = 4)
    val xDest = UInt(OUTPUT, width = 4)
    val address = UInt(OUTPUT, width = 32)
  }

  val data = io.packet.toBits().toUInt()

  io.yDest := data(PacketConsts.Y_DEST_END, PacketConsts.Y_DEST_BEGIN)
  io.xDest := data(PacketConsts.X_DEST_END, PacketConsts.X_DEST_BEGIN)
  io.ySender := data(PacketConsts.Y_SEND_END, PacketConsts.Y_SEND_BEGIN)
  io.xSender := data(PacketConsts.X_SEND_END, PacketConsts.X_SEND_BEGIN)
}

class PacketTestModuleTest(m: PacketTestModule) extends Tester(m) {
  def testGetXYDest() {
    poke(m.io.packet.dest.y, 15)
    poke(m.io.packet.dest.x, 8)
    step(1)
    expect(m.io.yDest, 15)
    expect(m.io.xDest, 8)
  }

  def testGetXYSend() {
    poke(m.io.packet.sender.y, 7)
    poke(m.io.packet.sender.x, 3)
    step(1)
    expect(m.io.ySender, 7)
    expect(m.io.xSender, 3)
  }

  testGetXYDest()
  testGetXYSend()
}
