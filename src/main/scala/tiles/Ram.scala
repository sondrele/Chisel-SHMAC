package tiles

import Chisel._
import router._

class ReadCmd extends Bundle {
  val address = UInt(width = 32)
}

class WriteCmd(w: Int) extends ReadCmd {
  override def clone = new WriteCmd(w).asInstanceOf[this.type]
  val data = UInt(width = w)
}

class RamIO(w: Int) extends Bundle {
  override def clone = new RamIO(w).asInstanceOf[this.type]
  val reads  = new DeqIO(new ReadCmd())
  val writes = new DeqIO(new WriteCmd(w))
  val out    = new EnqIO(UInt(width = w))
}

class Ram(depth: Int, w: Int) extends Module {
  val io = new RamIO(w)

  val ram = Mem(UInt(width = w), depth)

  when (io.writes.valid) {
    val cmd = io.writes.deq()
    ram(cmd.address) := cmd.data
  }.elsewhen (io.reads.valid && io.out.ready) {
    val cmd = io.reads.deq()
    io.out.enq(ram(cmd.address))
  }
}
