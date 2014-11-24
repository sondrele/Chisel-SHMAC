package shmac

import Chisel.Tester
import main.scala.router.{West, East}
import main.scala.shmac.Shmac

class ShmacTester(s: Shmac) extends Tester(s) {
  import s.proc.unit

  def checkImemRequest(addr: Int, valid: Int, imem_resp_ready: Int = 1): Unit = {
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

  def checkDmemRequest(addr: Int, data: Int, valid: Int, fcn: Int = 1): Unit = {
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

  def checkPortStoreRequest(addr: Int, payload: Int): Unit = {
    expect(s.proc.io.ports(East.index).out.valid, 1)
    expect(s.proc.io.ports(East.index).out.bits, ramStoreRequest(payload, addr))

    expect(s.ram.io.ports(West.index).in.ready, 1)
    expect(s.ram.io.ports(West.index).in.bits, ramStoreRequest(payload, addr))
  }

  def checkPortReadResponse(addr: Int, payload: Int): Unit = {
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

  def ramResponse(payload: Int, addr: Int) = Array[BigInt](
    addr,   // Header address
    1,      // Header reply
    0,      // Header writeReq
    0,      // Header writeMask
    0,      // Header exop
    0,      // Header error
    payload,  // Payload
    0,      // Sender x
    0,      // Sender y
    1,      // Dest x
    0       // Dest y
  )

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
    data.foreach {
      case (addr, payload) =>
        poke(s.ram.io.ports(East.index).in.valid, 1)
        poke(s.ram.io.ports(East.index).in.bits, writeData(addr, payload))
        step(1)
    }
    poke(s.ram.io.ports(East.index).in.valid, 0)
    poke(s.ram.io.ports(East.index).in.bits, emptyPacket)
    step(2)
  }

  def verifyRamData(addr: Int, data: Int) {
    poke(s.ram.io.ports(East.index).in.valid, 1)
    poke(s.ram.io.ports(East.index).in.bits, readData(addr))

    step(1)

    poke(s.ram.io.ports(East.index).in.valid, 0)
    poke(s.ram.io.ports(East.index).in.bits, emptyPacket)

    step(3)

    expect(s.ram.io.ports(East.index).out.valid, 1)
    expect(s.ram.io.ports(East.index).out.bits.header.address, addr)
    expect(s.ram.io.ports(East.index).out.bits.payload, data)

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

}
