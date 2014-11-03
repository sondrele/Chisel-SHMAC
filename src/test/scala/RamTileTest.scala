package test

import Chisel._
import tiles._

class RamTileTest(t: RamTile) extends Tester(t) {
  def peekAtRamTile() {
    peek(t.ram)
  }

  def emptyPacket = Array[BigInt](0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

  def writeFromEastToLocal = Array[BigInt](10, 0, 1, 0, 0, 0, 15, 2, 1, 1, 1)

  def readFromEastToLocal = Array[BigInt](10, 0, 0, 0, 0, 0, 0, 2, 1, 1, 1)

  def responseFromLocalToEast = Array[BigInt](10, 1, 0, 0, 0, 0, 15, 2, 1, 1, 1)

  // Cycle 0: Send data to the tiles east input port
  poke(t.io.ports(0).in.valid, 1)
  poke(t.io.ports(0).in.bits, writeFromEastToLocal)
  poke(t.io.ports(0).out.ready, 0)

  expect(t.io.ports(0).in.ready, 1)
  expect(t.io.ports(0).out.valid, 0) // output port should be empty
  expect(t.io.ports(0).out.bits, emptyPacket)

  peekAtRamTile()

  step(1)

  // Cycle 1: Stop sending data, and wait for it to arrive the
  // local output port
  poke(t.io.ports(0).in.valid, 0)
  poke(t.io.ports(0).in.bits, emptyPacket)
  poke(t.io.ports(0).out.ready, 0)

  step(1)

  // Cycle 2: The packet should now have reached the output port of
  // the local router. The payload and data from the packet is extracted
  // and further transmitted to ram.io.writes
  expect(t.router.ports(4).out.bits, writeFromEastToLocal)
  expect(t.ram.writes.bits.data, 15)
  expect(t.ram.writes.bits.address, 10)

  peekAtRamTile()

  step(1)

  // Cycle 3: The data should now have been written to ram.
  // Issue a read-request to the ram-tile and expect the packet to
  // arrive to the east output port two cycles later
  poke(t.io.ports(0).in.valid, 1)
  poke(t.io.ports(0).in.bits, readFromEastToLocal)
  poke(t.io.ports(0).out.ready, 0)

  step(1)

  // Cycle 4: Stop sending the read-request
  poke(t.io.ports(0).in.valid, 0)
  poke(t.io.ports(0).in.bits, emptyPacket)
  poke(t.io.ports(0).out.ready, 0)

  peekAtRamTile()

  step(1)

  // Cycle 5: Packet should have reached the tile and the
  // read request should be at the ram
  expect(t.router.ports(4).out.bits, readFromEastToLocal)
  expect(t.router.ports(4).out.valid, 1)
  expect(t.router.ports(4).in.ready, 1)
  expect(t.ram.reads.bits.address, 10)
  expect(t.ram.reads.ready, 1)
  expect(t.ram.out.bits, 15)
  expect(t.ram.out.valid, 1)
  expect(t.router.ports(4).in.bits, responseFromLocalToEast)
  peekAtRamTile()

  step(1)

  // Cycle 6: Packet should have left the local input port and
  // reached the input of the east output port
  expect(t.io.ports(0).out.valid, 0)

  peekAtRamTile()

  step(1)

  // Cycle 7: Packet is now at the east output port of the router
  // and the tile
  expect(t.io.ports(0).out.bits, responseFromLocalToEast)
  expect(t.io.ports(0).out.valid, 1)

  step(1)

  // Cycle 8: Read the data from east output port
  poke(t.io.ports(0).out.ready, 1)
  expect(t.io.ports(0).out.bits, responseFromLocalToEast)
  expect(t.io.ports(0).out.valid, 1)

  step(1)

  // Cycle 9: The data should now have left the east output port
  expect(t.io.ports(0).out.bits, emptyPacket)
}
