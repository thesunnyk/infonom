package org.teamchoko.infonom.tomato

import scala.concurrent.ExecutionContext
import org.http4s.Method
import org.http4s.Request
import org.http4s.ResponseBuilder
import org.http4s.Status
import org.http4s.Uri
import org.http4s.server.HttpService
import org.teamchoko.infonom.carrot.Articles.CompleteArticleCase
import org.teamchoko.infonom.carrot.JsonArticles.CompleteArticleCodecJson
import org.teamchoko.infonom.tomato.Errors.StringError
import argonaut.Argonaut.StringToParseWrap
import scalaz.Scalaz._
import scodec.bits.ByteVector
import org.slf4j.Logger
import org.slf4j.LoggerFactory

trait MyService {
  val log = LoggerFactory.getLogger(classOf[MyService]);
  
  def render(body: ByteVector): StringError[Unit] = (for {
    article <- new String(body.toArray).decodeOption[CompleteArticleCase]
    		.toRightDisjunction("Could not decode article")
    _ = log.info("Got an article: {}", article.article.heading)
    saved <- SaveToFile.saveToFile(article)
    _ = log.info("Saved")
  } yield saved)
  
  def toError(err: StringError[Unit]): String = err.fold(err => err, succ => "Success")

  def makeString(body: ByteVector): String = new String(body.toArray)
  
  def service(implicit executionContext: ExecutionContext): HttpService = HttpService {
    case Request(Method.GET, Uri(_, _, "/", _, _), _, _, _, _) =>
      ResponseBuilder(Status.Ok, "Server is up")
    case Request(Method.POST, Uri(_, _, "/new/", _, _), _, _, doBody, _) =>
      ResponseBuilder(Status.Ok, doBody.map(x => toError(render(x))))
  }

}
