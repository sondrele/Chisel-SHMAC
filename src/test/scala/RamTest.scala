import Chisel._
import tiles._
import router._

class RamTest(r: Ram) extends Tester(r) {

  def write(addr: Int, data: Int) {
    poke(r.io.out.ready, 0)
    poke(r.io.reads.valid, 0)
    poke(r.io.writes.valid, 1)
    poke(r.io.writes.bits.data, data)
    poke(r.io.writes.bits.address, addr)
    step(1)
  }

  def read(addr: Int, expected: Int) {
    poke(r.io.writes.valid, 0)
    poke(r.io.out.ready, 1)
    poke(r.io.reads.valid, 1)
    poke(r.io.reads.bits.address, addr)
    step(1)
    expect(r.io.out.bits, expected)
  }

  def readData(addr: Int): Int = {
    poke(r.io.writes.valid, 0)
    poke(r.io.out.ready, 1)
    poke(r.io.reads.valid, 1)
    poke(r.io.reads.bits.address, addr)
    step(1)
    peek(r.io.out.bits).toInt
  }

  write(0, 15)
  write(7, 10)

  read(0, 15)
  read(7, 10)

  var sum = 0
  for (i <- 0 until 8) {
    sum += readData(i)
  }
  expect(sum == 25, "Sum of memory is 25")
}
