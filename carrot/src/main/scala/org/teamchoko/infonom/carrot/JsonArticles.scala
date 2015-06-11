package org.teamchoko.infonom.carrot

import java.net.URI

import org.joda.time.DateTime
import org.teamchoko.infonom.carrot.Articles.Article
import org.teamchoko.infonom.carrot.Articles.Author
import org.teamchoko.infonom.carrot.Articles.Category
import org.teamchoko.infonom.carrot.Articles.Comment
import org.teamchoko.infonom.carrot.Articles.CompleteArticleCase
import org.teamchoko.infonom.carrot.Articles.CompleteCommentCase
import org.teamchoko.infonom.carrot.Articles.Html
import org.teamchoko.infonom.carrot.Articles.TextFilter
import org.teamchoko.infonom.carrot.Articles.Textile

import argonaut.Argonaut.BooleanDecodeJson
import argonaut.Argonaut.BooleanEncodeJson
import argonaut.Argonaut.CanBuildFromDecodeJson
import argonaut.Argonaut.OptionDecodeJson
import argonaut.Argonaut.OptionEncodeJson
import argonaut.Argonaut.StringDecodeJson
import argonaut.Argonaut.StringEncodeJson
import argonaut.Argonaut.TraversableOnceEncodeJson
import argonaut.Argonaut.casecodec2
import argonaut.Argonaut.casecodec3
import argonaut.Argonaut.casecodec4
import argonaut.Argonaut.casecodec8
import argonaut.Argonaut.jString
import argonaut.CodecJson

object JsonArticles {

  implicit val UriCodecJson: CodecJson[URI] = CodecJson(
    uri => jString(uri.toASCIIString),
    js => for {
        item <- js.as[String]
      } yield new URI(item)
  )
  
  implicit val DateTimeCodecJson: CodecJson[DateTime] = CodecJson(
    dateTime => jString(dateTime.toString),
    js => for {
      item <- js.as[String]
    } yield DateTime.parse(item)
  )

  implicit val TextFilterCodecJson: CodecJson[TextFilter] = CodecJson(
      filter => jString(filter match {
        case Html => "html"
        case Textile => "textile"
      }),
      js => for {
        item <- js.as[String]
        lower = item.toLowerCase
        filter = lower match {
          case x if x == "html" => Html
          case x if x == "textile" => Textile
        }
      } yield filter
    )

  implicit def AuthorCodecJson = casecodec3(Author.apply, Author.unapply)("name", "email", "uri")

  implicit def CategoryCodecJson = casecodec2(Category.apply, Category.unapply)("name", "uri")

  implicit def CommentCodecJson = casecodec2(Comment.apply, Comment.unapply)("text", "pubdate")

  implicit def ArticleCodecJson = casecodec8(Article.apply, Article.unapply)("heading",
      "text", "textFilter", "draft", "extract", "pullquote", "pubDate", "uri")

  implicit def CompleteCommentCodecJson =
    casecodec2(CompleteCommentCase.apply, CompleteCommentCase.unapply)("comment", "author")

  implicit def CompleteArticleCodecJson =
    casecodec4(CompleteArticleCase.apply, CompleteArticleCase.unapply)("article", "comments", "categories", "author")

}
