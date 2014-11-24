package shmac

import Chisel.Tester
import main.scala.router.East
import main.scala.shmac.ConfigurableShmac
import main.scala.tiles.{SodorTileConfig, SodorTile, RamTile}

class ConfigurableShmacTest(s: ConfigurableShmac) extends Tester(s) {

  def checkImemRequest(addr: Int, valid: Int, imem_resp_ready: Int = 1): Unit = {
    s.tiles(0) match {
      case sodor if sodor.isInstanceOf[SodorTile] =>
        val proc = sodor.asInstanceOf[SodorTile]
        expect(proc.unit.io.mem.req.valid, valid) // Gets valid after req.ready is set
        expect(proc.unit .io.mem.req.bits.fcn, 0)
        expect(proc.unit .io.mem.req.bits.typ, 7)
        expect(proc.unit .io.mem.req.bits.addr, addr)
        expect(proc.unit .io.mem.resp.ready, 1)
        // Verify that the same signals is set for imem
        expect(proc.unit.core.io.imem.req.valid, valid)
        expect(proc.unit.core.io.imem.resp.ready, imem_resp_ready)
        expect(proc.unit.core.io.imem.req.bits.fcn, 0)
        expect(proc.unit.core.io.imem.req.bits.typ, 7)
        expect(proc.unit.core.io.imem.req.bits.addr, addr)
        // Verify that there is no dmem request
        expect(proc.unit.core.io.dmem.resp.ready, 0)
    }
  }

  def writeData(addr: Int, instr: Int) = Array[BigInt](
    addr,   // Header address
    0,      // Header reply
    1,      // Header writeReq
    0,      // Header writeMask
    0,      // Header exop
    0,      // Header error
    instr,  // Payload
    2,      // Sender x
    0,      // Sender y
    1,      // Dest x
    0       // Dest y
  )

  def readData(addr: Int, payload: Int = 0) = Array[BigInt](
    addr,   // Header address
    0,      // Header reply
    0,      // Header writeReq
    0,      // Header writeMask
    0,      // Header exop
    0,      // Header error
    payload,// Payload
    2,      // Sender x
    0,      // Sender y
    1,      // Dest x
    0       // Dest y
  )

  val emptyPacket = Array[BigInt](0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

  def loadProgram(instructions: Array[Int]): Unit = {
    val data = instructions.zipWithIndex.map {
      case (instr, index) => (0x2000 + index * 4, instr)
    }
    loadData(data)
  }

  def loadData(data: Array[(Int, Int)]): Unit = {
    s.tiles(1) match {
      case ram if ram.isInstanceOf[RamTile] =>
        data.foreach {
          case (addr, payload) =>
            poke(ram.io.ports(East.index).in.valid, 1)
            poke(ram.io.ports(East.index).in.bits, writeData(addr, payload))
            step(1)
        }
        poke(ram.io.ports(East.index).in.valid, 0)
        poke(ram.io.ports(East.index).in.bits, emptyPacket)
        step(2)

    }
  }

  def verifyRamData(addr: Int, data: Int) {
    s.tiles(1) match {
      case ram if ram.isInstanceOf[RamTile] =>
        poke(ram.io.ports(East.index).in.valid, 1)
        poke(ram.io.ports(East.index).in.bits, readData(addr))
        step(1)
        poke(ram.io.ports(East.index).in.valid, 0)
        poke(ram.io.ports(East.index).in.bits, emptyPacket)
        step(3)
        expect(ram.io.ports(East.index).out.valid, 1)
        expect(ram.io.ports(East.index).out.bits.header.address, addr)
        expect(ram.io.ports(East.index).out.bits.payload, data)
    }
  }

  // Stop processor while memory is filled with instructions
  poke(s.io.host.reset, 1)

  //             Address        Width         rd            LD
  val ld_a     = (0x120 << 20) | (0x2 << 12) | (0x2 << 7) | 0x03
  val ld_b     = (0x124 << 20) | (0x2 << 12) | (0x3 << 7) | 0x03
  //             Function      rs2           rs1           rd           ADD
  val add_ab   = (0x0 << 25) | (0x3 << 20) | (0x2 << 15) | (0x4 << 7) | 0x33
  //             rs2           Base          Function      Addr         SW
  val sw_ab    = (0x4 << 20) | (0x0 << 15) | (0x2 << 12) | (0xf << 7) | 0x23

  // Write the instructions to ram
  loadProgram(Array(ld_a, ld_b, add_ab, sw_ab))

  loadData(Array((0x120, 0xdead), (0x124, 0xbeef)))

  verifyRamData(0x120, 0xdead)

  // Start the processor, it will start fetching instructions
  poke(s.io.host.reset, 0)

  checkImemRequest(0x2000, 1)

  step(1)

  checkImemRequest(0x2004, 0)

  step(55)

  verifyRamData(0xf, 0xdead + 0xbeef)
}
