package shmac

import main.scala.shmac.Shmac

class ShmacBranchTileLocTest(s: Shmac) extends ShmacTester(s) {

  def testBranchingOnTileLoc(shouldBranch: Boolean): Unit = {
    // Stop processor while memory is filled with instructions
    poke(s.io.host.reset, 1)

    val tileLocValue = shouldBranch match { case false => 1 case true => 0}
    //             Address       Width         rd            LD
    val ld_loc   = (0x1 << 20) | (0x2 << 12) | (0x2 << 7) | 0x03
    val ld_a     = (0x0 << 20) | (0x2 << 12) | (0x3 << 7) | 0x03
    val ld_b     = (0x4 << 20) | (0x2 << 12) | (0x4 << 7) | 0x03
    // SB-type     rs2           rs1           BEQ           imm[4:1]     imm[11]      Branch
    val br_loc   = (0x3 << 20) | (0x2 << 15) | (0x0 << 12) | (0xc << 7) | (0x0 << 6) | 0x63
    //             Address        Width         rd            LD
    val ld_c     = (0x8 << 20) | (0x2 << 12) | (0x3 << 7) | 0x03 // 0x200c
    val ld_d     = (0xc << 20) | (0x2 << 12) | (0x4 << 7) | 0x03 // 0x2010
    //             Function      rs2           rs1           rd           ADD
    val add      = (0x0 << 25) | (0x4 << 20) | (0x3 << 15) | (0x5 << 7) | 0x33 // 0x2014
    //             rs2           Base          Function      Addr          SW
    val sw       = (0x5 << 20) | (0x0 << 15) | (0x2 << 12) | (0x10 << 7) | 0x23 // 0x2018

    // Write the instructions to ram
    loadProgram(Array(ld_loc, ld_a, ld_b, br_loc, ld_c, ld_d, add, sw))

    loadData(Array((0x0, tileLocValue), (0x4, 0x2), (0x8, 0x3), (0xc, 0x4)))

    // Start the processor, it will start fetching instructions
    poke(s.io.host.reset, 0)

    checkImemRequest(0x2000, 1)

    step(9)

    // Two cycles for the packet to reach the processor, it issues a request for
    // the next instruction on the next cycle
    checkDmemRequest(0x1, 0x0, 1, 0)

    step(1)

    // It "only" takes two cycles to get the value of tile TileLoc
    peekArbiter()

    step(1)

    checkImemRequest(0x2004, 1)

    // Skip until the end, and verify the result based on the outcome of the branch
    step(130)

    // Integration test over, verify that it was working successfully
    // The TileLoc-value for the processor is 0, it does not branch if ld_a == 0
    if (shouldBranch) {
      // The branch is taken
      verifyRamData(0x10, 0x2)
    } else {
      // The branch is not taken
      verifyRamData(0x10, 0x7)
    }
  }

  // Test both outcomes
  testBranchingOnTileLoc(shouldBranch = true)

  reset(1)

  testBranchingOnTileLoc(shouldBranch = false)
}
