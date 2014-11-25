package shmac

import Chisel.Tester
import Sodor.SodorUnit
import main.scala.router.East
import main.scala.shmac.Shmac2
import main.scala.tiles.{TileLoc, RamTile}

class Shmac2Test(s: Shmac2) extends Tester(s) {
  import s.proc00.{unit => p0}
  import s.proc01.{unit => p1}
  import s.{ram10 => imem}
  import s.{ram11 => dmem}

  def checkImemRequest(unit: SodorUnit, addr: Int, valid: Int, imem_resp_ready: Int = 1): Unit = {
    expect(unit.io.mem.req.valid, valid) // Gets valid after req.ready is set
    expect(unit.io.mem.req.bits.fcn, 0)
    expect(unit.io.mem.req.bits.typ, 7)
    expect(unit.io.mem.req.bits.addr, addr)
    expect(unit.io.mem.resp.ready, 1)
    // Verify that the same signals is set for imem
    expect(unit.core.io.imem.req.valid, valid)
    expect(unit.core.io.imem.resp.ready, imem_resp_ready)
    expect(unit.core.io.imem.req.bits.fcn, 0)
    expect(unit.core.io.imem.req.bits.typ, 7)
    expect(unit.core.io.imem.req.bits.addr, addr)
    // Verify that there is no dmem request
    expect(unit.core.io.dmem.resp.ready, 0)
  }

  def checkDmemRequest(unit: SodorUnit, addr: Int, data: Int, valid: Int, fcn: Int = 1): Unit = {
    expect(unit.io.mem.req.valid, valid) // Gets valid after req.ready is set
    expect(unit.io.mem.req.bits.fcn, fcn)
    expect(unit.io.mem.req.bits.typ, 3)
    expect(unit.io.mem.req.bits.addr, addr)
    expect(unit.io.mem.req.bits.data, data)
    expect(unit.io.mem.resp.ready, 1)
    // Verify that there is no imem request
    expect(unit.core.io.imem.resp.ready, 0)
    // Verify that the same signals is set for dmem
    expect(unit.core.io.dmem.req.valid, valid)
    expect(unit.core.io.dmem.req.bits.fcn, fcn)
    expect(unit.core.io.dmem.req.bits.typ, 3)
    expect(unit.core.io.dmem.req.bits.addr, addr)
    expect(unit.io.mem.req.bits.data, data)
  }

  val emptyPacket = Array[BigInt](0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

  def writeData(addr: Int, instr: Int, sx: Int, sy: Int, dx: Int, dy: Int) = Array[BigInt](
    addr,   // Header address
    0,      // Header reply
    1,      // Header writeReq
    0,      // Header writeMask
    0,      // Header exop
    0,      // Header error
    instr,  // Payload
    sx,     // Sender x
    sy,     // Sender y
    dx,     // Dest x
    dy      // Dest y
  )

  def readData(addr: Int, payload: Int = 0, sx: Int, sy: Int, dx: Int, dy: Int) = Array[BigInt](
    addr,   // Header address
    0,      // Header reply
    0,      // Header writeReq
    0,      // Header writeMask
    0,      // Header exop
    0,      // Header error
    payload,// Payload
    sx,     // Sender x
    sy,     // Sender y
    dx,     // Dest x
    dy      // Dest y
  )

  def loadProgram(ram: RamTile, instructions: Array[Int]): Unit = {
    val data = instructions.zipWithIndex.map {
      case (instr, index) => (0x2000 + index * 4, instr)
    }
    loadData(ram, s = TileLoc(2, 0), d = TileLoc(1, 0), data)
  }

  def loadData(ram: RamTile, s: TileLoc, d: TileLoc, data: Array[(Int, Int)]): Unit = {
    data.foreach {
      case (addr, payload) =>
        poke(ram.io.ports(East.index).in.valid, 1)
        poke(ram.io.ports(East.index).in.bits, writeData(addr, payload, s.x, s.y, d.x, d.y))
        step(1)
    }
    poke(ram.io.ports(East.index).in.valid, 0)
    poke(ram.io.ports(East.index).in.bits, emptyPacket)
    step(2)
  }

  def verifyRamData(ram: RamTile, s: TileLoc, d: TileLoc, addr: Int, data: Int) {
    poke(ram.io.ports(East.index).in.valid, 1)
    poke(ram.io.ports(East.index).in.bits, readData(addr, 0, s.x, s.y, d.x, d.y))

    step(1)

    poke(ram.io.ports(East.index).in.valid, 0)
    poke(ram.io.ports(East.index).in.bits, emptyPacket)

    step(3)

    poke(ram.io.ports(East.index).out.ready, 1)
    expect(ram.io.ports(East.index).out.valid, 1)
    expect(ram.io.ports(East.index).out.bits.header.address, addr)
    expect(ram.io.ports(East.index).out.bits.payload, data)
  }

  // Stop processor while memory is filled with instructions
  poke(s.io.host.reset, 1)

  //                                                                                                      //  P0       P1
  //             Address       Width         rd            LD                                             //-------------------
  val ld_loc   = (0x1 << 20) | (0x2 << 12) | (0x2 << 7) | 0x03                                            //| 0x2000 | 0x2000 |
  //             imm[10:5]     rs2           rs1           BEQ           imm[4:1]     imm[11]      Branch //|        |        |
  val br_loc   = (0x0 << 25) | (0x2 << 20) | (0x0 << 15) | (0x0 << 12) | (0x18 << 7) | (0x0 << 6) | 0x63  //| 0x2004 | 0x2004 |
  //             Address        Width         rd            LD                                            //|        |        |
  val ld_a     = (0x0 << 20) | (0x2 << 12) | (0x3 << 7) | 0x03                                            //|        | 0x2008 |
  val ld_b     = (0x4 << 20) | (0x2 << 12) | (0x4 << 7) | 0x03                                            //|        | 0x200c |
  //             Function      rs2           rs1           rd           ADD                               //|        |        |
  val add_ab   = (0x0 << 25) | (0x4 << 20) | (0x3 << 15) | (0x5 << 7) | 0x33 // 0x2014                    //|        | 0x2010 |
  //             rs2           Base          Function      Addr          SW                               //|        |        |
  val sw_ab    = (0x5 << 20) | (0x0 << 15) | (0x2 << 12) | (0x10 << 7) | 0x23 // 0x2018                   //|        | 0x2014 |
  //             imm[10:1]     rd           JAL                                                           //|        |        |
  val jump     = (0x14 << 21) | (0x0 << 7) | 0x6f                                                         //|        | 0x2018 |
  //             Address        Width         rd            LD                                            //|        |        |
  val ld_c     = (0x8 << 20) | (0x2 << 12) | (0x6 << 7) | 0x03                                            //| 0x2008 |        |
  val ld_d     = (0xc << 20) | (0x2 << 12) | (0x7 << 7) | 0x03                                            //| 0x200c |        |
  //             Function      rs2           rs1           rd           ADD                               //|        |        |
  val add_cd   = (0x0 << 25) | (0x6 << 20) | (0x7 << 15) | (0x8 << 7) | 0x33 // 0x2014                    //| 0x2010 |        |
  //             rs2           Base          Function      Addr          SW                               //|        |        |
  val sw_cd    = (0x8 << 20) | (0x0 << 15) | (0x2 << 12) | (0x14 << 7) | 0x23 // 0x2018                   //| 0x2014 |        |


  loadProgram(imem, Array(ld_loc, br_loc, ld_a, ld_b, add_ab, sw_ab, jump, ld_c, ld_d, add_cd, sw_cd))

  loadData(dmem, TileLoc(2, 1), TileLoc(1, 1), Array((0x0, 0x1), (0x4, 0x3), (0x8, 0x5), (0xc, 0x7)))

  step(1)

  poke(s.io.host.reset, 0)

  // Both processors requesting the first instruction
  checkImemRequest(p0, 0x2000, 1)
  checkImemRequest(p1, 0x2000, 1)

  step(9)

  // Requesting tile location register
  checkDmemRequest(p0, 0x1, 0x0, 1, 0)

  step(4)

  // It takes four more cycles for the 2nd processor to issue the same request
  // because the packet has to go though one additional router
  checkDmemRequest(p1, 0x1, 0x0, 1, 0)

  step(50)

  // None of the processors has finished the program
  // P0 writes the result of ld_c + ld_d to dmem[0x14] when its finished
  verifyRamData(dmem, s = TileLoc(2, 1), d = TileLoc(1, 1), 0x14, 0x0)
  // P1 writes the result of ld_a + ld_b to dmem[0x10] when its finished
  verifyRamData(dmem, s = TileLoc(2, 1), d = TileLoc(1, 1), 0x10, 0x0)

  step(24)

  // P0 has finished the program, and written its result to dmem[0x14]
  verifyRamData(dmem, s = TileLoc(2, 1), d = TileLoc(1, 1), 0x14, 0xc)
  // P1 has not yet written its result to dmem[0x10] because its further away from the imem
  verifyRamData(dmem, s = TileLoc(2, 1), d = TileLoc(1, 1), 0x10, 0x0)

  step(10)

  verifyRamData(dmem, s = TileLoc(2, 1), d = TileLoc(1, 1), 0x14, 0xc)
  // A few cycles later P1 has written the result to dmem as well
  verifyRamData(dmem, s = TileLoc(2, 1), d = TileLoc(1, 1), 0x10, 0x4)

}
