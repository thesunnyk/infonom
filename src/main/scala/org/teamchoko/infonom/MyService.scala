package org.teamchoko.infonom

import scala.concurrent.ExecutionContext
import org.http4s.server.HttpService
import org.http4s.Uri
import org.http4s.Request
import org.http4s.Method
import org.http4s.Status
import org.http4s.ResponseBuilder

trait MyService {
  val hello = "Hello"
  // TODO Use markwrap to parse textile code.

  def service(implicit executionContext: ExecutionContext) = HttpService {
    case Request(Method.GET, Uri(_, _, "/", _, _), _, _, _, _) =>
      ResponseBuilder(Status.Ok, "pong")
  }

}
