package main.scala.tiles

import Chisel._

import main.scala.router.{Router, RouterIO}

case class TileLoc(x: Int, y: Int)

case class EmptyTileConfig(tile: TileLoc, fifoSize: Int = 4)

class TileIO(numPorts: Int) extends RouterIO(numPorts)

class EmptyTile(implicit conf: EmptyTileConfig) extends Module {
  val numPorts = 4
  val io: TileIO = new TileIO(numPorts)

  val router = Module(new Router(conf.tile, numPorts, conf.fifoSize)).io
  for (i <- 0 until numPorts) {
    io.ports(i) <> router.ports(i)
  }

}
