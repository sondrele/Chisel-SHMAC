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
  val x = UInt(4)
  val y = UInt(4)
}

class Packet extends Bundle {
  val header      = new PacketHeader()
  val data        = Vec.fill(16) { UInt(width = 8) }
  val sender      = new TileLocation()
  val destination = new TileLocation()
}

object Direction {

  // TODO: Direction have to be a bus of of 5 Bools instead of a number
  val east :: north :: west :: south :: local :: Nil = Enum(UInt(), 5)
}
