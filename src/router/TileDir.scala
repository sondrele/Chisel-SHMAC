package router

import Chisel._

abstract class TileDir {
  val index: Int
  val value: UInt
}

object East extends TileDir {
  override val index = 0
  override val value = UInt("b00001", width = 5)
}

object North extends TileDir {
  override val index = 1
  override val value = UInt("b00010", width = 5)
}

object West extends TileDir {
  override val index = 2
  override val value = UInt("b00100", width = 5)
}

object South extends TileDir {
  override val index = 3
  override val value = UInt("b01000", width = 5)
}

object Local extends TileDir {
  override val index = 4
  override val value = UInt("b10000", width = 5)
}
