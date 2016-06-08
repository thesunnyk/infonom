package org.teamchoko.infonom.lettuce

import argonaut._
import argonaut.Argonaut._
import java.io.File
import java.io.FileWriter
import java.net.URI
import org.slf4j.LoggerFactory
import org.teamchoko.infonom.carrot.Articles.Article
import org.teamchoko.infonom.carrot.Articles.Author
import org.teamchoko.infonom.carrot.Articles.Category
import org.teamchoko.infonom.carrot.Articles.CompleteArticle
import org.teamchoko.infonom.carrot.Articles.CompleteArticleCase
import org.teamchoko.infonom.carrot.Errors._
import org.teamchoko.infonom.carrot.JsonArticles._

object JsonOutput {

  val log = LoggerFactory.getLogger("JsonOutput")

  def createDirectory(file: File): StringError[Unit] =
    extractErrors(if (!file.exists()) file.mkdirs() else true).flatMap(x => checkTrue(x, "Could not create directory"))

  def newFile(str: String, suffix: String): StringError[File] = for {
    file <- extractErrors(new File(str))
    _ <- checkTrue(!file.exists(), s"File ${file} already exists")
  } yield file

  def output(ca: CompleteArticleCase): StringError[Unit] = for {
    blogs <- extractErrors(new File("blogs/" + ca.article.pubDate.getYear))
    _ <- createDirectory(blogs)
    file <- newFile("blogs/" + ca.article.pubDate.getYear + "/" + ca.id + ".json")
    writer <- extractErrors(new FileWriter(file))
    _ <- extractErrors(writer.write(ca.asJson.spaces2))
    _ <- extractErrors(writer.close())
    _ = file.setLastModified(ca.article.pubDate.getMillis)
  } yield ()

}
