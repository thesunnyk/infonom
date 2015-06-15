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
import org.teamchoko.infonom.carrot.Articles.Category
import org.teamchoko.infonom.carrot.Articles.Author
import org.teamchoko.infonom.carrot.JsonArticles.CompleteArticleCodecJson
import org.teamchoko.infonom.tomato.Errors.StringError
import argonaut.Argonaut.StringToParseWrap
import scalaz._
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

import java.net.URI

trait MyService {
  val log = LoggerFactory.getLogger(classOf[MyService]);

  val quaddmg = new URI("http://blog.quaddmg.com")
  
  def saveArticle(body: ByteVector): StringError[Unit] = for {
    article: CompleteArticleCase <- new String(body.toArray).decodeEither[CompleteArticleCase]
    _ = log.info("Got an article: {}", article.article.heading)
    _ <- DbConn.persistCompleteArticle(article).transact(DbConn.xa).attemptRun.leftMap(x => x.getMessage)
    _ = log.info("Saved")
  } yield ()

  def getAllArticles(): Task[List[CompleteArticleCase]] = DbConn.getAllCompleteArticles.transact(DbConn.xa)

  def saveAllFiles(articles: List[CompleteArticleCase]): StringError[Unit] =
    articles.map(SaveToFile.saveToFile).fold(().point[StringError])((x, y) => y)

  def publishAllArticles(): StringError[Unit] = for {
    articles <- getAllArticles.attemptRun.leftMap(x => x.getMessage)
    _ = log.info("Publishing {} articles", articles.length)
    save <- saveAllFiles(articles)
  } yield ()

  def extractCategories(articles: List[CompleteArticleCase]): List[Category] =
    articles.flatMap(article => article.categories).toSet.toList

  def mapCategories(cats: List[Category],
    articles: List[CompleteArticleCase]): Map[Category, List[CompleteArticleCase]] =
      cats.map(cat => (cat, articles.filter(art => art.categories.contains(cat)))).toMap

  def extractAuthors(articles: List[CompleteArticleCase]): Map[Author, List[CompleteArticleCase]] =
    articles.groupBy(article => article.author)

  // TODO sequence?
  def saveEachCategory(mappedCategories: Map[Category, List[CompleteArticleCase]]): StringError[Unit] =
    mappedCategories.map(x => SaveToFile.saveCategory(x._1, x._2)).fold(\/-())((x, y) => x match {
      case -\/(_) => x
      case _ => y
    })

  def saveEachCategoryAtom(mappedCategories: Map[Category, List[CompleteArticleCase]]): StringError[Unit] =
    mappedCategories.map(x => SaveToFile.saveCategoryAtom(quaddmg, x._1, x._2)).fold(\/-())((x, y) => x match {
      case -\/(_) => x
      case _ => y
    })

  def saveEachAuthor(mappedAuthors: Map[Author, List[CompleteArticleCase]]): StringError[Unit] =
    mappedAuthors.map(x => SaveToFile.saveAuthor(x._1, x._2)).fold(\/-())((x, y) => x match {
      case -\/(_) => x
      case _ => y
    })

  def saveEachAuthorAtom(mappedAuthors: Map[Author, List[CompleteArticleCase]]): StringError[Unit] =
    mappedAuthors.map(x => SaveToFile.saveAuthorAtom(quaddmg, x._1, x._2)).fold(\/-())((x, y) => x match {
      case -\/(_) => x
      case _ => y
    })

  def publishIndex(): StringError[Unit] = for {
    articles <- getAllArticles.attemptRun.leftMap(x => x.getMessage)
    _ <- SaveToFile.saveIndex(articles.take(10))
    _ <- SaveToFile.saveIndexAtom(quaddmg, articles.take(10))
    categories = extractCategories(articles)
    _ = log.info("Got categories: {}", categories)
    mappedCategories = mapCategories(categories, articles)
    _ <- SaveToFile.saveCategories(mappedCategories)
    _ <- saveEachCategory(mappedCategories)
    _ <- saveEachCategoryAtom(mappedCategories)
    mappedAuthors = extractAuthors(articles)
    _ <- SaveToFile.saveAuthors(mappedAuthors)
    _ <- saveEachAuthor(mappedAuthors)
    _ <- saveEachAuthorAtom(mappedAuthors)
  } yield ()
  
  def toError(err: StringError[Unit]): String = err.fold(err => err, succ => "Success")

  def makeString(body: ByteVector): String = new String(body.toArray)
  
  def service(implicit executionContext: ExecutionContext): HttpService = HttpService {
    case GET -> Root => Ok("Server is up")
    case req@ POST -> Root / "new" => Ok(req.body.chunkAll.map(x => {
      val vec = x.fold(ByteVector.empty)((x, y) => x ++ y)
      toError(saveArticle(vec))
    }))
    case GET -> Root / "publishAll" => Ok(toError(publishAllArticles))
    case GET -> Root / "index" => Ok(toError(publishIndex))
  }

}
