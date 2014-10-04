package utils

import Chisel._

class FifoIO extends Bundle {
  // TODO: The width of inData and outData must be
  // given as an argument
  val write    = Bool(INPUT)
  val canWrite = Bool(OUTPUT) // False if full
  val inData   = UInt(INPUT,  width = 32)
  val read     = Bool(INPUT)
  val canRead  = Bool(OUTPUT) // False if empty
  val outData  = UInt(OUTPUT, width = 32)
}

class Fifo(n: Int) extends Module {
  val io = new FifoIO()

  val enqPtr      = Reg(init = UInt(0, log2Up(n)))
  val deqPtr      = Reg(init = UInt(0, log2Up(n)))
  val isFull      = Reg(init = Bool(false))
  val doEnq       = io.canWrite && io.write
  val doDeq       = io.read && io.canRead
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
    ram(enqPtr) := io.inData
  }

  io.canWrite := !isFull
  io.canRead  := !isEmpty
  ram(deqPtr) <> io.outData
}

class FifoTest(q: Fifo) extends Tester(q) {
  val fst = 1
  val snd = 2
  val trd = 3
  val fth = 4

  def testFifoWrites() = {
    poke(q.io.write, 1)
    poke(q.io.read, 0)
    poke(q.io.inData, fst)
    step(1)
    poke(q.io.inData, snd)
    step(1)
    poke(q.io.inData, trd)
    step(1)
    poke(q.io.inData, fth)
    step(1)
    poke(q.io.inData, 5) // Does not work, queue is full
    expect(q.io.canWrite, 0)
    step(1)
  }

  def testFifoReads() = {
    poke(q.io.write, 0)
    expect(q.io.canRead, 1)
    expect(q.io.outData, fst)
    step(1)
    poke(q.io.read, 1)
    expect(q.io.outData, fst)
    step(1)
    expect(q.io.outData, snd)
    step(1)
    expect(q.io.outData, trd)
    step(1)
    expect(q.io.outData, fth)
    step(1)
    expect(q.io.canRead, 0)
  }

  testFifoWrites()
  testFifoReads()
}
