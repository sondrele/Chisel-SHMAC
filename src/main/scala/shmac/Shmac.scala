package main.scala.shmac

import Chisel.{Bundle, Module}
import Common.HTIFIO
import main.scala.router.{West, East}
import main.scala.tiles.{SodorTileConf, SodorTile, RamTile}

class Shmac extends Module {
  val io = new Bundle {
    val host = new HTIFIO()
  }

  implicit val sodorConf = SodorTileConf(imem = (1, 0), dmem = (1, 0))
  val proc = Module(new SodorTile(0, 0, 4, 4))
  proc.io.host <> io.host

  val ram = Module(new RamTile(1, 0, 4, 4, 256))

  // +----+----+
  // |Proc|Ram |
  // +----+----+
  proc.io.ports(East.index).in <> ram.io.ports(West.index).out
  proc.io.ports(East.index).out <> ram.io.ports(West.index).in

}
