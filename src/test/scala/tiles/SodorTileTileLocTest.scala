package tiles

import main.scala.router.{East, West}
import main.scala.tiles.SodorTile

class SodorTileTileLocTest(t: SodorTile) extends SodorTileTester(t) {
  import t.io.ports
  import t.unit


  // I-type      Address       Width         rd            LD
  val ld_a     = (0x1 << 20) | (0x2 << 12) | (0x2 << 7)  | 0x03
  // S-type      rs2           Base          Function      Addr        SW
  val sw_a     = (0x2 << 20) | (0x0 << 15) | (0x2 << 12) | (0xd << 7) | 0x23

  val imem_ld_request = Array[BigInt](
    0x2000, // Header address
    0,      // Header reply
    0,      // Header writeReq
    0,      // Header writeMask
    0,      // Header exop
    0,      // Header error
    0,      // Payload
    1,      // Sender x
    1,      // Sender y
    2,      // Dest x
    1       // Dest y
  )

  val imem_ld_response = Array[BigInt](
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

  val imem_sw_request = Array[BigInt](
    0x2004, // Header address
    0,      // Header reply
    0,      // Header writeReq
    0,      // Header writeMask
    0,      // Header exop
    0,      // Header error
    0,      // Payload
    1,      // Sender x
    1,      // Sender y
    2,      // Dest x
    1       // Dest y
  )

  val imem_sw_response = Array[BigInt](
    0,    // Header address
    1,    // Header reply
    0,    // Header writeReq
    0,    // Header writeMask
    0,    // Header exop
    0,    // Header error
    sw_a, // Payload
    1,    // Sender x
    1,    // Sender y
    2,    // Dest x - 2 because this is a reply
    1     // Dest y - 1 because this is a reply
  )

  val dmem_sw_request = Array[BigInt](
    0xd,    // Header address
    0,      // Header reply
    1,      // Header writeReq
    0,      // Header writeMask
    0,      // Header exop
    0,      // Header error
    0x3,    // Payload
    1,      // Sender x
    1,      // Sender y
    0,      // Dest x
    1       // Dest y
  )

  poke(t.io.host.reset, 1)

  step(1) // 1

  // Start the processor
  poke(t.io.host.reset, 0)

  // Processor starts requesting the first instruction at 0x2000
  // when it knows that the receiver of its request is ready
  checkImemRequest(1, 0x2000)
  // The requests sent to imem will be arriving at the tiles
  // east output port
  poke(ports(0).out.ready, 1)

  step(1) // 2

  // Processor is waiting for the response with the first instruction
  // to arrive, thus not requesting the next instruction
  checkImemRequest(0, 0x2004)
  // The request has not yet reached the east output port
  checkImemPortRequest(East.index, 0, 0)

  step(1) // 3

  // The packet with the first imem request should be at the east output port
  checkImemPortRequest(East.index, 1, 0x2000)
  // Serve the first instruction
  respondWithPacket(East.index, imem_ld_response)

  step(1) // 4

  checkImemPortRequest(East.index, 0, 0)
  // Stop responding to invalid requests
  expect(ports(0).out.valid, 0)
  poke(ports(0).in.valid, 0)
  poke(ports(0).in.bits, empty_packet)
  // Wait for the packet to arrive the local output port
  checkLocalPort(0, empty_packet)

  step(1) // 5

  checkImemPortRequest(East.index, 0, 0)
  // First instruction should have arrive the local output port
  checkLocalPort(1, imem_ld_response)
  // Verify that mem is waiting for instruction
  expect(unit.io.mem.resp.valid, 1)
  expect(unit.io.mem.resp.bits.data, ld_a)

  step(1) // 6

  checkImemPortRequest(East.index, 0, 0)
  // Processor should issue the request for the next instruction
  checkDmemRequest(1, 0x1, 0x0, 0)
  expect(t.localPort.in.valid, 0)

  expect(t.waitingForDmemReg, 0)
  expect(t.readingTileLocReg, 0)

  step(1) // 7

  // When readtingTileLocReg is true, the processor will only be stalled one cycle
  // in order for it to read the tile location during this cycle, and request a new
  // instruction on the next one
  expect(t.waitingForDmemReg, 1)
  expect(t.readingTileLocReg, 1)
  // Must stall this cycle
  expect(unit.io.mem.req.ready, 0)
  expect(unit.io.mem.req.valid, 0)
  expect(unit.io.mem.resp.ready, 1)
  expect(unit.io.mem.resp.valid, 1)
  expect(unit.io.mem.resp.bits.addr, 0x1)
  expect(unit.io.mem.resp.bits.data, 0x3)
  expect(t.localPort.in.valid, 0)

  checkImemRequest(0, 0x2004, imem_resp_ready = 1)
  // The dmem read request should never go off-tile
  checkDmemPortRequest(West.index, 0, empty_packet)

  step(1) // 8

  expect(t.waitingForDmemReg, 0)
  checkImemRequest(1, 0x2004)
  // The 2nd request should not yet have arrive the output port
  checkImemPortRequest(East.index, 0, 0)
  // The dmem read request should never go off-tile
  checkDmemPortRequest(West.index, 0, empty_packet)

  step(1) // 9

  // The 2nd request should not yet have arrive the output port
  checkImemPortRequest(East.index, 0, 0)

  step(1) // 10

  // The packet with the next imem request should be at the east output port
  checkImemPortRequest(East.index, 1, 0x2004)
  expect(ports(East.index).out.bits, imem_sw_request)
  // Respond with the next instruction
  respondWithPacket(East.index, imem_sw_response)

  step(1) // 11

  checkImemPortRequest(East.index, 0, 0)
  // Stop responding to invalid requests
  expect(ports(0).out.valid, 0)
  poke(ports(0).in.valid, 0)
  poke(ports(0).in.bits, empty_packet)

  // Wait for the packet to arrive the local output port
  checkLocalPort(0, empty_packet)

  step(1) // 12

  checkImemPortRequest(East.index, 0, 0)
  // First instruction should have arrive the local output port
  checkLocalPort(1, imem_sw_response)

  // Verify that mem is waiting for instruction
  expect(unit.io.mem.resp.valid, 1)
  expect(unit.io.mem.resp.bits.data, sw_a)

  step(1) // 13

  checkImemPortRequest(East.index, 0, 0)
  // Processor should issue store word request
  checkDmemRequest(1, 0xd, 0x3, fcn = 1)

  step(1) // 14

  // The resp.valid signal should be set automatically when the processor
  // issues a write-request because it's not waiting for a response
  expect(unit.io.mem.resp.valid, 1)
  // Issuing request for next instruction and don't care about a response for
  // the write
  checkImemRequest(1, 0x2008)
  // The request should not yet have arrived the output port
  checkDmemPortRequest(West.index, 0, empty_packet)

  step(1) // 15

  // The resp.valid should be back to normal on the next cycle
  expect(unit.io.mem.resp.valid, 0)
  // Waiting for the next instruction but not issuing before response at 0x2008
  checkImemRequest(0, 0x200c)
  // We pretend that the dmem is at tile (0, 0), thus the packet arrives
  // at west output instead of east
  checkDmemPortRequest(West.index, 1, dmem_sw_request)
}
