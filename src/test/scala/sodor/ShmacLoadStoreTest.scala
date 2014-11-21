package sodor

import Sodor.ShmacUnit

class ShmacLoadStoreTest(t: ShmacUnit) extends ShmacTester(t) {

  poke(t.io.host.reset, 1)
  poke(t.io.mem.req.ready, 0)

  step(1) // 1
  // I-type      Address       Width         rd            LD
  val ld_a     = (0xa << 20) | (0x2 << 12) | (0x2 << 7)  | 0x03
  // S-type      rs2           Base          Function      Addr        SW
  val sw_a     = (0x2 << 20) | (0x0 << 15) | (0x2 << 12) | (0xd << 7) | 0x23

  poke(t.io.host.reset, 0)

  // The processor should request the first instruction
  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1) // Gets valid after req.ready is set
  expect(t.io.mem.req.bits.fcn, 0)
  expect(t.io.mem.req.bits.typ, 7)
  expect(t.io.mem.req.bits.addr, 0x2000)
  peek(t.io.mem.req.bits.data)
  poke(t.io.mem.resp.valid, 1) // Resp must be ready if req.ready is set
  poke(t.io.mem.resp.bits.data, ld_a) // Serve the first instruction
  expect(t.io.mem.resp.ready, 1) // Waiting for imem response at 0x2000

  // Check imem
  expect(t.core.io.imem.req.valid, 1)
  expect(t.core.io.imem.req.ready, 1)
  // Check dmem
  expect(t.core.io.dmem.req.valid, 0)
  expect(t.core.io.dmem.req.ready, 1)

  step(1) // 2

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 0) // Load
  expect(t.io.mem.req.bits.typ, 3) // Imem?
  expect(t.io.mem.req.bits.addr, 0xa)
  peek(t.io.mem.req.bits.data)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, 0xb)
  expect(t.io.mem.resp.ready, 1)

  // Check imem
  expect(t.core.io.imem.req.valid, 0)
  expect(t.core.io.imem.req.ready, 0)
  // Check dmem
  expect(t.core.io.dmem.req.valid, 1)
  expect(t.core.io.dmem.req.ready, 1)

  step(1) // 3
  // Wait for processor to execute instruction and load data
  // Not feeding the next instruction before next cycle
  poke(t.io.mem.req.ready, 0)
  expect(t.io.mem.req.valid, 0)
  peekArbiter()

  step(1) // 4

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 0) // Load
  expect(t.io.mem.req.bits.typ, 7) // Imem?
  expect(t.io.mem.req.bits.addr, 0x2004)
  peek(t.io.mem.req.bits.data)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, sw_a)
  expect(t.io.mem.resp.ready, 1)

  // Check imem
  expect(t.core.io.imem.req.valid, 1)
  expect(t.core.io.imem.req.ready, 1)
  // Check dmem
  expect(t.core.io.dmem.req.valid, 0)
  expect(t.core.io.dmem.req.ready, 1)

  step(1) // 5

  poke(t.io.mem.req.ready, 0)
  expect(t.io.mem.req.valid, 0)

  step(1) // 6

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 1) // Store
  expect(t.io.mem.req.bits.typ, 3) // Dmem?
  expect(t.io.mem.req.bits.addr, 0xd)
  expect(t.io.mem.req.bits.data, 0xb)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, 0)
  expect(t.io.mem.resp.ready, 1)

  // Check imem
  expect(t.core.io.imem.req.valid, 0)
  expect(t.core.io.imem.req.ready, 0)
  // Check dmem
  expect(t.core.io.dmem.req.valid, 1)
  expect(t.core.io.dmem.req.ready, 1)
  expect(t.core.io.dmem.req.bits.fcn, 1) // Store
  expect(t.core.io.dmem.req.bits.typ, 3) // Dmem?
  expect(t.core.io.dmem.req.bits.addr, 0xd)
  expect(t.core.io.dmem.req.bits.data, 0xb)

}
