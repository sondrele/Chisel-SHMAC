package utils

import Chisel._

class Fifo(n: Int) extends Module {
  // TODO: Make a stand-alone bundle that other classes can
  // use to interface with Fifo
  val io = new Bundle {
    val write    = Bool(INPUT)
    val canWrite = Bool(OUTPUT) // False if full
    val inData   = UInt(INPUT,  width = 32)
    val read     = Bool(INPUT)
    val canRead  = Bool(OUTPUT) // False if empty
    val outData  = UInt(OUTPUT, width = 32)
  }

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

  // TODO: Verify control signals
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
  step(1)

  poke(q.io.write, 0)
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
}
