package router

import Chisel._

class PacketDir extends Bundle {
  val y = UInt(width = 4)
  val x = UInt(width = 4)
}

class PacketHeader extends Bundle {
  val error = Bool()
  val exop = Bool()
  val writeMask = Bits(width = 16)
  val writeReq = Bool()
  val reply = Bool()
  val address = Bits(width = 32)
}

class PacketBundle extends Bundle {
  val dest = new PacketDir()
  val sender = new PacketDir()
  val payload = Bits(width = 128)
  val header = new PacketHeader()

  def assign(other: PacketBundle): Unit = {
    this.dest.y := other.dest.y
    this.dest.x := other.dest.x
    this.sender.y := other.sender.y
    this.sender.x := other.sender.x
    this.payload := other.payload
    this.header.error := other.header.error
    this.header.exop := other.header.exop
    this.header.writeMask := other.header.writeMask
    this.header.writeReq := other.header.writeReq
    this.header.reply := other.header.reply
    this.header.address := other.header.address
  }

  def init(value: UInt): Unit = {
    this.dest.y := value
    this.dest.x := value
    this.sender.y := value
    this.sender.x := value
    this.payload := value
    this.header.error := value
    this.header.exop := value
    this.header.writeMask := value
    this.header.writeReq := value
    this.header.reply := value
    this.header.address := value
  }
}


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

  def update(
    yDest: UInt = UInt(0, width = 4),
    xDest: UInt = UInt(0, width = 4),
    ySender: UInt = UInt(0, width = 4),
    xSender: UInt = UInt(0, width = 4),
    payload: UInt = UInt(0, width = 128),
    error: Bool = Bool(false),
    exop: Bool = Bool(false),
    writeMask: UInt = UInt(0, width = 16),
    writeReq: Bool = Bool(false),
    reply: Bool = Bool(false),
    address: UInt = UInt(0, width = 32)
  ): PacketData = {
    assert(yDest.getWidth() == 4)
    assert(xDest.getWidth() == 4)
    assert(ySender.getWidth() == 4)
    assert(xSender.getWidth() == 4)
    assert(payload.getWidth() == 128)
    assert(writeMask.getWidth() == 16)
    assert(address.getWidth() == 32)

    val data = Cat(yDest, xDest, ySender, xSender, payload, error, exop, writeMask, writeReq, reply, address)
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
