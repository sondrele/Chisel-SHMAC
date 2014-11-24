package shmac

import Chisel.Tester
import main.scala.router.{West, East}
import main.scala.shmac.Shmac

class ShmacTester(s: Shmac) extends Tester(s) {
  import s.proc.unit

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

  def checkPortReadRequest(addr: Int): Unit = {
    expect(s.proc.io.ports(East.index).out.valid, 1)
    expect(s.proc.io.ports(East.index).out.bits, ramReadRequest(addr))

    expect(s.ram.io.ports(West.index).in.ready, 1)
    expect(s.ram.io.ports(West.index).in.bits, ramReadRequest(addr))
  }

  def checkPortStoreRequest(payload: Int, addr: Int): Unit = {
    expect(s.proc.io.ports(East.index).out.valid, 1)
    expect(s.proc.io.ports(East.index).out.bits, ramStoreRequest(payload, addr))

    expect(s.ram.io.ports(West.index).in.ready, 1)
    expect(s.ram.io.ports(West.index).in.bits, ramStoreRequest(payload, addr))
  }

  def checkPortReadResponse(payload: Int, addr: Int): Unit = {
    expect(s.ram.io.ports(West.index).out.valid, 1)
    expect(s.ram.io.ports(West.index).out.bits, ramResponse(payload, addr))

    expect(s.proc.io.ports(East.index).in.ready, 1)
    expect(s.proc.io.ports(East.index).in.bits, ramResponse(payload, addr))
  }

  def ramReadRequest(addr: Int) = Array[BigInt](
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

  val emptyPacket = Array[BigInt](0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

  def loadProgram(instructions: Array[Int]): Unit = {
    for (i <- 0 until instructions.length) {
      poke(s.ram.io.ports(East.index).in.valid, 1)
      poke(s.ram.io.ports(East.index).in.bits, writeInstruction(instructions(i), 0x2000 + i * 4))
      step(1)
    }

    poke(s.ram.io.ports(East.index).in.valid, 0)
    poke(s.ram.io.ports(East.index).in.bits, emptyPacket)
    step(2)
  }

}
