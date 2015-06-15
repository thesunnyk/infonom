package org.teamchoko.infonom.lettuce

import org.joda.time.DateTime
import org.teamchoko.infonom.carrot.Articles.Author
import org.teamchoko.infonom.carrot.Articles.Category
import org.teamchoko.infonom.carrot.Articles.CompleteArticle
import org.teamchoko.infonom.carrot.Articles.CompleteComment
import org.teamchoko.infonom.carrot.Articles.Html
import org.teamchoko.infonom.carrot.Articles.TextFilter
import org.teamchoko.infonom.carrot.Articles.Textile
import org.teamchoko.infonom.carrot.Articles.CompleteCommentCase
import org.teamchoko.infonom.carrot.Articles.Comment
import org.teamchoko.infonom.carrot.Articles.CompleteArticleCase
import org.teamchoko.infonom.carrot.Articles.Article
import org.teamchoko.infonom.carrot.JsonArticles._
import argonaut._
import argonaut.Argonaut._
import scala.xml.Elem
import java.net.URI
import scala.xml.Node
import java.io.File
import scala.xml.XML
import scalaj.http.Http
import org.slf4j.LoggerFactory

object Boot extends App {
  val log = LoggerFactory.getLogger(classOf[App])
  
  def toUri(s: String): URI = new URI(s.toLowerCase.replace(' ', '_'))
  
  def getItem(x: Node, str: String): Option[String] = (x \ str).headOption.map(f => f.text)
  
  def getId(x: Node): Option[String] = getItem(x, "@id")
  
  def authorFromXml(x: Node): Option[(String, Author)] = for {
    id <- getId(x)
    authName <- getItem(x, "name")
  } yield (id, Author(authName, getItem(x, "email"), Some(toUri(authName))))

  def commentAuthorFromXml(x: Node): Option[Author] = for {
    authName <- getItem(x, "name")
  } yield Author(authName, getItem(x, "email"), getItem(x, "url").map(x => new URI(x)))
  
  def categoryFromXml(x: Node): Option[(String, Category)] = for {
    id <- getId(x)
    name <- getItem(x, "name")
    uri <- getItem(x, "permalink")
  } yield (id, Category(name, toUri(name)))

  def commentFromXml(x: Node): Option[CompleteCommentCase] = for {
    date <- getItem(x, "@date")
    text <- getItem(x, "text")
    authXml <- (x \ "author").headOption
    author <- commentAuthorFromXml(authXml)
  } yield CompleteCommentCase(Comment(text, new DateTime(date)), author)

  def filterFromString(str: String): Option[TextFilter] = str match {
    case x if x.toLowerCase == "textile" => Some(Textile)
    case x if x.toLowerCase == "html" => Some(Html)
    case _ => {
      log.warn("Got unknown text filter: '{}'", str)
      None
    }
  }

  var allAuthors: Map[String, Author] = Map()

  var allCategories: Map[String, Category] = Map()
  
  def authorFromId(id: String): Option[Author] = allAuthors.get(id)
  
  def categoriesFromIds(ids: List[String]): List[Category] = ids.map(x => allCategories.get(x)).flatten

  def articleFromXml(x: Node): Option[CompleteArticleCase] = for {
    id <- getId(x)
    date <- getItem(x, "@date")
    pubDate = new DateTime(date)
    uri <- getItem(x, "permalink")
    heading <- getItem(x, "heading")
    textFilterNode <- (x \ "textFilter").headOption
    textFilterStr <- getItem(textFilterNode, "@type")
    textFilter <- filterFromString(textFilterStr)
    text: String <- getItem(x, "text")
    comments: List[CompleteCommentCase] = (x \ "comments").toList.map(commentFromXml).flatten
    authorId <- (x \ "author").headOption
    authorIdStr <- getId(authorId)
    author: Author <- authorFromId(authorIdStr)
    categories: List[Category] = categoriesFromIds((x \ "categories" \ "category").toList.map(getId).flatten)
    pullquote: Option[String] = getItem(x, "pullquote")
    extract: Option[String] = getItem(x, "extract")
    article: Article = Article(heading, text, textFilter, false, extract, pullquote, pubDate, new URI(uri))
  } yield CompleteArticleCase(article, comments, categories, author)
  
  def setAllAuthors(authors: List[(String, Author)]): Unit =
    authors.foreach(data => allAuthors = allAuthors ++ Map(data._1 -> data._2))

  def setAllCategories(categories: List[(String, Category)]): Unit =
    categories.foreach(data => allCategories = allCategories ++ Map(data._1 -> data._2))

  def fromCompleteXml(x: Node): List[CompleteArticleCase] = {
    val authors = x \ "authors" \ "author"
    log.info("Found {} authors", authors.size)
    setAllAuthors(authors.map(authorFromXml).flatten.toList)
    log.info("Found {} verified authors", allAuthors.size)
    val categories = x \ "categories" \ "category"
    log.info("Found {} categories", categories.size)
    setAllCategories(categories.map(categoryFromXml).flatten.toList)
    log.info("Found {} verified categories", allCategories.size)
    val articles = x \ "articles" \ "article"
    log.info("Found {} articles", articles.size)
    articles.map(articleFromXml).flatten.toList
  }

  val allArticles: List[CompleteArticleCase] = fromCompleteXml(XML.loadFile(new File(args(0))))
  val allJson: List[Json] = allArticles.map(_.asJson)

  log.info("Found {} verified articles", allArticles.size)
  allJson.map(x => {
    val response = Http("http://localhost:8080/new/").postData(x.spaces2).asString
    log.info("Posted and got {}", response.body)
  })
}

