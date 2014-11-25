import Chisel._
import Sodor.SodorUnit
import main.scala.memory.Ram
import main.scala.router._
import main.scala.shmac.{Shmac2, Shmac}
import main.scala.tiles._
import memory.RamTest
import router._
import shmac._
import sodor._
import tiles._

object TestMain {

  def main(args: Array[String]): Unit = {
    val testArgs = args.slice(1, args.length)

    val routerTests = Array(
      "RouteComputation",
      "InputPort",
      "OutputPort",
      "CrossBar",
      "DirectionArbiter",
      "Packet",
      "DirectionRouter",
      "Router"
    )

    val unitTests = Array(
      "Ram",
      "SodorStoreImm",
      "SodorAddImm",
      "SodorLoadStore",
      "SodorLoadAddStore"
    )

    val tileTests = Array(
      "RamTile",
      "RamTileSum",
      "SodorTile",
      "SodorTileLoadStore",
      "SodorTileTileLoc"
    )

    val shmacTests = Array(
      "ShmacStoreImm",
      "ShmacLoadAddStore",
      "ShmacBranchTaken",
      "ShmacBranchNotTaken",
      "ShmacBranchTileLoc",
      "Shmac2"
    )

    val allTests = routerTests ++ unitTests ++ tileTests ++ shmacTests

    args(0) match {
      case "router" => testModules(routerTests, testArgs)
      case "units" => testModules(unitTests, testArgs)
      case "tiles" => testModules(tileTests, testArgs)
      case "shmac" => testModules(shmacTests, testArgs)
      case "all" => testModules(allTests, testArgs)
      case other if allTests.contains(other) => testModule(other, testArgs)
      case none => sys.error(s"No module with name $none")
    }
  }

  def testModules(modules: Array[String], args: Array[String]): Unit = {
    modules.map(module => testModule(module, args))
  }

  def testModule(module: String, args: Array[String]): Unit = {

    implicit val ramTileConf = RamTileConfig(TileLoc(1, 1))
    implicit val sodorTileConf = SodorTileConfig(TileLoc(1, 1), TileLoc(2, 1), TileLoc(0, 1))

    module match {
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
      case "Router" => chiselMainTest(args, () => Module(new Router(TileLoc(1, 1), 5, 4))) {
        b => new RouterTest(b)
      }
      case "Ram" => chiselMainTest(args, () => Module(new Ram(depth = 8, dataWidth = 32))) {
        r => new RamTest(r)
      }
      case "SodorStoreImm" => chiselMainTest(args, () => Module(new SodorUnit())) {
        t => new SodorStoreImmTest(t)
      }
      case "SodorAddImm" => chiselMainTest(args, () => Module(new SodorUnit())) {
        t => new SodorAddImmTest(t)
      }
      case "SodorLoadStore" => chiselMainTest(args, () => Module(new SodorUnit())) {
        t => new SodorLoadStoreTest(t)
      }
      case "SodorLoadAddStore" => chiselMainTest(args, () => Module(new SodorUnit())) {
        t => new SodorLoadAddStoreTest(t)
      }
      case "RamTile" => chiselMainTest(args, () => Module(new RamTile())) {
        t => new RamTileTest(t)
      }
      case "RamTileSum" => chiselMainTest(args, () => Module(new RamTile())) {
        t => new RamTileSumTest(t)
      }
      case "SodorTile" => chiselMainTest(args, () => Module(new SodorTile())) {
        t => new SodorTileTest(t)
      }
      case "SodorTileLoadStore" => chiselMainTest(args, () => Module(new SodorTile())) {
        t => new SodorTileLoadStoreTest(t)
      }
      case "SodorTileTileLoc" => chiselMainTest(args, () => Module(new SodorTile())) {
        t => new SodorTileTileLocTest(t)
      }
      case "ShmacStoreImm" => chiselMainTest(args, () => Module(new Shmac())) {
        s => new ShmacStoreImmTest(s)
      }
      case "ShmacLoadAddStore" => chiselMainTest(args, () => Module(new Shmac())) {
        s => new ShmacLoadAddStoreTest(s)
      }
      case "ShmacBranchTaken" => chiselMainTest(args, () => Module(new Shmac())) {
        s => new ShmacBranchTakenTest(s)
      }
      case "ShmacBranchNotTaken" => chiselMainTest(args, () => Module(new Shmac())) {
        s => new ShmacBranchNotTakenTest(s)
      }
      case "ShmacBranchTileLoc" => chiselMainTest(args, () => Module(new Shmac())) {
        s => new ShmacBranchTileLocTest(s)
      }
      case "Shmac2" => chiselMainTest(args, () => Module(new Shmac2())) {
        s => new Shmac2Test(s)
      }
      case other => sys.error(s"No module with name $other")
    }
  }
}
