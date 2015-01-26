package org.teamchoko.infonom.tomato

import java.io.File
import java.io.FileWriter

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder
import org.teamchoko.infonom.carrot.Articles.Article
import org.teamchoko.infonom.carrot.Articles.CompleteArticle
import org.teamchoko.infonom.tomato.Errors.StringError
import org.teamchoko.infonom.tomato.Errors.checkTrue
import org.teamchoko.infonom.tomato.Errors.extractErrors
import org.teamchoko.infonom.tomato.Errors.success

object SaveToFile {

  val formatter = new DateTimeFormatterBuilder().appendYear(4, 4).appendLiteral('/').appendMonthOfYear(2)
    .appendLiteral('/').appendDayOfMonth(2).toFormatter()

  def formatDate(pubDate: DateTime): String = formatter.print(pubDate)

  def getDirectory(pubDate: DateTime): StringError[File] = extractErrors(new File(formatDate(pubDate)))
  
  def createDirectory(file: File): StringError[Unit] =
    extractErrors(if (file.exists()) file.mkdirs() else true).flatMap(x => checkTrue(x, "Could not create directory"))

  def createFile(parent: File, article: Article): StringError[File] = for {
    _ <- checkTrue(parent.exists() && parent.isDirectory(), "Invalid parent")
    file <- extractErrors(new File(parent, article.uri.toASCIIString()))
  } yield (file)
  
  def saveToFileIfNew(file: File, article: CompleteArticle): StringError[Unit] =
    if (file.exists()) {
      success()
    } else for {
      writer <- extractErrors(new FileWriter(file))
      articleString = ArticleRenderer.render(article)
      _ <- extractErrors(writer.write(articleString))
      _ <- extractErrors(writer.close())
    } yield ()
	
  def saveToFile(article: CompleteArticle): StringError[Unit] = for {
    folder <- getDirectory(article.article.pubDate)
    _ <- createDirectory(folder)
    file <- createFile(folder, article.article)
    _ <- saveToFileIfNew(file, article)
  } yield () 

}
