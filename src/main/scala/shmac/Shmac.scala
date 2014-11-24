package main.scala.shmac

import Chisel.{Bundle, Module}
import Common.HTIFIO
import main.scala.router.{West, East}
import main.scala.tiles._

class Shmac extends Module {
  val io = new Bundle {
    val host = new HTIFIO()
  }

  implicit val sodorConf = SodorTileConfig(imem = TileLoc(1, 0), dmem = TileLoc(1, 0))
  val proc = Module(new SodorTile(TileLoc(0, 0)))
  proc.io.host <> io.host

  implicit val ramConf = RamTileConfig()
  val ram = Module(new RamTile(TileLoc(1, 0)))

  // +----+----+
  // |Proc|Ram |
  // +----+----+
  proc.io.ports(East.index).in <> ram.io.ports(West.index).out
  proc.io.ports(East.index).out <> ram.io.ports(West.index).in

}
