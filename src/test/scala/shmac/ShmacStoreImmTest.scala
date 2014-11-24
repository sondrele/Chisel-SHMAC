package shmac

import main.scala.shmac.Shmac

class ShmacStoreImmTest(s: Shmac) extends ShmacTester(s) {

  // Stop processor while memory is filled with instructions
  poke(s.io.host.reset, 1)

  // I-type      Width         rd            LUI
  val ld_a   = (0x1 << 12) | (0x1 << 7)  | 0x37
  // S-type     rs2           Base          Function      Addr        SW
  val sw_a   = (0x1 << 20) | (0x0 << 15) | (0x2 << 12) | (0xa << 7) | 0x23

  // Write the instructions to ram
  loadProgram(Array(ld_a, sw_a))

  // Start the processor, it will start fetching instructions
  poke(s.io.host.reset, 0)

  checkImemRequest(0x2000, 1)

  step(1)

  checkImemRequest(0x2004, 0)

  step(1)

  // It takes two cycles to reach the Sodor-tiles output port
  checkPortReadRequest(0x2000)

  step(4)

  // It takes four cycles from a packet is at the input port until the
  // respective response is at the output port
  checkPortReadResponse(0x2000, ld_a)

  step(3)

  // Two cycles for the packet to reach the processor, it issues a request for
  // the next instruction on the next cycle
  checkImemRequest(0x2004, 1)

  step(2)

  checkPortReadRequest(0x2004)

  step(4)

  checkPortReadResponse(0x2004, sw_a)

  step(3)

  checkDmemRequest(0xa, 0x1000, 1, fcn = 1)

  step(1)

  checkImemRequest(0x2008, 1)

  step(1)

  checkPortStoreRequest(0xa, 0x1000)

  step(1)

  // Integration test over, verify that it was working successfully
  verifyRamData(0xa, 0x1000)
}
