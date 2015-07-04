package org.teamchoko.infonom.tomato

import java.io.File
import java.io.FileWriter
import java.net.URI
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder
import org.teamchoko.infonom.carrot.Articles.Article
import org.teamchoko.infonom.carrot.Articles.CompleteArticle
import org.teamchoko.infonom.carrot.Articles.Category
import org.teamchoko.infonom.carrot.Articles.Author
import org.teamchoko.infonom.tomato.Errors.StringError
import org.teamchoko.infonom.tomato.Errors.checkTrue
import org.teamchoko.infonom.tomato.Errors.extractErrors
import org.teamchoko.infonom.tomato.Errors.success
import org.slf4j.LoggerFactory

object SaveToFile {
  
  val log = LoggerFactory.getLogger("SaveToFile")

  val formatter = new DateTimeFormatterBuilder().appendYear(4, 4).appendLiteral('/').appendMonthOfYear(2)
    .appendLiteral('/').appendDayOfMonth(2).toFormatter()

  def formatDate(pubDate: DateTime): String = formatter.print(pubDate)

  def getDirectory(pubDate: DateTime): StringError[File] = extractErrors(new File(formatDate(pubDate)))
  
  def createDirectory(file: File): StringError[Unit] =
    extractErrors(if (!file.exists()) file.mkdirs() else true).flatMap(x => checkTrue(x, "Could not create directory"))

  def createFile(parent: File, article: Article): StringError[File] = for {
    _ <- checkTrue(parent.exists() && parent.isDirectory(), "Invalid parent")
    file <- extractErrors(new File(parent, article.uri.toASCIIString() + ".html"))
  } yield (file)
  
  def saveToSpecificFile(file: File, article: CompleteArticle): StringError[Unit] =
    for {
      writer <- extractErrors(new FileWriter(file))
      articleString = ArticleRenderer.render(article)
      _ <- extractErrors(writer.write(articleString))
      _ <- extractErrors(writer.close())
    } yield ()
	
  def saveToFile(article: CompleteArticle): StringError[Unit] = for {
    folder <- getDirectory(article.article.pubDate)
    _ <- createDirectory(folder)
    file <- createFile(folder, article.article)
    _ = log.info("Saving to file " + file)
    _ <- saveToSpecificFile(file, article)
  } yield () 

  def saveIndex(articles: List[CompleteArticle]): StringError[Unit] = for {
    file <- extractErrors(new File("index.html"))
    writer <- extractErrors(new FileWriter(file))
    rendered = ArticleRenderer.renderIndex(articles)
    _ <- extractErrors(writer.write(rendered))
    _ <- extractErrors(writer.close())
  } yield ()

  def saveIndexAtom(absUrl: URI, articles: List[CompleteArticle]): StringError[Unit] = for {
    file <- extractErrors(new File("index.atom"))
    writer <- extractErrors(new FileWriter(file))
    rendered = ArticleRenderer.renderIndexAtom(articles, absUrl)
    _ <- extractErrors(writer.write(rendered))
    _ <- extractErrors(writer.close())
  } yield ()


  def saveCategories(items: Map[Category, List[CompleteArticle]]): StringError[Unit] = for {
    dir <- extractErrors(new File("categories"))
    file <- extractErrors(new File(dir, "index.html"))
    _ <- extractErrors(dir.mkdirs())
    writer <- extractErrors(new FileWriter(file))
    rendered = ArticleRenderer.renderCategories(items)
    _ <- extractErrors(writer.write(rendered))
    _ <- extractErrors(writer.close())
  } yield ()

  def saveAuthors(items: Map[Author, List[CompleteArticle]]): StringError[Unit] = for {
    dir <- extractErrors(new File("authors"))
    file <- extractErrors(new File(dir, "index.html"))
    _ <- extractErrors(dir.mkdirs())
    writer <- extractErrors(new FileWriter(file))
    rendered = ArticleRenderer.renderAuthors(items)
    _ <- extractErrors(writer.write(rendered))
    _ <- extractErrors(writer.close())
  } yield ()

  def saveCategory(category: Category, articles: List[CompleteArticle]): StringError[Unit] = for {
    dir <- extractErrors(new File("categories"))
    file <- extractErrors(new File(dir, category.uri.toASCIIString() + ".html"))
    _ <- extractErrors(dir.mkdirs())
    writer <- extractErrors(new FileWriter(file))
    rendered = ArticleRenderer.renderCategory(category, articles)
    _ <- extractErrors(writer.write(rendered))
    _ <- extractErrors(writer.close())
  } yield ()

  def saveCategoryAtom(absUrl: URI, category: Category, articles: List[CompleteArticle]) = for {
    dir <- extractErrors(new File("categories"))
    file <- extractErrors(new File(dir, category.uri.toASCIIString() + ".atom"))
    _ <- extractErrors(dir.mkdirs())
    writer <- extractErrors(new FileWriter(file))
    rendered = ArticleRenderer.renderCategoryAtom(category, articles, absUrl)
    _ <- extractErrors(writer.write(rendered))
    _ <- extractErrors(writer.close())
  } yield ()

  def saveAuthor(author: Author, articles: List[CompleteArticle]) = for {
    dir <- extractErrors(new File("authors"))
    file <- extractErrors(new File(dir, author.uri.map(_.toASCIIString).getOrElse(author.name) + ".html"))
    _ <- extractErrors(dir.mkdirs())
    writer <- extractErrors(new FileWriter(file))
    rendered = ArticleRenderer.renderAuthor(author, articles)
    _ <- extractErrors(writer.write(rendered))
    _ <- extractErrors(writer.close())
  } yield ()
  
  def saveAuthorAtom(absUrl: URI, author: Author, articles: List[CompleteArticle]) = for {
    dir <- extractErrors(new File("authors"))
    file <- extractErrors(new File(dir, author.uri.map(_.toASCIIString).getOrElse(author.name) + ".atom"))
    _ <- extractErrors(dir.mkdirs())
    writer <- extractErrors(new FileWriter(file))
    rendered = ArticleRenderer.renderAuthorAtom(author, articles, absUrl)
    _ <- extractErrors(writer.write(rendered))
    _ <- extractErrors(writer.close())
  } yield ()
}
