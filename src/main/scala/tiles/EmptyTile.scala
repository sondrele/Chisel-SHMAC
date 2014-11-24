package main.scala.tiles

import Chisel._

import main.scala.router.{Router, RouterIO}

class TileIO(numPorts: Int) extends RouterIO(numPorts)

class EmptyTile(x: Int, y: Int, numPorts: Int, numRecords: Int) extends Module {
  val io: TileIO = new TileIO(numPorts)

  val router = Module(new Router(x, y, numPorts + 1, numRecords)).io
  for (i <- 0 until numPorts) {
    io.ports(i) <> router.ports(i)
  }

}
