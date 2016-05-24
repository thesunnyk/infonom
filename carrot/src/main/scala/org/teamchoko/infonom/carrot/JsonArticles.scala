package org.teamchoko.infonom.carrot

import argonaut.Argonaut.BooleanDecodeJson
import argonaut.Argonaut.BooleanEncodeJson
import argonaut.Argonaut.CanBuildFromDecodeJson
import argonaut.Argonaut.casecodec2
import argonaut.Argonaut.casecodec3
import argonaut.Argonaut.casecodec4
import argonaut.Argonaut.casecodec5
import argonaut.Argonaut.jString
import argonaut.Argonaut.OptionDecodeJson
import argonaut.Argonaut.OptionEncodeJson
import argonaut.Argonaut.StringDecodeJson
import argonaut.Argonaut.StringEncodeJson
import argonaut.Argonaut.TraversableOnceEncodeJson
import argonaut.CodecJson
import argonaut.Json
import argonaut.JsonObject
import java.net.URI
import org.joda.time.DateTime
import org.teamchoko.infonom.carrot.Articles.Article
import org.teamchoko.infonom.carrot.Articles.ArticleChunk
import org.teamchoko.infonom.carrot.Articles.Author
import org.teamchoko.infonom.carrot.Articles.Category
import org.teamchoko.infonom.carrot.Articles.Comment
import org.teamchoko.infonom.carrot.Articles.CompleteArticleCase
import org.teamchoko.infonom.carrot.Articles.CompleteCommentCase
import org.teamchoko.infonom.carrot.Articles.HtmlText
import org.teamchoko.infonom.carrot.Articles.PullQuote
import org.teamchoko.infonom.carrot.Articles.TextileText

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

  implicit val ArticleChunkCodecJson: CodecJson[ArticleChunk] = CodecJson(
      filter => filter match {
        case HtmlText(text) => Json(("type", jString("htmltext")), ("text", jString(text)))
        case TextileText(text) => Json(("type", jString("textiletext")), ("text", jString(text)))
        case PullQuote(text) => Json(("type", jString("pullquote")), ("text", jString(text)))
      },
      js => for {
        item <- (js --\ "type").as[String]
        lower = item.toLowerCase
        filter <- lower match {
          case x if x == "htmltext" => (js --\ "text").as[String].map(HtmlText(_))
          case x if x == "textiletext" => (js --\ "text").as[String].map(TextileText(_))
          case x if x == "pullquote" => (js --\ "text").as[String].map(PullQuote(_))
        }
      } yield filter
    )

  implicit def AuthorCodecJson = casecodec3(Author.apply, Author.unapply)("name", "email", "uri")

  implicit def CategoryCodecJson = casecodec2(Category.apply, Category.unapply)("name", "uri")

  implicit def CommentCodecJson = casecodec2(Comment.apply, Comment.unapply)("text", "pubdate")

  implicit def ArticleCodecJson = casecodec5(Article.apply, Article.unapply)("heading",
      "text", "extract", "pubDate", "uri")

  implicit def CompleteCommentCodecJson =
    casecodec2(CompleteCommentCase.apply, CompleteCommentCase.unapply)("comment", "author")

  implicit def CompleteArticleCodecJson =
    casecodec4(CompleteArticleCase.apply, CompleteArticleCase.unapply)("article", "comments", "categories", "author")

}
