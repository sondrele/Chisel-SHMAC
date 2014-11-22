package main.scala.tiles

import Chisel._
import Common.{MemoryOpConstants, HTIFIO, SodorConfiguration}
import main.scala.router.{Packet, Router, RouterIO}
import Sodor._

case class SodorTileConf(imem: (Int, Int), dmem: (Int, Int))

class SodorTileIO(numPorts: Int) extends RouterIO(numPorts) {
  implicit val sodorConf = SodorConfiguration()
  val host = new HTIFIO()
}

class SodorTile(x: Int, y: Int, numPorts: Int, numRecords: Int)(implicit conf: SodorTileConf) extends Module with MemoryOpConstants {

  val io = new SodorTileIO(numPorts)

  val router = Module(new Router(x, y, numPorts + 1, numRecords)).io
  for (i <- 0 until numPorts) {
    io.ports(i) <> router.ports(i)
  }

  val unit = Module(new ShmacUnit())
  unit.io.host <> io.host

  def isImemRequest: Bool = {
    unit.io.mem.req.valid && unit.io.mem.req.bits.fcn === M_XRD && unit.io.mem.req.bits.typ === MT_WU
  }

  def isDmemRequest: Bool = {
    unit.io.mem.req.valid && unit.io.mem.req.bits.typ === MT_W
  }

  def isDmemReadRequest: Bool = {
    unit.io.mem.req.valid && unit.io.mem.req.bits.fcn === M_XRD  && unit.io.mem.req.bits.typ === MT_W
  }

  val localPort = router.ports(numPorts)
  val packet = localPort.out.bits
  val address = packet.header.address
  val payload = packet.payload
  val isResponse = packet.header.reply

  val writeValidDmemReg = Reg(Bool(), next = Bool(false))
  val waitingForDmemReg = Reg(Bool(), init = Bool(false))

  localPort.out.ready := unit.io.mem.resp.ready
  unit.io.mem.resp.valid := (localPort.out.valid && isResponse) || writeValidDmemReg
  unit.io.mem.resp.bits.addr := address
  unit.io.mem.resp.bits.data := payload

  val outPacket = new Packet()
  outPacket.sender.y := UInt(y, width = 4)
  outPacket.sender.x := UInt(x, width = 4)

  // The instruction memory is currently hard coded to be at tile (2, 1)
  when (isImemRequest) {
    outPacket.dest.y := UInt(conf.imem._2, width = 4)
    outPacket.dest.x := UInt(conf.imem._1, width = 4)
  }.elsewhen (isDmemRequest) {
    when (isDmemReadRequest) {
      waitingForDmemReg := Bool(true)
    }.otherwise {
      writeValidDmemReg := Bool(true)
    }
    outPacket.dest.y := UInt(conf.dmem._2, width = 4)
    outPacket.dest.x := UInt(conf.dmem._1, width = 4)
  }.elsewhen (isDmemReadRequest) {
    outPacket.dest.y := UInt(conf.dmem._2, width = 4)
    outPacket.dest.x := UInt(conf.dmem._1, width = 4)
  }.otherwise {
    outPacket.dest.y := UInt(0, width = 4)
    outPacket.dest.x := UInt(0, width = 4)
  }

  // When the processor is waiting for data after a dmem read request it will consume
  // the next package as data if mem.req.ready is 1
  // This fix stalls the processor until the first packet has arrived
  when (waitingForDmemReg) {
    unit.io.mem.req.ready := Bool(false)
    when (localPort.out.valid) {
      waitingForDmemReg := Bool(false)
    }
  }.otherwise {
    unit.io.mem.req.ready := localPort.in.ready
  }

  outPacket.header.writeReq := unit.io.mem.req.bits.fcn

  outPacket.payload := unit.io.mem.req.bits.data
  outPacket.header.address := unit.io.mem.req.bits.addr

  localPort.in.valid := unit.io.mem.req.valid
  localPort.in.bits.assign(outPacket)
}
