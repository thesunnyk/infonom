package org.teamchoko.infonom.carrot

import java.io.File
import scalaz.\/.fromTryCatchNonFatal

object SystemConfig {
  def getDataDirName = System.getProperty("infonom.data.dir", "data")

  def getDataDir: File = new File(getDataDirName)

  def ensureExists(dir: File): Boolean = if (dir.exists) true
  	else fromTryCatchNonFatal {
      dir.mkdirs()
    }.fold(x => false, x => x)
}
