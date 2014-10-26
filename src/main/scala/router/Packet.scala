package router

import Chisel._

object PacketData {
  val LENGTH              = 196

  val Y_DEST_END          = 195
  val Y_DEST_BEGIN        = 192
  val X_DEST_END          = 191
  val X_DEST_BEGIN        = 188
  val Y_SEND_END          = 187
  val Y_SEND_BEGIN        = 184
  val X_SEND_END          = 183
  val X_SEND_BEGIN        = 180
  val DATA_END            = 179
  val DATA_BEGIN          = 52
  val ERROR_INDEX         = 51
  val EXOP_INDEX          = 50
  val WRITE_MASK_END      = 49
  val WRITE_MASK_BEGIN    = 34
  val WRITE_REQUEST_INDEX = 33
  val REPLY_INDEX         = 32
  val ADDRESS_END         = 31
  val ADDRESS_BEGIN       = 0

  def apply(x: Int): PacketData = Lit(x) {
    PacketData()
  }

  def apply(x: BigInt): PacketData = Lit(x) {
    PacketData()
  }

  def apply(x: UInt): PacketData = Lit(x.litValue()) {
    PacketData()
  }

  def apply(dir: IODirection = null): PacketData = {
    val res = new PacketData()
    res.create(dir, width = PacketData.LENGTH)
    res
  }

  def create(
    yDest: Int = 0,
    xDest: Int = 0,
    ySender: Int = 0,
    xSender: Int = 0,
    payload: Int = 0,
    error: Boolean = false,
    exop: Boolean = false,
    writeMask: Int = 0,
    writeReq: Boolean = false,
    reply: Boolean = false,
    address: Int = 0
  ): PacketData = {
    val yd = UInt(yDest, width = 4)
    val xd = UInt(xDest, width = 4)
    val ys = UInt(ySender, width = 4)
    val xs = UInt(xSender, width = 4)
    val _payload = UInt(payload, width = 128)
    val _error = Bool(error)
    val _exop = Bool(exop)
    val _writeMask = UInt(writeMask, width = 16)
    val _writeReq = Bool(writeReq)
    val _reply = Bool(reply)
    val _address = UInt(address, width = 32)

    val data = Cat(yd, xd, ys, xs, _payload, _error, _exop, _writeMask, _writeReq, _reply, _address)
    assert(data.getWidth() == PacketData.LENGTH)
    PacketData(data)
  }
}

class PacketData extends Bits {
  type T = PacketData

  def yDest: UInt = this(PacketData.Y_DEST_END, PacketData.Y_DEST_BEGIN)
  def xDest: UInt = this(PacketData.X_DEST_END, PacketData.X_DEST_BEGIN)
  def ySender: UInt = this(PacketData.Y_SEND_END, PacketData.Y_SEND_BEGIN)
  def xSender: UInt = this(PacketData.X_SEND_END, PacketData.X_SEND_BEGIN)
  def payload: UInt = this(PacketData.DATA_END, PacketData.DATA_BEGIN)
  def isError: Bool = this(PacketData.ERROR_INDEX)
  def isExop: Bool = this(PacketData.EXOP_INDEX)
  def writeMask: UInt = this(PacketData.WRITE_MASK_END, PacketData.WRITE_MASK_BEGIN)
  def isWriteReq: Bool = this(PacketData.WRITE_REQUEST_INDEX)
  def isReply: Bool = this(PacketData.REPLY_INDEX).toBool()
  def address: UInt = this(PacketData.ADDRESS_END, PacketData.ADDRESS_BEGIN)

  override def fromInt(x: Int): this.type = {
    PacketData(x).asInstanceOf[this.type]
  }
}

class PacketDataModule extends Module {
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

class PacketDataModuleTest(m: PacketDataModule) extends Tester(m) {
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
