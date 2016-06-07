package org.teamchoko.infonom.tomato

import argonaut.Argonaut.StringToParseWrap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.teamchoko.infonom.carrot.Articles.Author
import org.teamchoko.infonom.carrot.Articles.Category
import org.teamchoko.infonom.carrot.Articles.CompleteArticleCase
import org.teamchoko.infonom.carrot.Errors.StringError
import org.teamchoko.infonom.carrot.JsonArticles.CompleteArticleCodecJson
import scalaz._
import scalaz.Scalaz._

import java.net.URI

object Boot extends App {
  val log = LoggerFactory.getLogger("Boot");

  val quaddmg = new URI("https://blog.quaddmg.com")
  
  def saveAllFiles(articles: List[CompleteArticleCase]): StringError[Unit] =
    articles.map(SaveToFile.saveToFile).fold(().point[StringError])((x, y) => y)

  def publishAllArticles(): StringError[Unit] = for {
    articles <- maybeArticles
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
    articles <- maybeArticles
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

  val maybeArticles = JsonInput.input()

  val articlesResult = publishAllArticles()
  log.info("Published all with result {}", articlesResult)

  val indexResult = publishIndex()
  log.info("Published Index with result {}", indexResult)
}

