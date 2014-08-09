package org.teamchoko.infonom

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import spray.testkit.ScalatestRouteTest
import spray.routing.HttpService

class MyServiceSpec extends FlatSpec with Matchers with ScalatestRouteTest with HttpService with MyService {
  def actorRefFactory = system

  "App Message" should "have a greeting" in {
    Get() ~> myRoute ~> check {
      responseAs[String] should equal("PONG")
    }
  }
}
