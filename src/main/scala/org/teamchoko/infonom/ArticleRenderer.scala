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
  
  def renderHeader(articleInfo: CompleteArticle): List[Modifier] = {
    val article = articleInfo.article
     List(
      article.extract.map(extract => p(`class` := "extract, p-summary")(extract)).getOrElse(""),
	  p("by", articleInfo.author.uri.map {
        uri => a(`class`:= "p-author", href := uri.toString, (articleInfo.author.name))
	  }.getOrElse(span(`class`:= "p-author")(articleInfo.author.name)),
	  "on", span(`class` := "dt-published", renderDate(article.pubDate)))
	)
  }
  
  def renderPullquote(article: Article): Modifier =
    article.pullquote.map(x => span(`class` := "pullquote", x)).getOrElse("")
  
  def renderEntry(articleInfo: CompleteArticle): List[Modifier] = {
    h1(`class` := "p-name", articleInfo.article.heading) ::
    renderHeader(articleInfo) :::
    List(div(`class` := "e-content", renderPullquote(articleInfo.article),
        raw(renderArticleText(articleInfo.article))))
  }

  def render(articleInfo: CompleteArticle) = {
    val article = articleInfo.article
    "<!DOCTYPE html>" + html(head(title(article.heading)), body(
        div(`class` := "h-entry", renderEntry(articleInfo))
    ))
  }

}
