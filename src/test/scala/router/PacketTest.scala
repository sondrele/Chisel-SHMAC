package router

import Chisel._
import main.scala.router.Packet

class PacketTestModule extends Module {
  val io = new Bundle() {
    val packet = new Packet().asInput
    val ySender = UInt(OUTPUT, width = 4)
    val xSender = UInt(OUTPUT, width = 4)
    val yDest = UInt(OUTPUT, width = 4)
    val xDest = UInt(OUTPUT, width = 4)
    val payload = UInt(OUTPUT, width = Packet.DATA_WIDTH)
    val isError = Bool(OUTPUT)
    val isExop = Bool(OUTPUT)
    val writeMask = UInt(OUTPUT, width = 16)
    val isWriteReq = Bool(OUTPUT)
    val isReply = Bool(OUTPUT)
    val address = UInt(OUTPUT, width = Packet.ADDRESS_WIDTH)
  }

  val data = io.packet.toBits().toUInt()

  io.yDest      := data(Packet.Y_DEST_END, Packet.Y_DEST_BEGIN)
  io.xDest      := data(Packet.X_DEST_END, Packet.X_DEST_BEGIN)
  io.ySender    := data(Packet.Y_SEND_END, Packet.Y_SEND_BEGIN)
  io.xSender    := data(Packet.X_SEND_END, Packet.X_SEND_BEGIN)
  io.payload    := data(Packet.DATA_END, Packet.DATA_BEGIN)
  io.isError    := data(Packet.ERROR_INDEX).toBool()
  io.isExop     := data(Packet.EXOP_INDEX).toBool()
  io.writeMask  := data(Packet.WRITE_MASK_END, Packet.WRITE_MASK_BEGIN)
  io.isWriteReq := data(Packet.WRITE_REQUEST_INDEX).toBool()
  io.isReply    := data(Packet.REPLY_INDEX).toBool()
  io.address    := data(Packet.ADDRESS_END, Packet.ADDRESS_BEGIN)
}

class PacketTestModuleTest(m: PacketTestModule) extends Tester(m) {
  def testXYDest() {
    poke(m.io.packet.dest.y, 15)
    poke(m.io.packet.dest.x, 8)
    expect(m.io.yDest, 15)
    expect(m.io.xDest, 8)
  }

  def testXYSend() {
    poke(m.io.packet.sender.y, 7)
    poke(m.io.packet.sender.x, 3)
    expect(m.io.ySender, 7)
    expect(m.io.xSender, 3)
  }

  def testPayload() {
    expect(m.io.payload, 0)
    poke(m.io.packet.payload, (0x1<<31)-1)
    expect(m.io.payload, Int.MaxValue)
  }

  def testError() {
    expect(m.io.isError, 0)
    poke(m.io.packet.header.error, 1)
    expect(m.io.isError, 1)
  }

  def testExop() {
    expect(m.io.isExop, 0)
    poke(m.io.packet.header.exop, 1)
    expect(m.io.isExop, 1)
  }

  def testWriteMask() {
    expect(m.io.writeMask, 0)
    poke(m.io.packet.header.writeMask, 2048)
    expect(m.io.writeMask, 2048)
  }

  def testWriteReq() {
    expect(m.io.isWriteReq, 0)
    poke(m.io.packet.header.writeReq, 1)
    expect(m.io.isWriteReq, 1)
  }

  def testReply() {
    expect(m.io.isReply, 0)
    poke(m.io.packet.header.reply, 1)
    expect(m.io.isReply, 1)
  }

  def testAddress() {
    expect(m.io.address, 0)
    poke(m.io.packet.header.address, 398567)
    expect(m.io.address, 398567)
  }

  testXYDest()
  testXYSend()
  testPayload()
  testError()
  testExop()
  testWriteMask()
  testWriteReq()
  testReply()
  testAddress()
}
