package test

import Chisel._
import tiles._

class SodorTileTest(t: SodorTile) extends Tester(t) {
  import t.unit

  def checkImemRequest(valid: Int, addr: Int): Unit = {
    expect(unit.io.mem.req.valid, valid) // Gets valid after req.ready is set
    expect(unit.io.mem.req.bits.fcn, 0)
    expect(unit.io.mem.req.bits.typ, 7)
    expect(unit.io.mem.req.bits.addr, addr)
    expect(unit.io.mem.resp.ready, 1)
    // Verify that the same signals is set for imem
    expect(unit.core.io.imem.req.valid, valid)
    expect(unit.core.io.imem.resp.ready, 1)
    expect(unit.core.io.imem.req.bits.fcn, 0)
    expect(unit.core.io.imem.req.bits.typ, 7)
    expect(unit.core.io.imem.req.bits.addr, addr)
    // Verify that there is no dmem request
    expect(unit.core.io.dmem.resp.ready, 0)

    expect(t.localPort.in.bits.header.address, addr)
  }

  def checkImemPortRequest(valid: Int, addr: Int): Unit = {
    expect(t.io.ports(0).out.valid, valid)
    expect(t.io.ports(0).out.bits.header.address, addr)
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

  def peekLocal(): Unit = {
    println("#-----")
    println("# Printing local port")
    peek(t.localPort)
    println("#-----")
  }

  def peekEast(): Unit = {
    println("#-----")
    println("# Printing east port")
    peek(t.io.ports(0))
    println("#-----")
  }

  // I-type      Width         rd            LUI
  val ld_a    = (0x1 << 12) | (0x1 << 7)  | 0x37 // 0x2000
  assert(ld_a < Int.MaxValue)
  // S-type     rs2           Base          Function      Addr        SW
  val sw_a   = (0x1 << 20) | (0x0 << 15) | (0x2 << 12) | (0xa << 7) | 0x23 // 0x2004
  val ld_b   = (0x2 << 12) | (0x1 << 7)  | 0x37 // 0x2008
  val sw_b   = (0x1 << 20) | (0x0 << 15) | (0x2 << 12) | (0xb << 7) | 0x23 // 0x200c

  def empty_packet = Array[BigInt](0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

  val imem_ld_request_packet = Array[BigInt](
    0,    // Header address
    1,    // Header reply
    0,    // Header writeReq
    0,    // Header writeMask
    0,    // Header exop
    0,    // Header error
    ld_a, // Payload
    1,    // Sender x - 1 because this is a reply
    1,    // Sender y
    2,    // Dest x   - 2 because this is a reply
    1     // Dest y
  )

  val imem_sw_request_packet = Array[BigInt](
    0,    // Header address
    1,    // Header reply
    0,    // Header writeReq
    0,    // Header writeMask
    0,    // Header exop
    0,    // Header error
    sw_a, // Payload
    1,    // Sender x - 1 because this is a reply
    1,    // Sender y
    2,    // Dest x   - 2 because this is a reply
    1     // Dest y
  )

  poke(t.io.host.reset, 1)
  poke(t.io.reqReady, 0)

  step(1) // 1

  // Start the processor
  poke(t.io.host.reset, 0)

  // Processor starts requesting the first instruction at 0x2000
  // when it knows that the receiver of its request is ready
  poke(t.io.reqReady, 1)
  checkImemRequest(1, 0x2000)

  peekLocal()
  // The requests sent to imem will be arriving at the tiles
  // east output port
  poke(t.io.ports(0).out.ready, 1)

  step(1) // 2

  // Processor is waiting for the response with the first instruction
  // to arrive, thus not requesting the next instruction
  checkImemRequest(0, 0x2004)

  // The request has not yet reached the east output port
  checkImemPortRequest(0, 0)

  step(1) // 3

  // The packet with the first imem request should be at the east output port
  checkImemPortRequest(1, 0x2000)
  // Serve the first instruction
  expect(t.io.ports(0).in.ready, 1)
  poke(t.io.ports(0).in.valid, 1)
  poke(t.io.ports(0).in.bits, imem_ld_request_packet)

  step(1) // 4

  checkImemPortRequest(0, 0)
  // Stop responding to invalid requests
  expect(t.io.ports(0).out.valid, 0)
  poke(t.io.ports(0).in.valid, 0)
  poke(t.io.ports(0).in.bits, empty_packet)

  // Wait for the packet to arrive the local output port
  expect(t.localPort.out.ready, 1)
  expect(t.localPort.out.valid, 0)
  expect(t.localPort.out.bits, empty_packet)

  step(1) // 5

  checkImemPortRequest(0, 0)
  // First instruction should have arrive the local output port
  expect(t.router.ports(4).out.ready, 1)
  expect(t.router.ports(4).out.valid, 1)
  expect(t.router.ports(4).out.bits, imem_ld_request_packet)

  // Verify that the instruction is valid
  expect(unit.io.mem.resp.valid, 1)
  expect(unit.io.mem.resp.bits.data, ld_a)

  step(1) // 6

  checkImemPortRequest(0, 0)
  // Processor should issue the request for the next instruction
  checkImemRequest(1, 0x2004)

  step(1) // 7

  // The 2nd request should not yet have arrive the output port
  checkImemPortRequest(0, 0)

  step(1) // 8

  // The packet with the next imem request should be at the east output port
  checkImemPortRequest(1, 0x2004)
  // Serve the next instruction
  expect(t.io.ports(0).in.ready, 1)
  poke(t.io.ports(0).in.valid, 1)
  poke(t.io.ports(0).in.bits, imem_sw_request_packet)

}
