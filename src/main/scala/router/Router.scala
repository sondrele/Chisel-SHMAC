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
    routers(i).port.in.valid := io.ports(i).in.valid
    routers(i).port.in.bits.assign(io.ports(i).in.bits)
    routers(i).inRead := isGrantedByArbiters(numPorts - 1, i)
    crossbar.inData(i).assign(routers(i).crossbarIn)
    io.ports(i).in.ready := routers(i).port.in.ready
    io.ports(i).out.valid := routers(i).port.out.valid
    io.ports(i).out.bits.assign(routers(i).port.out.bits)
    routers(i).outWrite := arbiters(i).grantedReady
    routers(i).crossbarOut.assign(crossbar.outData(i))
    routers(i).port.out.ready := io.ports(i).out.ready
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
