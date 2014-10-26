package router

import Chisel._

class RouterIO(numPorts: Int) extends Bundle {
  val inRequest = Vec.fill(numPorts) { Bool(INPUT) } // Request to write into router
  val inData = Vec.fill(numPorts) { PacketData(INPUT) } // Data to write
  val inReady = Vec.fill(numPorts) { Bool(OUTPUT) } // True if input port is not full
  val outRequest = Vec.fill(numPorts) { Bool(OUTPUT) } // Router requesting to send data
  val outData = Vec.fill(numPorts) { PacketData(OUTPUT) } // Data to send
  val outReady = Vec.fill(numPorts) { Bool(INPUT) } // True to request output to send data
}

class Router(x: Int, y: Int, numPorts: Int, numRecords: Int) extends Module {
  val tileX = UInt(x, width = 4)
  val tileY = UInt(y, width = 4)

  val io = new RouterIO(numPorts)

  val crossbar = Module(new CrossBar(numPorts)).io
  val arbiters = Vec.fill(numPorts) { Module(new DirectionArbiter(numPorts)).io }
  val routers = Vec.fill(numPorts) { Module(new DirectionRouter(tileX, tileY, numRecords)).io }

  def isGrantedByArbiters(j: Int, i: Int): Bool = {
    if (j == 0) {
      arbiters(j).granted(i)
    } else {
      arbiters(j).granted(i) | isGrantedByArbiters(j - 1, i)
    }
  }

  for (i <- 0 until numPorts) {
    routers(i).inRequest := io.inRequest(i)
    routers(i).inData := io.inData(i)
    routers(i).inRead := isGrantedByArbiters(numPorts - 1, i)
    crossbar.inData(i) := routers(i).crossbarIn
    io.inReady(i) := routers(i).inReady
    io.outRequest(i) := routers(i).outRequest
    io.outData(i) := routers(i).outData
    routers(i).outWrite := arbiters(i).grantedReady
    routers(i).crossbarOut := crossbar.outData(i)
    routers(i).outReady := io.outReady(i)
    crossbar.select(i) := arbiters(i).granted
  }

  for (i <- 0 until numPorts) {
    for (j <- 0 until numPorts) {
      arbiters(i).isEmpty(j) := routers(j).isEmpty
      arbiters(i).requesting(j) := routers(j).direction(i)
    }
    arbiters(i).isFull := routers(i).isFull
  }
}
