import Chisel.Tester
import main.scala.router.{East, North}
import main.scala.shmac.Shmac

class ShmacStoreImmTest(s: Shmac) extends Tester(s) {

  // Stop processor while memory is filled with instructions
  poke(s.io.host.reset, 1)

  def writeInstruction(instr: Int, addr: Int) = Array[BigInt](
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

  def readPacket(addr: Int) = Array[BigInt](
    addr,   // Header address
    0,      // Header reply
    0,      // Header writeReq
    0,      // Header writeMask
    0,      // Header exop
    0,      // Header error
    0,      // Payload
    2,      // Sender x
    0,      // Sender y
    1,      // Dest x
    0       // Dest y
  )

  val emptyPacket = Array[BigInt](0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

  // I-type      Width         rd            LUI
  val ld_a   = (0x1 << 12) | (0x1 << 7)  | 0x37
  // S-type     rs2           Base          Function      Addr        SW
  val sw_a   = (0x1 << 20) | (0x0 << 15) | (0x2 << 12) | (0xa << 7) | 0x23

  // Write the instructions to ram
  poke(s.ram.io.ports(East.index).in.valid, 1)
  poke(s.ram.io.ports(East.index).in.bits, writeInstruction(ld_a, 0x2000))
  step(1)
  poke(s.ram.io.ports(East.index).in.valid, 1)
  poke(s.ram.io.ports(East.index).in.bits, writeInstruction(sw_a, 0x2004))
  step(1)
  poke(s.ram.io.ports(East.index).in.valid, 1)
  poke(s.ram.io.ports(East.index).in.bits, readPacket(0x2000))
  step(1)
  poke(s.ram.io.ports(East.index).in.valid, 1)
  poke(s.ram.io.ports(East.index).in.bits, readPacket(0x2004))
  step(1)
  poke(s.ram.io.ports(East.index).in.valid, 0)
  poke(s.ram.io.ports(East.index).in.bits, emptyPacket)
  step(1)
  step(1)
  // Verify that the instructions were store correctly
  poke(s.ram.io.ports(East.index).out.ready, 1)
  expect(s.ram.io.ports(East.index).out.bits.payload, ld_a)
  step(1)
  expect(s.ram.io.ports(East.index).out.bits.payload, sw_a)


  step(1) // 1

  // Start the processor, it will start fetching instructions
  poke(s.io.host.reset, 0)
}
