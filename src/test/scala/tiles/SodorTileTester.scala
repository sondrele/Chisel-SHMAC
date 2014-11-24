package tiles

import Chisel.Tester
import main.scala.tiles.SodorTile

class SodorTileTester(t: SodorTile) extends Tester(t) {
  import t.unit
  import t.{localPort => local}
  import t.io.ports

  val empty_packet = Array[BigInt](0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

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

    expect(local.in.bits.header.address, addr)
  }

  def checkImemPortRequest(port: Int, valid: Int, addr: Int): Unit = {
    expect(ports(port).out.valid, valid)
    expect(ports(port).out.bits.header.address, addr)
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

    expect(local.in.bits.header.address, addr)
  }

  def checkDmemPortRequest(port: Int, valid: Int, dmem_req: Array[BigInt]): Unit = {
    expect(ports(port).out.valid, valid)
    expect(ports(port).out.bits, dmem_req)
  }

  def checkLocalPort(valid: Int, packet: Array[BigInt]): Unit = {
    expect(local.out.ready, 1)
    expect(local.out.valid, valid)
    expect(local.out.bits, packet)
  }

  def respondWithPacket(port: Int, packet: Array[BigInt]): Unit = {
    expect(ports(port).out.valid, 1)
    poke(ports(port).in.valid, 1)
    poke(ports(port).in.bits, packet)
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

  def peekLocal(): Unit = {
    println("#-----")
    println("# Printing local port")
    peek(t.localPort)
    println("#-----")
  }

  def peekEast(): Unit = {
    println("#-----")
    println("# Printing east port")
    peek(ports(0))
    println("#-----")
  }

  def peekWest(): Unit = {
    println("#-----")
    println("# Printing east port")
    peek(ports(2))
    println("#-----")
  }

}
