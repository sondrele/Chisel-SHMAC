package sodor

import Chisel._
import Sodor.ShmacUnit

class ShmacUnitTest(t: ShmacUnit) extends Tester(t) {

  def checkCoreOutputs(mem_req_addr: Option[Int] = None) {
    expect(t.core.io.imem.req.bits.addr, 0x2000)

    mem_req_addr match {
      case Some(addr) =>
        expect(t.io.mem.req.valid, 1)
        expect(t.io.mem.req.bits.addr, addr)
      case None =>
        expect(t.io.mem.req.valid, 0)
    }
  }

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

  //Check stable reset behaviour
  for (i <- 1 to 10) {
    step(1)
    checkCoreOutputs()
  }

  step(1)
  checkCoreOutputs()
  step(1)
  val imem_req_addr = 0x2000
  poke(t.io.host.reset, 0)

  // The processor should request the first instruction
  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1) // Gets valid after req.ready is set
  expect(t.io.mem.req.bits.addr, imem_req_addr)
  // Serve the first instruction
  expect(t.core.io.imem.resp.ready, 1) // Core ready to receive imem request
  expect(t.io.mem.resp.ready, 1) // Waiting for imem response at 0x2000
  poke(t.io.mem.resp.valid, 1) // Resp must be ready if req.ready is set

  // Verify that there is no dmem request
  expect(t.core.io.dmem.resp.ready, 0) // Core not ready to receive dmem request
  expect(t.core.io.dmem.req.valid, 0) // Not requesting data
  expect(t.core.io.dmem.req.bits.addr, 0)
  expect(t.core.io.dmem.req.bits.data, 0)
  peekArbiter()

  step(1)

  // Verify that the PC is automaticallt increased
  for (i <- 1 to 10) {
    val next_pc = imem_req_addr + i * 4

    // Not requesting
    expect(t.core.io.imem.req.valid, 0)
    expect(t.io.mem.req.valid, 0)
    expect(t.io.mem.req.bits.addr, next_pc) // PC is increased
    expect(t.io.mem.resp.ready, 1)

//    peekArbiter()
    step(1)

    // Requesting next instruction
    expect(t.core.io.imem.req.valid, 1)
    expect(t.io.mem.req.valid, 1)
    expect(t.io.mem.req.bits.addr, next_pc) // PC not increased
    expect(t.io.mem.resp.ready, 1)

//    peekArbiter()
    step(1)
  }

  poke(t.io.host.reset, 1)
  step(1)
  poke(t.io.host.reset, 0)
  expect(t.io.mem.req.bits.addr, imem_req_addr)

}
