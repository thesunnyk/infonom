package org.teamchoko.infonom

import scalatags.Text.all._
import scalatags.Text.tags2.title
import org.clapper.markwrap.{MarkWrap, MarkupType}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object ArticleRenderer {
  
  def renderArticleText(article: Article): String = article.textFilter match {
    case Textile() => MarkWrap.parserFor(MarkupType.Textile).parseToHTML(article.text);
    case Html() => article.text
  }

  def renderDate(date: DateTime): String = DateTimeFormat.forPattern("dd MMM yyyy").print(date)

  def render(articleInfo: CompleteArticle) = {
    val article = articleInfo.article
    "<!DOCTYPE html>" + html(head(title(article.heading)), body(
	  h1(article.heading),
	  article.extract.map(extract => p(extract)).getOrElse(""),
	  p("by", articleInfo.author.uri.map(uri => a(href := uri.toString)((articleInfo.author.name))).getOrElse(span(
	      articleInfo.author.name)), "on", span(renderDate(article.pubDate))),
      div(article.pullquote.map(x => span(x)).getOrElse(""), raw(renderArticleText(article)))
    ))
  }

}
