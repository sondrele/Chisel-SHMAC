package test

import Chisel._
import tiles._

class SodorTileTest(t: SodorTile) extends Tester(t) {
  import t.unit

  def checkImemRequest(addr: BigInt): Unit = {
    expect(unit.io.mem.req.valid, 1) // Gets valid after req.ready is set
    expect(unit.io.mem.req.bits.fcn, 0)
    expect(unit.io.mem.req.bits.typ, 7)
    expect(unit.io.mem.req.bits.addr, addr)
    expect(unit.io.mem.resp.ready, 1)
    // Verify that the same signals is set for imem
    expect(unit.core.io.imem.req.valid, 1)
    expect(unit.core.io.imem.resp.ready, 1)
    expect(unit.core.io.imem.req.bits.fcn, 0)
    expect(unit.core.io.imem.req.bits.typ, 7)
    expect(unit.core.io.imem.req.bits.addr, addr)
    // Verify that there is no dmem request
    expect(unit.core.io.dmem.resp.ready, 0)
  }

  def peekArbiter(): Unit = {
    println("#-----")
    println("# Printing arbiter signals")
    peek(unit.arbiter.io.mem)
    peek(unit.arbiter.io.imem.req.ready)
    peek(unit.arbiter.io.imem.req.valid)
    peek(unit.arbiter.io.imem.req.bits.addr)
    peek(unit.arbiter.io.imem.req.bits.fcn)
    peek(unit.arbiter.io.imem.req.bits.typ)
    peek(unit.arbiter.io.imem.resp.ready)
    peek(unit.arbiter.io.imem.resp.valid)
    peek(unit.arbiter.io.imem.resp.bits.data)
    peek(unit.arbiter.io.dmem.req.ready)
    peek(unit.arbiter.io.dmem.req.valid)
    peek(unit.arbiter.io.dmem.req.bits.addr)
    peek(unit.arbiter.io.dmem.req.bits.data)
    peek(unit.arbiter.io.dmem.req.bits.fcn)
    peek(unit.arbiter.io.dmem.req.bits.typ)
    peek(unit.core.io.dmem.resp.ready)
    peek(unit.arbiter.io.dmem.resp.valid)
    peek(unit.arbiter.io.dmem.resp.bits.data)
    println("#-----")
  }

  // I-type      Width         rd            LUI
  val ld_a    = (0x1 << 12) | (0x1 << 7)  | 0x37 // 0x2000
  // S-type     rs2           Base          Function      Addr        SW
  val sw_a   = (0x1 << 20) | (0x0 << 15) | (0x2 << 12) | (0xa << 7) | 0x23 // 0x2004
  val ld_b    = (0x2 << 12) | (0x1 << 7)  | 0x37 // 0x2008
  val sw_b   = (0x1 << 20) | (0x0 << 15) | (0x2 << 12) | (0xb << 7) | 0x23 // 0x200c

  poke(t.io.host.reset, 1)
  poke(t.io.reqReady, 0)

  step(1) // 1

  poke(t.io.host.reset, 0)

  poke(t.io.reqReady, 1)
  checkImemRequest(0x2000)
}
