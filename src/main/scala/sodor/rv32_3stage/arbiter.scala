//**************************************************************************
// Arbiter for Princeton Architectures
//--------------------------------------------------------------------------
//
// Arbitrates instruction and data accesses to a single port memory.

package Sodor
{

import Chisel._
import Node._
import Common._

// arbitrates memory access
class SodorMemArbiter(implicit val conf: SodorConfiguration) extends Module
{
   // ID of requesting entity
   val MID_IMEM = UInt(0, 1)
   val MID_DMEM = UInt(1, 1)

   val outstanding_id = Reg(init=MID_IMEM)
   val outstanding = Bool()
   val outstanding_reg = Reg(init=Bool(false))

   val io = new Bundle
   {
      // TODO I need to come up with better names... this is too confusing 
      // from the point of view of the other modules
      val imem = new MemPortIo(conf.xprlen).flip // instruction fetch
      val dmem = new MemPortIo(conf.xprlen).flip // load/store 
      val mem  = new MemPortIo(conf.xprlen)      // the single-ported memory
   }

   //**********************************
   // Pipeline State Registers
   val req_fire_imem_reg = Reg(init=Bool(false))
   val req_fire_dmem_reg = Reg(init=Bool(false))

   //***************************
   // Figure out who gets to go
   val req_fire_dmem = Bool()
   val req_fire_imem = Bool()

   // default
   req_fire_dmem := Bool(false) 
   req_fire_imem := Bool(false) 

   when (io.dmem.req.valid)
   {
      req_fire_dmem := Bool(true)
   }
   .otherwise
   {
      req_fire_imem := Bool(true)
   }
 
   req_fire_imem_reg := req_fire_imem;
   req_fire_dmem_reg := req_fire_dmem;

   //***************************
   // apply back pressure as needed
   io.imem.req.ready := !req_fire_dmem && !outstanding && io.mem.req.ready
   io.dmem.req.ready := !outstanding && io.mem.req.ready

   //***************************
   // hook up requests

   when (io.mem.resp.valid)
   {
      outstanding_reg := Bool(false);
   }
   when (io.imem.req.valid || io.dmem.req.valid)
   {
      when (io.mem.req.ready)
      {
         outstanding_reg := Bool(true)
      }
      when (req_fire_dmem)
      {
         outstanding_id := MID_DMEM;
      }
      .otherwise
      {
         outstanding_id := MID_IMEM;
      }
   }

   outstanding := outstanding_reg && !io.mem.resp.valid;

   io.mem.req.valid     := io.imem.req.valid
   io.mem.req.bits.addr := io.imem.req.bits.addr
   io.mem.req.bits.fcn  := io.imem.req.bits.fcn
   io.mem.req.bits.typ  := io.imem.req.bits.typ
   io.mem.req.bits.excl  := io.imem.req.bits.excl

   when (req_fire_dmem)
   {
      io.mem.req.valid     := io.dmem.req.valid
      io.mem.req.bits.addr := io.dmem.req.bits.addr
      io.mem.req.bits.fcn  := io.dmem.req.bits.fcn
      io.mem.req.bits.typ  := io.dmem.req.bits.typ
      io.mem.req.bits.excl  := io.dmem.req.bits.excl
   }
   io.mem.req.bits.data := io.dmem.req.bits.data

   when (outstanding_id === MID_IMEM && !io.dmem.req.valid)
   {
      io.mem.resp.ready := io.imem.resp.ready
   }
   .otherwise
   {
      io.mem.resp.ready := Bool(true)
   }

   //***************************
   // hook up responses

   when (outstanding_id === MID_IMEM)
   {
      io.imem.resp.valid := io.mem.resp.valid
      io.dmem.resp.valid := Bool(false)
   }
   .otherwise
   {
      io.imem.resp.valid := Bool(false)
      io.dmem.resp.valid := io.mem.resp.valid
   }
   io.imem.resp.bits.data := io.mem.resp.bits.data
   io.dmem.resp.bits.data := io.mem.resp.bits.data
   io.imem.resp.bits.addr := io.mem.resp.bits.addr
   io.dmem.resp.bits.addr := io.mem.resp.bits.addr
   io.imem.resp.bits.error := io.mem.resp.bits.error
   io.dmem.resp.bits.error := io.mem.resp.bits.error
}
 
}
