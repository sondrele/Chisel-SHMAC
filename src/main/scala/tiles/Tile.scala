package main.scala.tiles

import Chisel._

import main.scala.router.{Router, RouterIO}

case class TileLoc(x: Int, y: Int)

class TileIO(numPorts: Int) extends RouterIO(numPorts)

class EmptyTile(location: TileLoc, numRecords: Int) extends Module {
  val numPorts = 4
  val io: TileIO = new TileIO(numPorts)

  val router = Module(new Router(location.x, location.y, numPorts, numRecords)).io
  for (i <- 0 until numPorts) {
    io.ports(i) <> router.ports(i)
  }

}
