package org.teamchoko.infonom

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class MyServiceSpec extends FlatSpec with Matchers with MyService {

  "App Message" should "have a greeting" in {
    hello should equal("Hello")
  }
}
