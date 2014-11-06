package Sodor

import Chisel._
import Node._
import Constants._
import Common._
import Common.Util._
import ReferenceChipBackend._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

class ShmacIo(implicit val conf: SodorConfiguration) extends Bundle
{
   val host = new HTIFIO()
   val mem  = new MemPortIo(conf.xprlen)
}

class ShmacUnit extends Module
{
   implicit val sodor_conf = SodorConfiguration()

   val io = new ShmacIo()

   val core   = Module(new Core(resetSignal = io.host.reset))
   val arbiter = Module(new SodorMemArbiter())

   core.io.imem <> arbiter.io.imem
   core.io.dmem <> arbiter.io.dmem
   arbiter.io.mem <> io.mem

   core.io.host <> io.host
}
