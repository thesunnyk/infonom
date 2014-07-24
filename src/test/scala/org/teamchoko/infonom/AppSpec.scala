package org.teamchoko.infonom

import org.scalatest._

class AppSpec extends FlatSpec with Matchers {

  "App Message" should "be a greeting" in {
    App.message should be ("Hello World")
  }
}
