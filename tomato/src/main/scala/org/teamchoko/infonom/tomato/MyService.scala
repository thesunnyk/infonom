package org.teamchoko.infonom.tomato

import scala.concurrent.ExecutionContext
import org.http4s.server.HttpService
import org.http4s.Uri
import org.http4s.Request
import org.http4s.Method
import org.http4s.Status
import org.http4s.ResponseBuilder
import org.http4s.Response
import scalaz.concurrent.Task
import scodec.bits.ByteVector

trait MyService {
  
  def makeString(body: ByteVector): String = new String(body.toArray) + "\n"

  def service(implicit executionContext: ExecutionContext): HttpService = HttpService {
    case Request(Method.GET, Uri(_, _, "/", _, _), _, _, _, _) =>
      ResponseBuilder(Status.Ok, "Server is up")
    case Request(Method.POST, Uri(_, _, "/new/", _, _), _, _, doBody, _) =>
      ResponseBuilder(Status.Ok, doBody.map(makeString))
  }

}
