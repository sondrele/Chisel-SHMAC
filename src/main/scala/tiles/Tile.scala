package main.scala.tiles

import Chisel._

import main.scala.router.{Router, RouterIO}

case class TileLoc(x: Int, y: Int)

trait TileConfig {
  val tile: TileLoc
  val fifoSize: Int = 4
  val ioPorts: Int = 4
}

class TileIO(numPorts: Int) extends RouterIO(numPorts)

abstract class Tile(implicit conf: TileConfig) extends Module

case class EmptyTileConfig(tile: TileLoc) extends TileConfig

class EmptyTile(implicit conf: EmptyTileConfig) extends Tile {
  val io: TileIO = new TileIO(conf.ioPorts)

  val router = Module(new Router(conf.tile, conf.ioPorts, conf.fifoSize)).io
  for (i <- 0 until conf.ioPorts) {
    io.ports(i) <> router.ports(i)
  }

}
