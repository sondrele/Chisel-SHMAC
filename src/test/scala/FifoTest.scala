package test

import Chisel._
import router._

class FifoTest[T <: Bits](q: Fifo[T]) extends Tester(q) {
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
