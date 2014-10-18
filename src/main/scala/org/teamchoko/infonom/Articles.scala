package org.teamchoko.infonom

import java.net.URI
import org.joda.time.DateTime

sealed abstract class TextFilter
case class Textile() extends TextFilter
case class Html() extends TextFilter

case class Author(name: String, email: Option[String], uri: Option[URI])
case class Category(name: String, uri: URI)
case class Comment(author: Author, text: String, pubDate: DateTime)
case class Article(heading: String,
                   text: String,
                   textFilter: TextFilter,
                   extract: Option[String],
                   pullquote: Option[String],
                   comments: Seq[Comment],
                   categories: Seq[Category],
                   pubDate: DateTime,
                   author: Author,
                   uri: URI)

case class RssFeed(folder: String, htmlurl: URI, xmlurl: URI, title: String, text: String, lastUpdate: DateTime)

