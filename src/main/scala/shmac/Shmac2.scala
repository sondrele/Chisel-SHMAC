package main.scala.shmac

import Chisel.{Bundle, Module}
import Common.HTIFIO
import main.scala.router.{North, South, West, East}
import main.scala.tiles._

class Shmac2 extends Module {
  val io = new Bundle {
    val host = new HTIFIO()
  }

  implicit var sodorConf = SodorTileConfig(TileLoc(0, 0), imem = TileLoc(1, 0), dmem = TileLoc(1, 1))
  val proc00 = Module(new SodorTile())
  proc00.io.host.reset := io.host.reset

  sodorConf = SodorTileConfig(TileLoc(0, 1), imem = TileLoc(1, 0), dmem = TileLoc(1, 1))
  val proc01 = Module(new SodorTile())
  proc01.io.host.reset := io.host.reset

  implicit var ramConf = RamTileConfig(TileLoc(1, 0))
  val ram10 = Module(new RamTile())

  ramConf = RamTileConfig(TileLoc(1, 1))
  val ram11 = Module(new RamTile())

  // +------+------+
  // |Proc00|Ram10 |
  // +------+------+
  // |Proc01|Ram11 |
  // +------+------+

  proc00.io.ports(East.index).in  <> ram10.io.ports(West.index).out
  proc00.io.ports(East.index).out <> ram10.io.ports(West.index).in

  proc00.io.ports(South.index).in  <> proc01.io.ports(North.index).out
  proc00.io.ports(South.index).out <> proc01.io.ports(North.index).in

  ram10.io.ports(South.index).in  <> ram11.io.ports(North.index).out
  ram10.io.ports(South.index).out <> ram11.io.ports(North.index).in

  proc01.io.ports(East.index).in  <> ram11.io.ports(West.index).out
  proc01.io.ports(East.index).out <> ram11.io.ports(West.index).in

}
