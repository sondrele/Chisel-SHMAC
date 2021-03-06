package sodor

import Sodor.SodorUnit

class SodorAddImmTest(t: SodorUnit) extends SodorTester(t) {

  poke(t.io.host.reset, 1)
  poke(t.io.mem.req.ready, 0)

  step(1) // Cycle 1
  // I-type      Width         rd            LUI
  val ld_a    = (0x1 << 12) | (0x1 << 7)  | 0x37 // 0x2000
  val ld_b    = (0x2 << 12) | (0x2 << 7)  | 0x37 // 0x2004
  // R-type     Function       rs2           rs1           rd            ADD
  val add_ab  = (0x0 << 25)   | (0x2 << 20) | (0x1 << 15) | (0x3 << 7)  | 0x33 // 0x2008
  // S-type     rs2           Base          Function      Addr        SW
  val sw_ab   = (0x3 << 20) | (0x0 << 15) | (0x2 << 12) | (0x10 << 7) | 0x23 // 0x200c

  // I-type      Width         rd            LUI
  val ld_c    = (0x3 << 12) | (0x3 << 7)  | 0x37 // 0x2010
  val ld_d    = (0x4 << 12) | (0x4 << 7)  | 0x37 // 0x2014
  // R-type     Function       rs2           rs1           rd            ADD
  val add_cd  = (0x0 << 25)   | (0x4 << 20) | (0x3 << 15) | (0x5 << 7)  | 0x33 // 0x2018
  // S-type     rs2           Base          Function      Addr        SW
  val sw_cd   = (0x3 << 20) | (0x0 << 15) | (0x2 << 12) | (0x14 << 7) | 0x23 // 0x201c

  poke(t.io.host.reset, 0)

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1) // Gets valid after req.ready is set
  expect(t.io.mem.req.bits.fcn, 0)
  expect(t.io.mem.req.bits.typ, 7)
  expect(t.io.mem.req.bits.addr, 0x2000)
  expect(t.io.mem.req.bits.data, 0)
  poke(t.io.mem.resp.valid, 1) // Resp must be ready if req.ready is set
  poke(t.io.mem.resp.bits.data, ld_a) // Serve the first instruction
  expect(t.io.mem.resp.ready, 1) // Waiting for imem response at 0x2000


  // Not requesting instruction
  expect(t.core.io.imem.req.valid, 1)
  expect(t.core.io.imem.req.ready, 1)

  // Requesting data
  expect(t.core.io.dmem.req.valid, 0)
  expect(t.core.io.dmem.req.ready, 1)

  peekArbiter()
  step(1) // Cycle 2

  expect(t.io.mem.req.valid, 0)
  poke(t.io.mem.req.ready, 0)

  step(1) // Cycle 3

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 0) // Load
  expect(t.io.mem.req.bits.typ, 7) // Imem? Valid?
  expect(t.io.mem.req.bits.addr, 0x2004)
  expect(t.io.mem.req.bits.data, 0)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, ld_b)
  expect(t.io.mem.resp.ready, 1)


  // Not requesting instruction
  expect(t.core.io.imem.req.valid, 1)
  expect(t.core.io.imem.req.ready, 1)

  // Requesting data
  expect(t.core.io.dmem.req.valid, 0)
  expect(t.core.io.dmem.req.ready, 1)

  peekArbiter()
  step(1) // Cycle 4

  expect(t.io.mem.req.valid, 0)
  poke(t.io.mem.req.ready, 0)

  step(1) // Cycle 5

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 0) // Load
  expect(t.io.mem.req.bits.typ, 7) // Imem? Valid?
  expect(t.io.mem.req.bits.addr, 0x2008)
  expect(t.io.mem.req.bits.data, 0)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, add_ab)
  expect(t.io.mem.resp.ready, 1)

  // Not requesting instruction
  expect(t.core.io.imem.req.valid, 1)
  expect(t.core.io.imem.req.ready, 1)

  // Requesting data
  expect(t.core.io.dmem.req.valid, 0)
  expect(t.core.io.dmem.req.ready, 1)

  peekArbiter()
  step(1) // Cycle 6

  expect(t.io.mem.req.valid, 0)
  poke(t.io.mem.req.ready, 0)

  step(1) // Cycle 7

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 0) // Load
  expect(t.io.mem.req.bits.typ, 7) // Imem? Valid?
  expect(t.io.mem.req.bits.addr, 0x200c)
  expect(t.io.mem.req.bits.data, 0x2000) // b
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, sw_ab)
  expect(t.io.mem.resp.ready, 1)

  // Requesting instruction
  expect(t.core.io.imem.req.valid, 1)
  expect(t.core.io.imem.req.ready, 1)

  // Not Requesting data
  expect(t.core.io.dmem.req.valid, 0)
  expect(t.core.io.dmem.req.ready, 1)

  peekArbiter()
  step(1) // Cycle 8

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 1) // Store
  expect(t.io.mem.req.bits.typ, 3) // Dmem? Valid?
  expect(t.io.mem.req.bits.addr, 0x10)
  expect(t.io.mem.req.bits.data, 0x3000)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, sw_ab)
  expect(t.io.mem.resp.ready, 1)

  // Requesting instruction
  expect(t.core.io.imem.req.valid, 0)
  expect(t.core.io.imem.req.ready, 0)

  // Not Requesting data
  expect(t.core.io.dmem.req.valid, 1)
  expect(t.core.io.dmem.req.ready, 1)

  step(1) // Cycle 9

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 0) // Load
  expect(t.io.mem.req.bits.typ, 7) // Imem? Valid?
  expect(t.io.mem.req.bits.addr, 0x2010)
  expect(t.io.mem.req.bits.data, 0x3000)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, ld_c)
  expect(t.io.mem.resp.ready, 1)

  // Not requesting instruction
  expect(t.core.io.imem.req.valid, 1)

  // Requesting data
  expect(t.core.io.dmem.req.valid, 0)
  expect(t.core.io.dmem.req.ready, 1)

  peekArbiter()
  step(1) // Cycle 10

  expect(t.io.mem.req.valid, 0)
  poke(t.io.mem.req.ready, 0)

  step(1) // Cycle 11

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 0) // Load
  expect(t.io.mem.req.bits.typ, 7) // Imem? Valid?
  expect(t.io.mem.req.bits.addr, 0x2014)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, ld_d)
  expect(t.io.mem.resp.ready, 1)

  // Not requesting instruction
  expect(t.core.io.imem.req.valid, 1)
  // Requesting data
  expect(t.core.io.dmem.req.valid, 0)

  step(1) // 12

  expect(t.io.mem.req.valid, 0)
  poke(t.io.mem.req.ready, 0)

  step(1) // 13

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 0) // Load
  expect(t.io.mem.req.bits.typ, 7) // Imem? Valid?
  expect(t.io.mem.req.bits.addr, 0x2018)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, add_cd)
  expect(t.io.mem.resp.ready, 1)

  // Not requesting instruction
  expect(t.core.io.imem.req.valid, 1)
  // Requesting data
  expect(t.core.io.dmem.req.valid, 0)

  step(1)

  expect(t.io.mem.req.valid, 0)
  poke(t.io.mem.req.ready, 0)

  step(1)

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 0) // Load
  expect(t.io.mem.req.bits.typ, 7) // Imem? Valid?
  expect(t.io.mem.req.bits.addr, 0x201c)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, sw_cd)
  expect(t.io.mem.resp.ready, 1)

  // Not requesting instruction
  expect(t.core.io.imem.req.valid, 1)
  // Requesting data
  expect(t.core.io.dmem.req.valid, 0)

  step(1)

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 1) // Store
  expect(t.io.mem.req.bits.typ, 3) // Dmem? Valid?
  expect(t.io.mem.req.bits.addr, 0x14)
  expect(t.io.mem.req.bits.data, 0x3000)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, sw_cd)
  expect(t.io.mem.resp.ready, 1)

  // Requesting instruction
  expect(t.core.io.imem.req.valid, 0)
  expect(t.core.io.imem.req.ready, 0)

  // Not Requesting data
  expect(t.core.io.dmem.req.valid, 1)
  expect(t.core.io.dmem.req.ready, 1)

  step(1)

  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.addr, 0x2020)
}
