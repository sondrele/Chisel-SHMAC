package main.scala.router

import Chisel._

class PacketDir extends Bundle {
  val y = UInt(width = Packet.PACKET_DIR_WIDTH)
  val x = UInt(width = Packet.PACKET_DIR_WIDTH)
}

class PacketHeader extends Bundle {
  val error = Bool()
  val exop = Bool()
  val writeMask = Bits(width = Packet.WRITE_MASK_WIDTH)
  val writeReq = Bool()
  val reply = Bool()
  val address = Bits(width = Packet.ADDRESS_WIDTH)
}

class Packet extends Bundle {
  val dest = new PacketDir()
  val sender = new PacketDir()
  val payload = Bits(width = Packet.DATA_WIDTH)
  val header = new PacketHeader()

  def assign(other: Packet): Unit = {
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

object Packet {
  val ADDRESS_WIDTH    = 32
  val WRITE_MASK_WIDTH = 4
  val DATA_WIDTH       = 32
  val PACKET_DIR_WIDTH = 4

  val ADDRESS_BEGIN       = 0
  val ADDRESS_END         = ADDRESS_BEGIN       + ADDRESS_WIDTH - 1
  val REPLY_INDEX         = ADDRESS_END         + 1
  val WRITE_REQUEST_INDEX = REPLY_INDEX         + 1
  val WRITE_MASK_BEGIN    = WRITE_REQUEST_INDEX + 1
  val WRITE_MASK_END      = WRITE_MASK_BEGIN    + WRITE_MASK_WIDTH - 1
  val EXOP_INDEX          = WRITE_MASK_END      + 1
  val ERROR_INDEX         = EXOP_INDEX          + 1
  val DATA_BEGIN          = ERROR_INDEX         + 1
  val DATA_END            = DATA_BEGIN          + DATA_WIDTH - 1
  val X_SEND_BEGIN        = DATA_END            + 1
  val X_SEND_END          = X_SEND_BEGIN        + PACKET_DIR_WIDTH - 1
  val Y_SEND_BEGIN        = X_SEND_END          + 1
  val Y_SEND_END          = Y_SEND_BEGIN        + PACKET_DIR_WIDTH - 1
  val X_DEST_BEGIN        = Y_SEND_END          + 1
  val X_DEST_END          = X_DEST_BEGIN        + PACKET_DIR_WIDTH - 1
  val Y_DEST_BEGIN        = X_DEST_END          + 1
  val Y_DEST_END          = Y_DEST_BEGIN        + PACKET_DIR_WIDTH - 1

  val LENGTH = Y_DEST_END + 1
}
