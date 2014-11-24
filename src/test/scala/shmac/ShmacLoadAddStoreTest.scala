package shmac

import main.scala.shmac.Shmac

class ShmacLoadAddStoreTest(s: Shmac) extends ShmacTester(s) {

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

}
