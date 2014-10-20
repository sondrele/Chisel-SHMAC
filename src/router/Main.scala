package router

import Chisel._
import utils._

object Main {

  def main(args: Array[String]): Unit = {
    val testArgs = args.slice(1, args.length)
    val modules = Array("RouteComputation", "Fifo", "InputPort", "OutputPort", "CrossBar", "RoutingArbiter")

    args(0) match {
      case "testall" => testModules(modules, testArgs)
      case other => testModule(other, testArgs)
    }
  }

  def testModules(modules: Array[String], args: Array[String]) = {
    modules.map(module => testModule(module, args))
  }

  def testModule(module: String, args: Array[String]) = module match {
    case "RouteComputation" => chiselMainTest(args, () => Module(new RouteComputation())) {
      r => new RouteComputationTest(r)
    }
    case "Fifo" => chiselMainTest(args, () => Module(new Fifo(PacketData(), 4))) {
      f => new FifoTest(f)
    }
    case "InputPort" => chiselMainTest(args, () => Module(new InputPort(4))) {
      p => new InputPortTest(p)
    }
    case "OutputPort" => chiselMainTest(args, () => Module(new OutputPort(2))) {
      p => new OutputPortTest(p)
    }
    case "CrossBar" => chiselMainTest(args, () => Module(new CrossBar())) {
      b => new CrossBarTest(b)
    }
    case "RoutingArbiter" => chiselMainTest(args, () => Module(new RoutingArbiter())) {
      b => new RoutingArbiterTest(b)
    }
    case "DirectionArbiter" => chiselMainTest(args, () => Module(new DirectionArbiter(5))) {
      b => new DirectionArbiterTest(b)
    }
    case "Packet" => chiselMainTest(args, () => Module(new PacketDataModule())) {
      b => new PacketDataModuleTest(b)
    }
    case "DirectionRouter" => chiselMainTest(args, () => Module(new DirectionRouter(UInt(1), UInt(1), 4))) {
      b => new DirectionRouterTest(b)
    }
    case "Router" => chiselMainTest(args, () => Module(new Router(1, 1))) {
      b => new RouterTest(b)
    }
    case other => sys.error(s"No module with name $other")
  }

}
