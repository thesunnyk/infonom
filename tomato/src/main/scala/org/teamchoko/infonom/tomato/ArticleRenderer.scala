package org.teamchoko.infonom.tomato

import org.clapper.markwrap.MarkWrap
import org.clapper.markwrap.MarkupType
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.teamchoko.infonom.carrot.Articles.Article
import org.teamchoko.infonom.carrot.Articles.CompleteArticle
import org.teamchoko.infonom.carrot.Articles.Html
import org.teamchoko.infonom.carrot.Articles.Textile

import scalatags.Text.all.Modifier
import scalatags.Text.all.SeqNode
import scalatags.Text.all.a
import scalatags.Text.all.body
import scalatags.Text.all.charset
import scalatags.Text.all.`class`
import scalatags.Text.all.div
import scalatags.Text.all.h1
import scalatags.Text.all.head
import scalatags.Text.all.href
import scalatags.Text.all.html
import scalatags.Text.all.link
import scalatags.Text.all.meta
import scalatags.Text.all.name
import scalatags.Text.all.content
import scalatags.Text.all.httpEquiv
import scalatags.Text.all.rel
import scalatags.Text.all.p
import scalatags.Text.all.raw
import scalatags.Text.all.span
import scalatags.Text.all.stringAttr
import scalatags.Text.all.stringFrag
import scalatags.Text.tags2.title

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
	  p("by ", articleInfo.author.uri.map {
        uri => a(`class`:= "p-author", href := uri.toString, (articleInfo.author.name))
	  }.getOrElse(span(`class`:= "p-author")(articleInfo.author.name)),
	  " on ", span(`class` := "dt-published", renderDate(article.pubDate)))
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
  
  def renderMeta = List(meta(charset := "utf-8"),
      meta(httpEquiv := "X-UA-Compatible", content := "IE=edge,chrome=1"),
      meta(name := "description", content := ""),
      meta(name := "viewport", content := "width=device-width")) 
  
  def renderStylesheets = List(link(rel := "stylesheet", href := "/css/normalize.css"),
      link(rel := "stylesheet", href := "/css/main.css"))
      
  def renderHead(article: Article) = head(title(article.heading) :: renderMeta ::: renderStylesheets)

  def render(articleInfo: CompleteArticle): String = {
    val article = articleInfo.article
    "<!DOCTYPE html>" + html(renderHead(article), body(
        div(`class` := "h-entry", renderEntry(articleInfo))
    ))
  }

}
