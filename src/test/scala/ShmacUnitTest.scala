package test

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

  def checkSodorMemoryRequests(isReady: BigInt = 1): Unit = {
    peek(t.io.mem)
    peek(t.arbiter.io.mem)
    expect(t.core.io.imem.resp.ready, isReady) // Ready to receive next instruction
    expect(t.arbiter.io.mem.resp.ready, isReady) // Expecting the above signal go through to this
    expect(t.io.mem.resp.ready, isReady)
  }

  def peekArbiter(): Unit = {
    peek(t.arbiter.io.imem)
    peek(t.arbiter.io.dmem.req.ready)
    peek(t.arbiter.io.dmem.req.valid)
    peek(t.arbiter.io.dmem.req.bits.addr )
    peek(t.arbiter.io.dmem.req.bits.data )
    peek(t.arbiter.io.dmem.req.bits.fcn )
    peek(t.arbiter.io.dmem.req.bits.typ )
    peek(t.arbiter.io.dmem.resp.ready)
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
  poke(t.io.host.reset, 0)
  step(1)

  // The processor should request the first instruction
  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1) // Gets valid after req.ready is set
  expect(t.io.mem.req.bits.addr, 0x2000)
  // Serve the first instruction
  expect(t.io.mem.resp.ready, 1) // Waiting for imem response at 0x2000
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, 0x6f) //Branch to self
  poke(t.io.mem.resp.bits.addr, 0x2000)
  // Verify that there is no dmem request
  expect(t.core.io.imem.req.ready, 1) // Core ready to receive imem request
  expect(t.core.io.dmem.req.ready, 1) // Core ready to receive dmem request
  expect(t.core.io.dmem.req.valid, 0) // Not requesting data
  expect(t.core.io.dmem.req.bits.addr, 0)
  expect(t.core.io.dmem.req.bits.data, 0)
  peekArbiter()
  step(1)

//  poke(t.io.mem.req.ready, 0)
  expect(t.io.mem.req.valid, 0) // Gets valid after req.ready is set
  expect(t.io.mem.req.bits.addr, 0x2004) // PC is increased
  // Serve the first instruction
  expect(t.io.mem.resp.ready, 1) // Waiting for imem response at 0x2000
//  poke(t.io.mem.resp.valid, 0)
  expect(t.io.mem.resp.bits.data, 0x6f) //Branch to self
  poke(t.io.mem.resp.bits.addr, 0x2004)

  peekArbiter()
  step(1)
  peekArbiter()
  step(1)
  peekArbiter()

  step(1)
  peekArbiter()
  step(1)
  peekArbiter()
  step(1)
  peekArbiter()

//  peek(t.core.io.dmem.resp.valid)
////  peek(t.core.io.dmem.resp.bits.addr) Not used
//  peek(t.core.io.dmem.resp.bits.data)
//
//  // The processor should request the next instruction
//  poke(t.io.mem.req.ready, 1)
//  expect(t.io.mem.req.valid, 1) // Gets valid after it req.ready is set
//  expect(t.io.mem.req.bits.addr, 0x2004)
//  // Serve the first instruction
//  expect(t.io.mem.resp.ready, 1) // Waiting for imem response at 0x2004
//  expect(t.io.mem.resp.valid, 1)
//  expect(t.io.mem.resp.bits.data, 0x6f) //Same as last
//
//  // Verify that there is no dmem request
//  expect(t.core.io.dmem.req.ready, 1) // Ready to receive dmem request
//  expect(t.core.io.dmem.req.valid, 0) // Not requesting data
//  expect(t.core.io.dmem.req.bits.addr, 0)
//  expect(t.core.io.dmem.req.bits.data, 0)

  step(1)
  peekArbiter()

  step(1)
  peekArbiter()
}
