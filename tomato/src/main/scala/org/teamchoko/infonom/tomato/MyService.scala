package org.teamchoko.infonom.tomato

import scala.concurrent.ExecutionContext
import org.http4s.server.HttpService
import org.http4s.dsl.->
import org.http4s.dsl./
import org.http4s.dsl.Root
import org.http4s.dsl.Ok
import org.http4s.dsl.OkSyntax
import org.http4s.Method.GET
import org.http4s.Method.POST
import org.teamchoko.infonom.carrot.Articles.CompleteArticleCase
import org.teamchoko.infonom.carrot.JsonArticles.CompleteArticleCodecJson
import org.teamchoko.infonom.tomato.Errors.StringError
import argonaut.Argonaut.StringToParseWrap
import scalaz.Scalaz._
import scodec.bits.ByteVector
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import doobie.imports.ConnectionIO
import doobie.imports.DriverManagerTransactor
import doobie.imports.Query0
import doobie.imports.Update0
import doobie.imports.toMoreConnectionIOOps
import scalaz.concurrent.Task

trait MyService {
  val log = LoggerFactory.getLogger(classOf[MyService]);
  
  def saveArticle(body: ByteVector): StringError[Unit] = for {
    article <- new String(body.toArray).decodeEither[CompleteArticleCase]
    _ = log.info("Got an article: {}", article.article.heading)
    _ = DbConn.persistCompleteArticle(article).transact(DbConn.xa).attemptRun.leftMap(x => x.getMessage)
    _ = log.info("Saved")
  } yield ()

  def getAllArticles(): Task[List[CompleteArticleCase]] = DbConn.getAllCompleteArticles.transact(DbConn.xa)

  def saveAllFiles(articles: List[CompleteArticleCase]): StringError[Unit] =
    articles.map(SaveToFile.saveToFile).fold(().point[StringError])((x, y) => y)

  def publishAllArticles(): StringError[Unit] = for {
    articles <- getAllArticles.attemptRun.leftMap(x => x.getMessage)
    save <- saveAllFiles(articles)
  } yield ()

  def publishIndex(): StringError[Unit] = for {
    articles <- getAllArticles.attemptRun.leftMap(x => x.getMessage)
    save <- saveAllFiles(articles)
  } yield ()
  
  def toError(err: StringError[Unit]): String = err.fold(err => err, succ => "Success")

  def makeString(body: ByteVector): String = new String(body.toArray)
  
  def service(implicit executionContext: ExecutionContext): HttpService = HttpService {
    case GET -> Root => Ok("Server is up")
    case req@ POST -> Root / "new" => Ok(req.body.map(x => toError(saveArticle(x))))
    case GET -> Root / "publishAll" => Ok(toError(publishAllArticles))
    case GET -> Root / "index" => Ok(toError(publishIndex))
  }

}
