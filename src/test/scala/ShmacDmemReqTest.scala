package test

import Chisel._
import Sodor.ShmacUnit

class ShmacDmemReqTest(t: ShmacUnit) extends Tester(t) {

  def peekArbiter(): Unit = {
    println("#-----")
    println("# Printing arbiter signals")
    println("#-----")
    peek(t.arbiter.io.imem.req.ready)
    peek(t.arbiter.io.imem.req.valid)
    peek(t.arbiter.io.imem.req.bits.addr)
    //    peek(t.arbiter.io.imem.req.bits.data )
    peek(t.arbiter.io.imem.req.bits.fcn)
    peek(t.arbiter.io.imem.req.bits.typ)
    peek(t.arbiter.io.imem.resp.ready)
    peek(t.arbiter.io.imem.resp.valid)
    peek(t.arbiter.io.imem.resp.bits.data)
    peek(t.arbiter.io.dmem.req.ready)
    peek(t.arbiter.io.dmem.req.valid)
    peek(t.arbiter.io.dmem.req.bits.addr)
    peek(t.arbiter.io.dmem.req.bits.data)
    peek(t.arbiter.io.dmem.req.bits.fcn)
    peek(t.arbiter.io.dmem.req.bits.typ)
    //    peek(t.arbiter.io.dmem.resp.ready)
    peek(t.arbiter.io.dmem.resp.valid)
    peek(t.arbiter.io.dmem.resp.bits.data)
    peek(t.arbiter.io.mem)
  }

  poke(t.io.host.reset, 1)
  poke(t.io.mem.req.ready, 0)

  step(1)
  //             Address        Width         rd            LD
  val ld_a     = (0x123 << 20) | (0x2 << 12) | (0x1 << 7)  | 0x03
  //             Address        Width         rd            LD
  val ld_b     = (0x124 << 20) | (0x2 << 12) | (0x2 << 7)  | 0x03
  //             Function       rs2           rs1           rd            ADD
  val a_plus_b = (0x0 << 25)   | (0x2 << 20) | (0x1 << 15) | (0x3 << 7)  | 0x0
  //             Function       rs2           Base          Function      Addr       SW
  val sw_sum   = (0x0 << 25)   | (0x3 << 20) | (0x0 << 15) | (0x2 << 12) | (0xc << 7) | 0x23

  val imem_req_addr = 0x2000
  poke(t.io.host.reset, 0)

  // The processor should request the first instruction
  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1) // Gets valid after req.ready is set
  expect(t.core.io.imem.req.valid, 1)
  expect(t.io.mem.req.bits.addr, imem_req_addr)
  // Serve the first instruction
  expect(t.core.io.imem.resp.ready, 1) // Core ready to receive imem request
  expect(t.io.mem.resp.ready, 1) // Waiting for imem response at 0x2000
  poke(t.io.mem.resp.valid, 1) // Resp must be ready if req.ready is set
  poke(t.io.mem.resp.bits.data, ld_a) // ld from mem addr 0x123

  // Verify that there is no dmem request
  expect(t.core.io.dmem.resp.ready, 0) // Core not ready to receive dmem request
  expect(t.core.io.dmem.req.valid, 0) // Not requesting data
  expect(t.core.io.dmem.req.bits.addr, 0)
  expect(t.core.io.dmem.req.bits.data, 0)
  peekArbiter()

  step(1)

  // Not requesting instruction
  expect(t.core.io.imem.req.valid, 0)

  // Requesting data
  expect(t.core.io.dmem.req.valid, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.addr, 0x123) // LD address
  // Core is not ready to receive data before resp is valid
  poke(t.io.mem.resp.valid, 1)
  expect(t.io.mem.resp.ready, 1)
  expect(t.core.io.dmem.resp.ready, 0)

  expect(t.core.io.dmem.req.valid, 1) // Requesting data
  expect(t.core.io.dmem.req.bits.addr, 0x123)
  expect(t.core.io.dmem.req.bits.data, 0)

  peekArbiter()
  step(1)

  // Requesting next instruction
  expect(t.core.io.imem.req.valid, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.addr, 0x2004) // PC not increased
  expect(t.io.mem.resp.ready, 1)

  // Verify that there is no dmem request
  expect(t.core.io.dmem.resp.ready, 0) // Core ready to receive dmem request
  expect(t.core.io.dmem.req.valid, 0) // Not requesting data
  expect(t.core.io.dmem.req.bits.addr, 0x123)
  expect(t.core.io.dmem.req.bits.data, 0)
  peekArbiter()
  step(1)

}
