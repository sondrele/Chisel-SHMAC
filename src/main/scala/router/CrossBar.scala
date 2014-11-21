package main.scala.router

import Chisel._

class CrossBarPortIO(numPorts: Int) extends Bundle {
  val inData = new Packet().asInput
  val select = UInt(INPUT, width = numPorts)
  val outData = new Packet().asOutput
}

class CrossBarIO(numPorts: Int) extends Bundle {
  val port = Vec.fill(numPorts) { new CrossBarPortIO(numPorts) }
}

class CrossBar(numPorts: Int) extends Module {
  val io = new CrossBarIO(numPorts)

  for (i <- 0 until numPorts) {
    io.port(i).outData.init(UInt(0))

    when (io.port(i).select != UInt(0)) {
      for (j <- 0 until numPorts) {
        when(io.port(i).select(j)) {
          io.port(i).outData.assign(io.port(j).inData)
        }
      }
    }
  }
}
