package test

import Chisel._
import tiles._
import router._

class RamTileTest(t: RamTile) extends Tester(t) {
  def peekAtRamTile() {
    peek(t.ram)
  }

  def emptyPacket = Array[BigInt](0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

  def pokePacket(bundle: PacketBundle, writeReq: Int, reply: Int, payload: Int, address: Int): Unit = {
    poke(bundle.dest.y, 1)
    poke(bundle.dest.x, 1)
    poke(bundle.sender.y, 1)
    poke(bundle.sender.x, 2)
    poke(bundle.header.writeReq, writeReq)
    poke(bundle.header.reply, reply)
    poke(bundle.payload, payload)
    poke(bundle.header.address, address)
  }

  def expectPacket(bundle: PacketBundle, writeReq: Int, reply: Int, payload: Int, address: Int): Unit = {
    expect(bundle.dest.y, 1)
    expect(bundle.dest.x, 1)
    expect(bundle.sender.y, 1)
    expect(bundle.sender.x, 2)
    expect(bundle.header.writeReq, writeReq)
    expect(bundle.header.reply, reply)
    expect(bundle.payload, payload)
    expect(bundle.header.address, address)
  }

  // Cycle 0: Send data to the tiles east input port
  poke(t.io.ports(0).inRequest, 1)
  pokePacket(t.io.ports(0).inData, 1, 0, 15, 10) // writeFromEastToLocal
  poke(t.io.ports(0).outReady, 0)

  expect(t.io.ports(0).inReady, 1)
  expect(t.io.ports(0).outRequest, 0) // output port should be empty
  expect(t.io.ports(0).outData, emptyPacket)

  peekAtRamTile()

  step(1)

  // Cycle 1: Stop sending data, and wait for it to arrive the
  // local output port
  poke(t.io.ports(0).inRequest, 0)
  pokePacket(t.io.ports(0).inData, 0, 0, 0, 0)
  poke(t.io.ports(0).outReady, 0)

  step(1)

  // Cycle 2: The packet should now have reached the output port of
  // the local router. The payload and data from the packet is extracted
  // and further transmitted to ram.io.writes
  expectPacket(t.router.ports(4).outData, 1, 0, 15, 10) // writeFromEastToLocal
  expect(t.ram.writes.bits.data, 15)
  expect(t.ram.writes.bits.address, 10)

  peekAtRamTile()

  step(1)

  // Cycle 3: The data should now have been written to ram.
  // Issue a read-request to the ram-tile and expect the packet to
  // arrive to the east output port two cycles later
  poke(t.io.ports(0).inRequest, 1)
  pokePacket(t.io.ports(0).inData, 0, 0, 0, 10) // readFromEastToLocal
  poke(t.io.ports(0).outReady, 0)

  step(1)

  // Cycle 4: Stop sending the read-request
  poke(t.io.ports(0).inRequest, 0)
  pokePacket(t.io.ports(0).inData, 0, 0, 0, 0)
  poke(t.io.ports(0).outReady, 0)

  peekAtRamTile()

  step(1)

  // Cycle 5: Packet should have reached the tile and the
  // read request should be at the ram
  expectPacket(t.router.ports(4).outData, 0, 0, 0, 10) // readFromEastToLocal
  expect(t.router.ports(4).outRequest, 1)
  expect(t.router.ports(4).inReady, 1)
  expect(t.ram.reads.bits.address, 10)
  expect(t.ram.reads.ready, 1)
  expect(t.ram.out.bits, 15)
  expect(t.ram.out.valid, 1)
  expectPacket(t.router.ports(4).inData, 0, 1, 15, 10) // responseFromLocalToEast

  peekAtRamTile()

  step(1)

  // Cycle 6: Packet should have left the local input port and
  // reached the input of the east output port
  expect(t.io.ports(0).outRequest, 0)

  peekAtRamTile()

  step(1)

  // Cycle 7: Packet is now at the east output port of the router
  // and the tile
  expectPacket(t.io.ports(0).outData, 0, 1, 15, 10) // responseFromLocalToEast
  expect(t.io.ports(0).outRequest, 1)

  step(1)

  // Cycle 8: Read the data from east output port
  poke(t.io.ports(0).outReady, 1)
  expectPacket(t.io.ports(0).outData, 0, 1, 15, 10) // responseFromLocalToEast
  expect(t.io.ports(0).outRequest, 1)

  step(1)

  // Cycle 9: The data should now have left the east output port
  expect(t.io.ports(0).outData, emptyPacket)
}
