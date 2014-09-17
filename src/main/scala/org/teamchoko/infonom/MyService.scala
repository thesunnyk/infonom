package org.teamchoko.infonom

import scala.concurrent.ExecutionContext
import org.http4s.server.HttpService
import org.http4s.dsl._

trait MyService {
  val hello = "Hello"

  def service(implicit executionContext: ExecutionContext = ExecutionContext.global): HttpService = {
    case GET -> Root =>
      Ok("pong")
  }

}
