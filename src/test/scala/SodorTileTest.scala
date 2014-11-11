class test

import Chisel._
import tiles._

class SodorTileTest(t: SodorTile) extends Tester(t) {
  import t.unit.{io => sodor}
  import t.io.host

  def checkCoreOutputs(mem_req_addr: Option[Int] = None,
                       host_ipi_req_valid: Int = 0,
                       host_csr_rep_valid: Int = 0) {

    mem_req_addr match {
      case Some(addr) =>
        expect(t.unit.io.mem.req.valid, 1)
        expect(sodor.mem.req.bits.addr, addr)
      case None =>
        expect(sodor.mem.req.valid, 0)
    }

    expect(sodor.host.ipi_req.valid, host_ipi_req_valid)
    expect(sodor.host.csr_rep.valid, host_csr_rep_valid)
  }

  def checkSodorMemoryRequests(): Unit = {
    expect(t.unit.core.io.imem.resp.ready, 1) // Ready to receive next instruction
    expect(t.unit.arbiter.io.mem.resp.ready, 1) // Expecting the above signal go through to this
    peek(sodor.mem)
    peek(t.unit.arbiter.io.mem)
    expect(t.unit.io.mem.resp.ready, 1)
    expect(t.localPort.out.ready, 1)
  }

  poke(host.reset, 1)
  poke(sodor.mem.req.ready, 0)
  poke(sodor.host.csr_req.valid, 0)
  poke(sodor.host.ipi_rep.valid, 0)

  //Check stable reset behaviour
  for (i <- 1 to 10) {
    step(1)
    checkCoreOutputs()
  }

  checkSodorMemoryRequests()
  step(1)
  checkSodorMemoryRequests()
  step(1)
  poke(host.reset, 0)
//  poke(t.sodor.io.mem.req.ready, 1)
//  poke(t.sodor.io.mem.resp.valid, 1)
//  poke(t.sodor.io.mem.resp.bits.data, 0x6f) //Branch to self

  peek(t.unit.core.io.imem)

  step(1)
  peek(t.unit.core.io.imem)
//  checkCoreOutputs()
}
