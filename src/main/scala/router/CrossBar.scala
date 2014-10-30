package router

import Chisel._

class CrossBarIO(numPorts: Int) extends Bundle {
  val inData = Vec.fill(numPorts) { new PacketBundle().asInput }
  val select = Vec.fill(numPorts) { UInt(INPUT, width = numPorts) }
  val outData = Vec.fill(numPorts) { new PacketBundle().asOutput }
}

class CrossBar(numPorts: Int) extends Module {
  val io = new CrossBarIO(numPorts)

  for (i <- 0 until numPorts) {
    io.outData(i).init(UInt(0))

    when (io.select(i) != UInt(0)) {
      for (j <- 0 until numPorts) {
        when(io.select(i)(j)) {
          io.outData(i).assign(io.inData(j))
        }
      }
    }
  }
}
