package tiles

import Chisel._
import router._

class ReadCmd extends Bundle {
  val address = UInt(width = 32)
}

class WriteCmd(dataWidth: Int) extends ReadCmd {
  override def clone = new WriteCmd(dataWidth).asInstanceOf[this.type]
  val data = UInt(width = dataWidth)
}

class RamIO(dataWidth: Int) extends Bundle {
  override def clone = new RamIO(dataWidth).asInstanceOf[this.type]
  val reads  = new DeqIO(new ReadCmd())
  val writes = new DeqIO(new WriteCmd(dataWidth))
  val out    = new EnqIO(UInt(width = dataWidth))
}

class Ram(depth: Int, dataWidth: Int) extends Module {
  val io = new RamIO(dataWidth)

  val ram = Mem(UInt(width = dataWidth), depth)

  when (io.writes.valid) {
    val cmd = io.writes.deq()
    ram(cmd.address) := cmd.data
  }.elsewhen (io.reads.valid && io.out.ready) {
    val cmd = io.reads.deq()
    io.out.enq(ram(cmd.address))
  }
}
