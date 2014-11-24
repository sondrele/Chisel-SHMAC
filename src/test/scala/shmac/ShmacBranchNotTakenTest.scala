package shmac

import main.scala.shmac.Shmac

class ShmacBranchNotTakenTest(s: Shmac) extends ShmacTester(s) {

  // Stop processor while memory is filled with instructions
  poke(s.io.host.reset, 1)

  //             Address       Width         rd            LD
  val ld_a     = (0x0 << 20) | (0x2 << 12) | (0x2 << 7) | 0x03
  val ld_b     = (0x4 << 20) | (0x2 << 12) | (0x3 << 7) | 0x03
  // SB-type     rs2           rs1           BNE           imm[4:1]     imm[11]      Branch
  val br_ab    = (0x3 << 20) | (0x2 << 15) | (0x1 << 12) | (0xc << 7) | (0x0 << 6) | 0x63
  //             Address        Width         rd            LD
  val ld_c     = (0x8 << 20) | (0x2 << 12) | (0x2 << 7) | 0x03 // 0x200c
  val ld_d     = (0xc << 20) | (0x2 << 12) | (0x3 << 7) | 0x03 // 0x2010
  //             Function      rs2           rs1           rd           ADD
  val add      = (0x0 << 25) | (0x3 << 20) | (0x2 << 15) | (0x4 << 7) | 0x33 // 0x2014
  //             rs2           Base          Function      Addr          SW
  val sw       = (0x4 << 20) | (0x0 << 15) | (0x2 << 12) | (0x10 << 7) | 0x23 // 0x2018

  // Write the instructions to ram
  loadProgram(Array(ld_a, ld_b, br_ab, ld_c, ld_d, add, sw))

  loadData(Array((0x0, 0xa), (0x4, 0xa), (0x8, 0xc), (0xc, 0xd)))

  // Start the processor, it will start fetching instructions
  poke(s.io.host.reset, 0)

  checkImemRequest(0x2000, 1)

  step(9)

  // Two cycles for the packet to reach the processor, it issues a request for
  // the next instruction on the next cycle
  checkDmemRequest(0x0, 0x0, 1, 0)

  step(9)

  checkImemRequest(0x2004, 1)

  step(9)

  checkDmemRequest(0x4, 0x0, 1, 0)

  step(9)

  checkImemRequest(0x2008, 1)

  step(6)

  // The branch instruction is arriving the processor
  checkPortReadResponse(0x2008, br_ab)

  step(3)

  // The branch is not taken
  checkImemRequest(0x200c, 1)

  step(9)

  checkDmemRequest(0x8, 0x0, 1, 0)

  step(9)

  checkImemRequest(0x2010, 1)

  step(9)

  checkDmemRequest(0xc, 0x0, 1, 0)

  step(9)

  checkImemRequest(0x2014, 1)

  step(9)

  checkImemRequest(0x2018, 1)

  step(9)

  // Writing add-result to memory
  checkDmemRequest(0x10, 0xc + 0xd, 1)

  step(4)

  // Integration test over, verify that it was working successfully
  verifyRamData(0x10, 0xc + 0xd)
}
