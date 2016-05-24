package org.teamchoko.infonom.carrot

import java.net.URI
import org.joda.time.DateTime

object Articles {
  sealed trait TextFilter
  case object Textile extends TextFilter
  case object Html extends TextFilter

  sealed trait ArticleChunk
  case class TextileText(text: String) extends ArticleChunk
  case class HtmlText(text: String) extends ArticleChunk
  case class PullQuote(text: String) extends ArticleChunk

  case class Author(name: String, email: Option[String], uri: Option[URI])
  case class Category(name: String, uri: URI)
  case class Comment(text: String, pubDate: DateTime)

  case class Article(heading: String,
                     content: List[ArticleChunk],
                     extract: Option[String],
                     pubDate: DateTime,
                     uri: URI)

  trait CompleteComment
  {
    val comment: Comment
    val author: Author
  }
  
  case class CompleteCommentCase(comment: Comment, author: Author) extends CompleteComment

  trait CompleteArticle
  {
    val article: Article
    val comments: List[CompleteComment]
    val categories: List[Category]
    val author: Author
  }
  
  case class CompleteArticleCase(article: Article, comments: List[CompleteCommentCase], categories: List[Category],
    author: Author) extends CompleteArticle

  case class RssFeed(folder: String, htmlurl: URI, xmlurl: URI, title: String, text: String, lastUpdate: DateTime)
}

