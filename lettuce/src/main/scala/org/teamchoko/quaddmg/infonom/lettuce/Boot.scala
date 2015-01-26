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
import scala.xml.Elem
import java.net.URI
import scala.xml.Node

object Boot extends App {
  
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
    case x if x.toLowerCase == "textile" => Some(Textile())
    case x if x.toLowerCase == "html" => Some(Html())
    case _ => None
  }
  
  def authorFromId(id: String): Option[Author] = ???
  
  def categoriesFromIds(ids: List[String]): List[Category] = ???

  def articleFromXml(x: Node): Option[CompleteArticle] = for {
    id <- getId(x)
    date <- getItem(x, "@date")
    pubDate = new DateTime(date)
    uri <- getItem(x, "permalink")
    heading <- getItem(x, "heading")
    textFilterStr <- getItem(x, "textFilter")
    textFilter <- filterFromString(textFilterStr)
    text: String <- getItem(x, "text")
    comments: List[CompleteCommentCase] = (x \ "comments").toList.map(commentFromXml).flatten
    authorId <- (x \ "author").headOption
    authorIdStr <- getId(authorId)
    author: Author <- authorFromId(authorIdStr)
    categories: List[Category] = categoriesFromIds((x \ "categories").toList.map(getId).flatten)
    pullquote: Option[String] = getItem(x, "pullquote")
    extract: Option[String] = getItem(x, "extract")
    article: Article = Article(heading, text, textFilter, false, extract, pullquote, pubDate, new URI(uri))
  } yield CompleteArticleCase(article, comments, categories, author)


//  val xmlData = {
//    <xml>
//    <authors>{authors.map(makeXml)}</authors>
//    <categories>{categories.map(makeXml)}</categories>
//    <articles>
//      {entries.map(makeXml)}
//    </articles>
//    </xml>
//  }

}

