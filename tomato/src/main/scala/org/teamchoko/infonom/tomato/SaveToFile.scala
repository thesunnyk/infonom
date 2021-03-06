package org.teamchoko.infonom.tomato

import java.io.File
import java.io.FileWriter
import java.net.URI
import org.slf4j.LoggerFactory
import org.teamchoko.infonom.carrot.Articles.Article
import org.teamchoko.infonom.carrot.Articles.Author
import org.teamchoko.infonom.carrot.Articles.Category
import org.teamchoko.infonom.carrot.Articles.CompleteArticle
import org.teamchoko.infonom.carrot.Errors.checkTrue
import org.teamchoko.infonom.carrot.Errors.extractErrors
import org.teamchoko.infonom.carrot.Errors.StringError
import org.teamchoko.infonom.carrot.Errors.success
import org.teamchoko.infonom.tomato.render.ArticleRenderer

object SaveToFile {
  
  val log = LoggerFactory.getLogger("SaveToFile")

  def getDirectory(uri: URI): StringError[File] = extractErrors(new File(uri.toString).getParentFile())
  
  def createDirectory(file: File): StringError[Unit] =
    extractErrors(if (!file.exists()) file.mkdirs() else true).flatMap(x => checkTrue(x, "Could not create directory"))

  def createFile(uri: URI): StringError[File] = for {
    file <- extractErrors(new File(uri.toString))
  } yield (file)
  
  def saveToSpecificFile(file: File, article: CompleteArticle): StringError[Unit] =
    for {
      writer <- extractErrors(new FileWriter(file))
      articleString = ArticleRenderer.render(article)
      _ <- extractErrors(writer.write(articleString))
      _ <- extractErrors(writer.close())
    } yield ()
	
  def saveToFile(article: CompleteArticle): StringError[Unit] = for {
    folder <- getDirectory(article.article.uri)
    _ <- createDirectory(folder)
    file <- createFile(article.article.uri)
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
    file <- extractErrors(new File(dir, author.uri.get.toASCIIString + ".html"))
    _ <- extractErrors(dir.mkdirs())
    writer <- extractErrors(new FileWriter(file))
    rendered = ArticleRenderer.renderAuthor(author, articles)
    _ <- extractErrors(writer.write(rendered))
    _ <- extractErrors(writer.close())
  } yield ()
  
  def saveAuthorAtom(absUrl: URI, author: Author, articles: List[CompleteArticle]) = for {
    dir <- extractErrors(new File("authors"))
    file <- extractErrors(new File(dir, author.uri.get.toASCIIString + ".atom"))
    _ <- extractErrors(dir.mkdirs())
    writer <- extractErrors(new FileWriter(file))
    rendered = ArticleRenderer.renderAuthorAtom(author, articles, absUrl)
    _ <- extractErrors(writer.write(rendered))
    _ <- extractErrors(writer.close())
  } yield ()
}
