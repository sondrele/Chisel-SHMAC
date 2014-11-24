package main.scala.tiles

import Chisel._

import main.scala.router.{Router, RouterIO}

case class TileLoc(x: Int, y: Int)

class TileIO(numPorts: Int) extends RouterIO(numPorts)

class EmptyTile(tile: TileLoc, fifoSize: Int) extends Module {
  val numPorts = 4
  val io: TileIO = new TileIO(numPorts)

  val router = Module(new Router(tile.x, tile.y, numPorts, fifoSize)).io
  for (i <- 0 until numPorts) {
    io.ports(i) <> router.ports(i)
  }

}
