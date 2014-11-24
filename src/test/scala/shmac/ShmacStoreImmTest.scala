import Chisel.Tester
import main.scala.router.{West, East, North}
import main.scala.shmac.Shmac

class ShmacStoreImmTest(s: Shmac) extends Tester(s) {
  import s.proc.unit

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

  def ramResponse(instr: Int, addr: Int) = Array[BigInt](
    addr,   // Header address
    1,      // Header reply
    0,      // Header writeReq
    0,      // Header writeMask
    0,      // Header exop
    0,      // Header error
    instr,  // Payload
    0,      // Sender x
    0,      // Sender y
    1,      // Dest x
    0       // Dest y
  )

  def ramRequest(addr: Int) = Array[BigInt](
    addr,   // Header address
    0,      // Header reply
    0,      // Header writeReq
    0,      // Header writeMask
    0,      // Header exop
    0,      // Header error
    0,      // Payload
    0,      // Sender x
    0,      // Sender y
    1,      // Dest x
    0       // Dest y
  )

  def ramStoreRequest(payload: Int, addr: Int) = Array[BigInt](
    addr,   // Header address
    0,      // Header reply
    1,      // Header writeReq
    0,      // Header writeMask
    0,      // Header exop
    0,      // Header error
    payload,// Payload
    0,      // Sender x
    0,      // Sender y
    1,      // Dest x
    0       // Dest y
  )


  def checkImemRequest(valid: Int,  addr: Int, imem_resp_ready: Int = 1): Unit = {
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

  def checkDmemRequest(valid: Int, addr: Int, data: Int, fcn: Int = 1): Unit = {
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

  def peekArbiter(): Unit = {
    println("#-----")
    println("# Printing arbiter signals")
    peek(unit.arbiter.io.mem)
    peek(unit.arbiter.io.imem.req.ready)
    peek(unit.arbiter.io.imem.req.valid)
    peek(unit.arbiter.io.imem.req.bits.addr)
    peek(unit.arbiter.io.imem.req.bits.fcn)
    peek(unit.arbiter.io.imem.req.bits.typ)
    peek(unit.arbiter.io.imem.resp.ready)
    peek(unit.arbiter.io.imem.resp.valid)
    peek(unit.arbiter.io.imem.resp.bits.data)
    peek(unit.arbiter.io.dmem.req.ready)
    peek(unit.arbiter.io.dmem.req.valid)
    peek(unit.arbiter.io.dmem.req.bits.addr)
    peek(unit.arbiter.io.dmem.req.bits.data)
    peek(unit.arbiter.io.dmem.req.bits.fcn)
    peek(unit.arbiter.io.dmem.req.bits.typ)
    peek(unit.core.io.dmem.resp.ready)
    peek(unit.arbiter.io.dmem.resp.valid)
    peek(unit.arbiter.io.dmem.resp.bits.data)
    println("#-----")
  }

  def checkProcPort(port: Int, valid: Int, packet: Array[BigInt]): Unit = {
    expect(s.proc.io.ports(port).out.valid, valid)
    expect(s.proc.io.ports(port).out.bits, packet)
  }

  def checkRamPort(port: Int, valid: Int, packet: Array[BigInt]): Unit = {
    expect(s.ram.io.ports(port).out.valid, valid)
    expect(s.ram.io.ports(port).out.bits, packet)
  }

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
  poke(s.ram.io.ports(East.index).in.valid, 0)
  poke(s.ram.io.ports(East.index).in.bits, emptyPacket)
  step(8) // 10

  // Start the processor, it will start fetching instructions
  poke(s.io.host.reset, 0)

  checkImemRequest(1, 0x2000)

  step(1)

  checkImemRequest(0, 0x2004)

  step(1)

  // It takes two cycles to reach the Sodor-tiles output port
  expect(s.proc.io.ports(East.index).out.valid, 1)
  expect(s.proc.io.ports(East.index).out.bits, ramRequest(0x2000))

  expect(s.ram.io.ports(West.index).in.ready, 1)
  expect(s.ram.io.ports(West.index).in.bits, ramRequest(0x2000))

  step(4)

  // It takes four cycles from a packet is at the input port until the
  // respective response is at the output port
  expect(s.ram.io.ports(West.index).out.valid, 1)
  expect(s.ram.io.ports(West.index).out.bits, ramResponse(ld_a, 0x2000))

  expect(s.proc.io.ports(East.index).in.ready, 1)
  expect(s.proc.io.ports(East.index).in.bits, ramResponse(ld_a, 0x2000))

  step(3)

  // Two cycles for the packet to reach the processor, it issues a request for
  // the next instruction on the next cycle
  checkImemRequest(1, 0x2004)

  step(2)

  expect(s.proc.io.ports(East.index).out.valid, 1)
  expect(s.proc.io.ports(East.index).out.bits, ramRequest(0x2004))

  expect(s.ram.io.ports(West.index).in.ready, 1)
  expect(s.ram.io.ports(West.index).in.bits, ramRequest(0x2004))

  step(4)

  expect(s.ram.io.ports(West.index).out.valid, 1)
  expect(s.ram.io.ports(West.index).out.bits, ramResponse(sw_a, 0x2004))

  expect(s.proc.io.ports(East.index).in.ready, 1)
  expect(s.proc.io.ports(East.index).in.bits, ramResponse(sw_a, 0x2004))

  step(3)

  checkDmemRequest(1, 0xa, 0x1000, fcn = 1)

  step(1)

  checkImemRequest(1, 0x2008)

  step(1)

  expect(s.proc.io.ports(East.index).out.valid, 1)
  expect(s.proc.io.ports(East.index).out.bits, ramStoreRequest(0x1000, 0xa))

  expect(s.ram.io.ports(West.index).in.ready, 1)
  expect(s.ram.io.ports(West.index).in.bits, ramStoreRequest(0x1000, 0xa))

  step(1)

  poke(s.ram.io.ports(East.index).in.valid, 1)
  poke(s.ram.io.ports(East.index).in.bits, readData(0xa))

  step(1)

  poke(s.ram.io.ports(East.index).in.valid, 0)
  poke(s.ram.io.ports(East.index).in.bits, emptyPacket)

  step(3)

  expect(s.ram.io.ports(East.index).out.valid, 1)
  expect(s.ram.io.ports(East.index).out.bits.header.address, 0xa)
  expect(s.ram.io.ports(East.index).out.bits.payload, 0x1000)

}
