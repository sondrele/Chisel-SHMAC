import Chisel._
import Sodor.ShmacUnit
import router._
import tiles._
import test._

object TestMain {

  def main(args: Array[String]): Unit = {
    val testArgs = args.slice(1, args.length)
    val modules = Array(
      "RouteComputation",
      "InputPort",
      "OutputPort",
      "CrossBar",
      "DirectionArbiter",
      "Packet",
      "DirectionRouter",
      "Router",
      "Ram",
      "RamTile",
      "ShmacUnit"
    )

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
    case "InputPort" => chiselMainTest(args, () => Module(new InputPort(4))) {
      p => new InputPortTest(p)
    }
    case "OutputPort" => chiselMainTest(args, () => Module(new OutputPort(2))) {
      p => new OutputPortTest(p)
    }
    case "CrossBar" => chiselMainTest(args, () => Module(new CrossBar(5))) {
      b => new CrossBarTest(b)
    }
    case "DirectionArbiter" => chiselMainTest(args, () => Module(new DirectionArbiter(5))) {
      b => new DirectionArbiterTest(b)
    }
    case "Packet" => chiselMainTest(args, () => Module(new PacketTestModule())) {
      b => new PacketTestModuleTest(b)
    }
    case "DirectionRouter" => chiselMainTest(args, () => Module(new DirectionRouter(UInt(1), UInt(1), 4))) {
      b => new DirectionRouterTest(b)
    }
    case "Router" => chiselMainTest(args, () => Module(new Router(1, 1, 5, 4))) {
      b => new RouterTest(b)
    }
    case "Ram" => chiselMainTest(args, () => Module(new Ram(depth = 8, dataWidth = 32))) {
      r => new RamTest(r)
    }
   case "RamTile" => chiselMainTest(args, () => Module(new RamTile(1, 1, 4, 4, memDepth = 4096))) {
     t => {
       new RamTileTest(t)
       new RamTileSumTest(t)
     }
   }
   case "ShmacUnit" =>
     chiselMainTest(args, () => Module(new ShmacUnit())) {
       t => new ShmacUnitTest(t)
     }
     chiselMainTest(args, () => Module(new ShmacUnit())) {
       t => new ShmacLoadStoreTest(t)
     }
     chiselMainTest(args, () => Module(new ShmacUnit())) {
       t => new ShmacStoreImmediateTest(t)
     }
    case other => sys.error(s"No module with name $other")
  }

}
