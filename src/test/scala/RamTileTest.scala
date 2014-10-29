package test

import Chisel._
import tiles._
import router._

class RamTileTest(t: RamTile) extends Tester(t) {
  def peekAtRamTile() {
    peek(t.ram)
    peek(t.router)
    peek(t.outPacket)
  }

  // This tile is at coordinates (x:1, y:1)
  val packetFromEastToLocal = PacketData.create(
    yDest = 1,
    xDest = 1,
    ySender = 1,
    xSender = 2,
    writeReq = true,
    reply = false,
    payload = 15,
    address = 0
  ).litValue()

  // Cycle 0: Send data to the tiles east input port
  poke(t.io.ports(0).inRequest, 1)
  poke(t.io.ports(0).inData, packetFromEastToLocal)
  poke(t.io.ports(0).outReady, 0)

  expect(t.io.ports(0).inReady, 1)
  expect(t.io.ports(0).outRequest, 0) // output port should be empty
  expect(t.io.ports(0).outData, 0)

  peekAtRamTile()

  step(1)

  // Cycle 1: Stop sending data, and wait for it to arrive the
  // local output port
  poke(t.io.ports(0).inRequest, 0)
  poke(t.io.ports(0).inData, 0)
  poke(t.io.ports(0).outReady, 0)

  peekAtRamTile()

  step(1)

  // Cycle 2: The packet should now have reached the output port of
  // the local router. The payload and data from the packet is extracted
  // and further transmitted to ram.io.writes
  expect(t.router.ports(4).outData, packetFromEastToLocal)
  expect(t.ram.writes.bits.data, 15)
  expect(t.ram.writes.bits.address, 0)

  peekAtRamTile()

  step(1)
}
