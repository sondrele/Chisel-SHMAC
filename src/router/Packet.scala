package router

import Chisel._

class PacketHeader extends Bundle {
  val address      = UInt(width = 32)
  val reply        = Bool()
  val writeRequest = Bool()
  val writeMask    = UInt(width = 16)
  val exop         = UInt(width = 2)
  val error        = Bool()
}

class TileLocation extends Bundle {
  val x = UInt(width = 4)
  val y = UInt(width = 4)
}

class Packet extends Bundle {
  val header      = new PacketHeader() // 53 bits
  val data        = Vec.fill(16) { UInt(width = 8) } // 128 bits
  val sender      = new TileLocation() // 8 bits
  val destination = new TileLocation() // 8 bits
}

object Packet {
  val length = 197
}

class PacketData extends Bits {
  type T = PacketData

  def address: UInt = this(31, 0)
  def xDest: UInt = this(191, 188)
  def yDest: UInt = this(195, 192)

  override def fromInt(x: Int): this.type = {
    PacketData(x).asInstanceOf[this.type]
  }
}

object PacketData {
  val length = 197

  def apply(x: Int): PacketData = Lit(x) {
    PacketData()
  }

  def apply(dir: IODirection = null): PacketData = {
    val res = new PacketData()
    res.create(dir, width = PacketData.length)
    res
  }
}

class PacketDataModule extends Module {
  val io = new Bundle() {
    val packet = PacketData(INPUT)
    val address = UInt(OUTPUT, width = 32)
    val xDest = UInt(OUTPUT, width = 4)
    val yDest = UInt(OUTPUT, width = 4)
  }

  // val upper = Reg(io.out.getUpper)

  when (io.packet != UInt(0)) {
    // io.out := Cat(upper, io.inData)
    io.address := io.packet.address
    io.xDest := io.packet.xDest
    io.yDest := io.packet.yDest
  }.otherwise {
    // io.out := UInt(0)
    io.address := UInt(0)
    io.xDest := UInt(0)
    io.yDest := UInt(0)
  }
}

class PacketDataModuleTest(m: PacketDataModule) extends Tester(m) {
  expect(m.io.address, 0)
  step(1)
  poke(m.io.packet, 1)
  // expect(m.io.out, 1)
  expect(m.io.address, 1)
}
