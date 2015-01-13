package org.teamchoko.infonom.tomato

import java.io.File

import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.DateTime


import org.teamchoko.infonom.carrot.CompleteArticle

object SaveToFile {

  val formatter = new DateTimeFormatterBuilder().appendYear(4, 4).appendLiteral('/').appendMonthOfYear(2)
    .appendLiteral('/').appendDayOfMonth(2).toFormatter()

  def formatDate(pubDate: DateTime): String = formatter.print(pubDate)

  def getFolder(pubDate: DateTime): File = ???

  def saveToFile(article: CompleteArticle): Unit = ArticleRenderer.render(article)

}
