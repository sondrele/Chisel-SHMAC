package router

import Chisel._

class RouterPortIO extends Bundle {
  val inRequest  = Bool(INPUT)        // Request to write into router
  val inData     = new Packet().asInput  // Data to write
  val inReady    = Bool(OUTPUT)       // True if input port is not full
  val outRequest = Bool(OUTPUT)       // Router requesting to send data
  val outData    = new Packet().asOutput // Data to send
  val outReady   = Bool(INPUT)        // True to request output to send data
}

class RouterIO(numPorts: Int) extends Bundle {
  val ports = Vec.fill(numPorts) { new RouterPortIO() }
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
    routers(i).inRequest := io.ports(i).inRequest
    routers(i).inData.assign(io.ports(i).inData)
    routers(i).inRead := isGrantedByArbiters(numPorts - 1, i)
    crossbar.inData(i).assign(routers(i).crossbarIn)
    io.ports(i).inReady := routers(i).inReady
    io.ports(i).outRequest := routers(i).outRequest
    io.ports(i).outData.assign(routers(i).outData)
    routers(i).outWrite := arbiters(i).grantedReady
    routers(i).crossbarOut.assign(crossbar.outData(i))
    routers(i).outReady := io.ports(i).outReady
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
