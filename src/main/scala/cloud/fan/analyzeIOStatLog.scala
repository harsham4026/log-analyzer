package cloud.fan

import scala.collection.mutable.ArrayBuffer

/**
 * Created by cloud on 3/24/14.
 */
object analyzeIOStatLog extends LogAnalyzer {

  val group = "disk"
  val charts = Array(
    Chart("flashUtilization", "Flash Utilization", percentage),
    Chart("writeBandwidth", "Flash Write Bandwidth", throughput),
    Chart("readBandwidth", "Flash Read Bandwidth", throughput)
  )
  val command: String = "iostat -xk 10"

  def apply(nodeType: String, node: String, logDir: String) {
    val logIterator = analyzeLog.getLogContentIterator(command, node, logDir)
    val block = ArrayBuffer.empty[Array[String]]
    getBlock(logIterator, block)
    charts.foreach(_.series = block.map(_.head).toArray)
    analyzeLog.initCharts(nodeType, node, group, charts)
    while (true) {
      ChartSender.sendData(nodeType, node, group, charts(0).name, block.map(_.last).toArray)
      ChartSender.sendData(nodeType, node, group, charts(1).name, block.map(_(6)).toArray)
      ChartSender.sendData(nodeType, node, group, charts(2).name, block.map(_(5)).toArray)
      getBlock(logIterator, block)
    }
  }

  def getBlock(i: Iterator[String], block: ArrayBuffer[Array[String]]) {
    block.clear()
    i.find(_.startsWith("Device:"))
    var line = i.next()
    while (line != "") {
      val data = line.trim.split("\\s+")
      if (checkValidDevice(data.head)) {
        block += data
      }
      line = i.next()
    }
  }

  def checkValidDevice(name: String) = {
    name.matches("s[a-z]{2}") || name.startsWith("md")
  }
}
