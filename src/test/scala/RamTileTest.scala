package test

import Chisel._
import tiles._
import router._

class RamTileTest(t: RamTile) extends Tester(t) {
//  def peekAtRamTile() {
//    peek(t.ram)
//    peek(t.router)
//  }
//
//  // This tile is at coordinates (x:1, y:1)
//  val writeFromEastToLocal = PacketData.create(
//    yDest = 1,
//    xDest = 1,
//    ySender = 1,
//    xSender = 2,
//    writeReq = true,
//    reply = false,
//    payload = 15,
//    address = 10
//  ).litValue()
//
//  val readFromEastToLocal = PacketData.create(
//    yDest = 1,
//    xDest = 1,
//    ySender = 1,
//    xSender = 2,
//    writeReq = false,
//    reply = false,
//    payload = 0,
//    address = 10
//  ).litValue()
//
//  val responseFromLocalToEast = PacketData.create(
//    yDest = 1,
//    xDest = 1,
//    ySender = 1,
//    xSender = 2,
//    writeReq = false,
//    reply = true,
//    payload = 15,
//    address = 10
//  ).litValue()
//
//  // Cycle 0: Send data to the tiles east input port
//  poke(t.io.ports(0).inRequest, 1)
//  poke(t.io.ports(0).inData, writeFromEastToLocal)
//  poke(t.io.ports(0).outReady, 0)
//
//  expect(t.io.ports(0).inReady, 1)
//  expect(t.io.ports(0).outRequest, 0) // output port should be empty
//  expect(t.io.ports(0).outData, 0)
//
//  peekAtRamTile()
//
//  step(1)
//
//  // Cycle 1: Stop sending data, and wait for it to arrive the
//  // local output port
//  poke(t.io.ports(0).inRequest, 0)
//  poke(t.io.ports(0).inData, 0)
//  poke(t.io.ports(0).outReady, 0)
//
//  step(1)
//
//  // Cycle 2: The packet should now have reached the output port of
//  // the local router. The payload and data from the packet is extracted
//  // and further transmitted to ram.io.writes
//  expect(t.router.ports(4).outData, writeFromEastToLocal)
//  expect(t.ram.writes.bits.data, 15)
//  expect(t.ram.writes.bits.address, 10)
//
//  peekAtRamTile()
//
//  step(1)
//
//  // Cycle 3: The data should now have been written to ram.
//  // Issue a read-request to the ram-tile and expect the packet to
//  // arrive to the east output port two cycles later
//  poke(t.io.ports(0).inRequest, 1)
//  poke(t.io.ports(0).inData, readFromEastToLocal)
//  poke(t.io.ports(0).outReady, 0)
//
//  step(1)
//
//  // Cycle 4: Stop sending the read-request
//  poke(t.io.ports(0).inRequest, 0)
//  poke(t.io.ports(0).inData, 0)
//  poke(t.io.ports(0).outReady, 0)
//
//  peekAtRamTile()
//
//  step(1)
//
//  // Cycle 5: Packet should have reached the tile and the
//  // read request should be at the ram
//  expect(t.router.ports(4).outData, readFromEastToLocal)
//  expect(t.router.ports(4).outRequest, 1)
//  expect(t.router.ports(4).inReady, 1)
//  expect(t.ram.reads.bits.address, 10)
//  expect(t.ram.reads.ready, 1)
//  expect(t.ram.out.bits, 15)
//  expect(t.ram.out.valid, 1)
//  expect(t.router.ports(4).inData, responseFromLocalToEast)
//
//  peekAtRamTile()
//
//  step(1)
//
//  // Cycle 6: Packet should have left the local input port and
//  // reached the input of the east output port
//  expect(t.io.ports(0).outRequest, 0)
//
//  peekAtRamTile()
//
//  step(1)
//
//  // Cycle 7: Packet is now at the east output port of the router
//  // and the tile
//  expect(t.io.ports(0).outData, responseFromLocalToEast)
//  expect(t.io.ports(0).outRequest, 1)
//
//  step(1)
//
//  // Cycle 8: Read the data from east output port
//  poke(t.io.ports(0).outReady, 1)
//  expect(t.io.ports(0).outData, responseFromLocalToEast)
//  expect(t.io.ports(0).outRequest, 1)
//
//  step(1)
//
//  // Cycle 9: The data should now have left the east output port
//  expect(t.io.ports(0).outData, 0)
//  expect(t.io.ports(0).outRequest, 0)
}
