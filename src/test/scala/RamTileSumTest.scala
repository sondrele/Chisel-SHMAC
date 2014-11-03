package test

import Chisel._
import tiles._
import router._

class RamTileSumTest(t: RamTile) extends Tester(t) {
  def peekAtRamTile() {
    peek(t.ram)
    peek(t.router)
  }


  // This tile is at coordinates (x:1, y:1)
  // Poking a bundle can either be done by explicitly poking every field of the bundle
  // or implicitly with an Array. If done implicitly, it is important to notice that
  // the last element in the array will be set to the first field in the bundle, and so on...
  def emptyPacket = Array[BigInt](0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

  // WritePackets has writeReq == true
  def writePacket(data: Int, addr: Int) = Array[BigInt](addr, 0, 1, 0, 0, 0, data, 2, 1, 1, 1)

  // ReadPackets has writeReq == false and payload == 0
  def readPacket(addr: Int) = Array[BigInt](addr, 0, 0, 0, 0, 0, 0, 2, 1, 1, 1)


  // ResponsePackets has writeReq == false and reply == true
  def responsePacket(data: Int, addr: Int) = Array[BigInt](addr, 1, 0, 0, 0, 0, data, 2, 1, 1, 1)

  // Issue 8 write-requests to memory with the values 1-8 to
  // the addresses 0-7
  for (i <- 0 until 8) {
    poke(t.io.ports(0).in.valid, 1)
    poke(t.io.ports(0).in.bits, writePacket(i + 1, i))
    poke(t.io.ports(0).out.ready, 0)
    step(1)
  }

  var sum = 0
  for (i <- 0 until 13) {
    // Issue read-requests for the 8 next cycles. Read the written
    // packets in sequential order, at addresses 0-7
    if (i < 8) {
      poke(t.io.ports(0).in.valid, 1)
      poke(t.io.ports(0).in.bits, readPacket(i))
    } else {
      poke(t.io.ports(0).in.valid, 0)
      poke(t.io.ports(0).in.bits, emptyPacket)
    }

    // It takes 2 cycles for the first read-request to arrive at the ram-tile,
    // and two cycles for the response to arrive back to the sender.
    val j = i - 4

    // Four cycles later, the east output fifo should have contain a packet for
    // the first read request
    if (j >= 0 && j < 8) {
      poke(t.io.ports(0).out.ready, 1)
      expect(t.io.ports(0).out.valid, 1)
      // Check that the packets is the response with data 1-8 for addresses 0-7
      // and sum together the values when the expected packet is at the output port
      if (expect(t.io.ports(0).out.bits, responsePacket(j + 1, j))) {
        sum += j + 1
      }
    } else {
      // Check that there is no out-request during the cycles
      // that we do not expect to read a packet
      expect(t.io.ports(0).out.valid, 0)
    }

    step(1)
  }

  expect(sum == 36, s"Should sum correctly $sum == 36")
}
