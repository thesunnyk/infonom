package org.teamchoko.infonom

import java.io.File
import scalaz.\/.fromTryCatch

object SystemConfig {
  def getDataDirName = System.getProperty("infonom.data.dir", "data")

  def getDataDir: File = new File(getDataDirName)

  def ensureExists(dir: File): Boolean = if (dir.exists) true
  	else fromTryCatch {
      dir.mkdirs()
    }.fold(x => false, x => x)
}
