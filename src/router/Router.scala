package router

import Chisel._

object Direction {

  // TODO: Direction have to be a bus of of 5 Bools instead of a number
  val east :: north :: west :: south :: local :: Nil = Enum(UInt(), 5)
}
