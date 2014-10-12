package router

import Chisel._

class PacketHeader extends Bundle {
  val address      = UInt(width = 32)
  val reply        = Bool()
  val writeRequest = Bool()
  val writeMask    = UInt(width = 16)
  val exop         = UInt(width = 2)
  val error        = Bool()
}

class TileLocation extends Bundle {
  val x = UInt(width = 4)
  val y = UInt(width = 4)
}

class Packet extends Bundle {
  val header      = new PacketHeader() // 53 bits
  val data        = Vec.fill(16) { UInt(width = 8) } // 128 bits
  val sender      = new TileLocation() // 8 bits
  val destination = new TileLocation() // 8 bits
}

object Packet {
  val length = 197
}
