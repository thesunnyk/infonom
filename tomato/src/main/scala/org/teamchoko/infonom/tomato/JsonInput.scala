package org.teamchoko.infonom.tomato

import argonaut._
import argonaut.Argonaut._
import java.io.File
import java.io.FileReader
import java.net.URI
import org.slf4j.LoggerFactory
import org.teamchoko.infonom.carrot.Articles.Article
import org.teamchoko.infonom.carrot.Articles.Author
import org.teamchoko.infonom.carrot.Articles.Category
import org.teamchoko.infonom.carrot.Articles.CompleteArticle
import org.teamchoko.infonom.carrot.Articles.CompleteArticleCase
import org.teamchoko.infonom.carrot.Errors._
import org.teamchoko.infonom.carrot.JsonArticles._
import scala.io.Source
import scalaz.syntax.traverse._
import scalaz.std.list._

object JsonInput {

  val log = LoggerFactory.getLogger("JsonInput")

  def input(): StringError[List[CompleteArticleCase]] = for {
    blogs <- extractErrors(new File("blogs"))
    _ = log.info("Reading blogs directory")
    files <- extractErrors(blogs.listFiles.toList)
    articles <- files.traverse(readFile)
    _ = log.info("got files")
  } yield articles

  def readFile(file: File): StringError[CompleteArticleCase] = for {
    artString <- extractErrors(Source.fromFile(file).mkString)
    article <- artString.decodeEither[CompleteArticleCase]
    _ = log.info("Got file {}", file)
  } yield article

}
