package utils

import Chisel._

class DecoupledFifoIO extends Bundle {
  // TODO: The width of inData and outData must be
  // given as an argument
  val in = Decoupled(UInt(width = 32)).flip()
  val out = Decoupled(UInt(width = 32))
}

class Fifo(n: Int) extends Module {
  val io = new DecoupledFifoIO()

  val enqPtr      = Reg(init = UInt(0, log2Up(n)))
  val deqPtr      = Reg(init = UInt(0, log2Up(n)))
  val isFull      = Reg(init = Bool(false))
  val doEnq       = io.in.ready && io.in.valid
  val doDeq       = io.out.valid && io.out.ready
  val isEmpty     = !isFull && (enqPtr === deqPtr)
  val deqPtrIncr  = deqPtr + UInt(1)
  val enqPtrIncr  = enqPtr + UInt(1)

  val is_full_next = Mux(
    doEnq && ~doDeq && (enqPtrIncr === deqPtr),
    Bool(true),
    Mux(doDeq && isFull, Bool(false), isFull)
  )

  enqPtr := Mux(doEnq, enqPtrIncr, enqPtr)
  deqPtr := Mux(doDeq, deqPtrIncr, deqPtr)
  isFull := is_full_next
  val ram = Mem(UInt(width = 32), n)
  when (doEnq) {
    ram(enqPtr) := io.in.bits
  }

  io.in.ready := !isFull
  io.out.valid := !isEmpty
  ram(deqPtr) <> io.out.bits
}

class FifoTest(q: Fifo) extends Tester(q) {
  val fst = 1
  val snd = 2
  val trd = 3
  val fth = 4

  def testFifoWrites() = {
    poke(q.io.in.valid, 1)
    poke(q.io.out.ready, 0)
    poke(q.io.in.bits, fst)
    step(1)
    poke(q.io.in.bits, snd)
    step(1)
    poke(q.io.in.bits, trd)
    step(1)
    poke(q.io.in.bits, fth)
    step(1)
    poke(q.io.in.bits, 5) // Does not work, queue is full
    expect(q.io.in.ready, 0)
    step(1)
  }

  def testFifoReads() = {
    poke(q.io.in.valid, 0)
    expect(q.io.out.valid, 1)
    expect(q.io.out.bits, fst)
    step(1)
    poke(q.io.out.ready, 1)
    expect(q.io.out.bits, fst)
    step(1)
    expect(q.io.out.bits, snd)
    step(1)
    expect(q.io.out.bits, trd)
    step(1)
    expect(q.io.out.bits, fth)
    step(1)
    expect(q.io.out.valid, 0)
  }

  testFifoWrites()
  testFifoReads()
}
