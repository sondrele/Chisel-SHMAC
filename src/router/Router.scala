package router

import Chisel._

class Router extends Module {
  val numPorts = 2
  val io = new Bundle {
    val inRequests = UInt(INPUT, width = numPorts)
    val inData = UInt(INPUT, width = numPorts * Packet.length)
    val outRequests = UInt(OUTPUT, width = numPorts)
    val outData = UInt(OUTPUT, width = numPorts * Packet.length)
  }
}

class RouterTest(r: Router) extends Router {

}