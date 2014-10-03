package router

import Chisel._
import utils._

object Main {

  def main(args: Array[String]): Unit = {
    chiselMainTest(args, () => Module(new RoutingXY())) {
      r => new RoutingXYTest(r)
    }

    chiselMainTest(args, () => Module(new Fifo(4))) {
      f => new FifoTest(f)
    }

    chiselMainTest(args, () => Module(new InputPort(4))) {
      p => new InputPortTest(p)
    }

  }

}
