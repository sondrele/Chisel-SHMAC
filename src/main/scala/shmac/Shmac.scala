package main.scala.shmac

import Chisel.{Bundle, Module}
import Common.HTIFIO
import main.scala.router.{West, East}
import main.scala.tiles._

case class ShmacConfig(tileConfigs: Array[TileConfig], connections: Array[((Int, Int), (Int, Int))])

class Shmac extends Module {
  val io = new Bundle {
    val host = new HTIFIO()
  }

  implicit val sodorConf = SodorTileConfig(TileLoc(0, 0), imem = TileLoc(1, 0), dmem = TileLoc(1, 0))
  val proc = Module(new SodorTile())
  proc.io.host <> io.host

  implicit val ramConf = RamTileConfig(TileLoc(1, 0))
  val ram = Module(new RamTile())

  // +----+----+
  // |Proc|Ram |
  // +----+----+
  proc.io.ports(East.index).in <> ram.io.ports(West.index).out
  proc.io.ports(East.index).out <> ram.io.ports(West.index).in

}

class ConfigurableShmac(implicit conf: ShmacConfig) extends Module {
  val io = new Bundle {
    val host = new HTIFIO()
  }

  var tiles: Array[Tile] = Array()

  for (i <- 0 until conf.tileConfigs.length) {
    conf.tileConfigs(i) match {
      case config if config.isInstanceOf[SodorTileConfig] =>
        implicit val sodorConf = config.asInstanceOf[SodorTileConfig]
        val proc = Module(new SodorTile())
        proc.io.host <> io.host
        tiles ++= Array(proc)

      case config if config.isInstanceOf[RamTileConfig] =>
        implicit val ramConf = config.asInstanceOf[RamTileConfig]
        val ram = Module(new RamTile())
        tiles ++= Array(ram)
    }
  }

  conf.connections.foreach {
    case ((tileA, portA), (tileB, portB)) =>
      tiles(tileA).io.ports(portA).in  <> tiles(tileB).io.ports(portB).out
      tiles(tileA).io.ports(portA).out <> tiles(tileB).io.ports(portB).in
  }

}
