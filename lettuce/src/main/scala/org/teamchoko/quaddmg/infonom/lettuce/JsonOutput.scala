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

  def getDirectory(uri: URI): StringError[File] = extractErrors(new File(uri).getParentFile())
  
  def createDirectory(file: File): StringError[Unit] =
    extractErrors(if (!file.exists()) file.mkdirs() else true).flatMap(x => checkTrue(x, "Could not create directory"))

  def output(ca: CompleteArticleCase): StringError[Unit] = for {
    blogs <- extractErrors(new File("blogs"))
    _ = log.info("created blogs")
    _ <- createDirectory(blogs)
    _ = log.info("created dir")
    file <- extractErrors(new File("blogs/" + ca.id + ".json"))
    _ = log.info("created file")
    writer <- extractErrors(new FileWriter(file))
    _ = log.info("created writer")
    _ <- extractErrors(writer.write(ca.asJson.spaces2))
    _ <- extractErrors(writer.close())
  } yield ()

}
