package utils

import Chisel._

class Fifo(n: Int) extends Module {
  val io = new Bundle {
    val enqVal = Bool(INPUT)
    val enqRdy = Bool(OUTPUT)
    val deqVal = Bool(OUTPUT)
    val deqRdy = Bool(INPUT)
    val enqData = UInt(INPUT,  width = 32)
    val deqData = UInt(OUTPUT, width = 32)
  }

  val enqPtr      = Reg(init = UInt(0, log2Up(n)))
  val deqPtr      = Reg(init = UInt(0, log2Up(n)))
  val isFull      = Reg(init = Bool(false))
  val doEnq       = io.enqRdy && io.enqVal
  val doDeq       = io.deqRdy && io.deqVal
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
    ram(enqPtr) := io.enqData
  }

  io.enqRdy := !isFull
  io.deqVal := !isEmpty
  ram(deqPtr) <> io.deqData
}

class FifoTest(q: Fifo) extends Tester(q) {
  val fst = 1
  val snd = 2
  val trd = 3
  val fth = 4
  poke(q.io.enqVal, 1)
  poke(q.io.deqRdy, 0)

  poke(q.io.enqData, fst)
  step(1)

  poke(q.io.enqData, snd)
  step(1)

  poke(q.io.enqData, trd)
  step(1)

  poke(q.io.enqData, fth)
  step(1)

  poke(q.io.enqData, 5) // Does not work, queue is full
  step(1)

  poke(q.io.enqVal, 0)
  expect(q.io.deqData, fst)
  step(1)

  poke(q.io.deqRdy, 1)
  expect(q.io.deqData, fst)
  step(1)

  expect(q.io.deqData, snd)
  step(1)

  expect(q.io.deqData, trd)
  step(1)

  expect(q.io.deqData, fth)
}
