package router

import Chisel._

object Direction {
  val east :: north :: west :: south :: local :: Nil = Enum(UInt(), 5)
}
