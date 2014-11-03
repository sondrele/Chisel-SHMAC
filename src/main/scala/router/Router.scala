package router

import Chisel._

// in.valid  -> Request to write into router
// in.bits   -> Data to write
// in.ready  <- True if input port is not full
// out.valid <- Router requesting to send data
// out.bits  <- Data to send
// out.ready -> True to request output to send data
class RouterPortIO extends Bundle {
  val in = Decoupled(new Packet()).flip()
  val out = Decoupled(new Packet())
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
    io.ports(i) <> routers(i).port
    routers(i).inRead := isGrantedByArbiters(numPorts - 1, i)
    routers(i).outWrite := arbiters(i).grantedReady

    crossbar.port(i).inData.assign(routers(i).crossbarIn)
    routers(i).crossbarOut.assign(crossbar.port(i).outData)
    crossbar.port(i).select := arbiters(i).granted
  }

  for (i <- 0 until numPorts) {
    for (j <- 0 until numPorts) {
      arbiters(i).isEmpty(j) := routers(j).isEmpty
      arbiters(i).requesting(j) := routers(j).direction(i)
    }
    arbiters(i).isFull := routers(i).isFull
  }
}
