package org.teamchoko.infonom

import org.scalatest._

class AppSpec extends FlatSpec with Matchers {

  "App Message" should "be a greeting" in {
    Boot.message should be ("Hello World")
  }
}
