package Sodor

import Chisel._
import Node._
import Constants._
import Common._
import Common.Util._
import ReferenceChipBackend._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

class SodorUnitIO(implicit val conf: SodorConfiguration) extends Bundle
{
  val irq = Bool(INPUT)
  val host = new HTIFIO()
  val mem  = new MemPortIo(conf.xprlen)
}

class SodorUnit extends Module
{
  implicit val sodor_conf = SodorConfiguration()

  val io = new SodorUnitIO()

  val core   = Module(new Core(resetSignal = io.host.reset))
  val arbiter = Module(new SodorMemArbiter())

  core.io.irq <> io.irq

  core.io.imem <> arbiter.io.imem
  core.io.dmem <> arbiter.io.dmem
//  arbiter.io.dmem.resp.ready := core.io.dmem.resp.ready
  arbiter.io.mem <> io.mem

  core.io.host <> io.host
  // Connect other loose wires
  io.host.mem_req.ready := core.io.host.mem_req.ready
  io.host.mem_rep.valid := core.io.host.mem_rep.valid
  io.host.mem_rep.bits := core.io.host.mem_rep.bits

  arbiter.io.imem.req.bits.data := core.io.imem.req.bits.data
}
