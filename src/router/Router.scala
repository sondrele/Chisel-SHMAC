package router

import Chisel._

class Router extends Module {
  val numPorts = 2
  val io = new Bundle {
    val inRequests = UInt(INPUT, width = numPorts)
    val inData = UInt(INPUT, width = numPorts * PacketData.LENGTH)
    val outRequests = UInt(OUTPUT, width = numPorts)
    val outData = UInt(OUTPUT, width = numPorts * PacketData.LENGTH)
  }
}

class RouterTest(r: Router) extends Router {

}