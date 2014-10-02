package router

import Chisel._
import utils.{FifoTest, Fifo}

object Main {

  def main(args: Array[String]): Unit = {
    chiselMainTest(args, () => Module(new RoutingXY())) {
      r => new RoutingXYTest(r)
    }
  }

}
