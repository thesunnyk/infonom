package org.teamchoko.infonom.tomato

import org.clapper.markwrap.MarkWrap
import org.clapper.markwrap.MarkupType
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.teamchoko.infonom.carrot.Articles.Article
import org.teamchoko.infonom.carrot.Articles.Author
import org.teamchoko.infonom.carrot.Articles.Category
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
import scalatags.Text.all.h2
import scalatags.Text.all.h3
import scalatags.Text.all.ul
import scalatags.Text.all.li
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
import scalatags.Text.all.xmlns
import scalatags.Text.tags2.title

import Atom.feed
import Atom.author
import Atom.entry
import Atom.id
import Atom.updated
import Atom.category
import Atom.scheme
import Atom.summary
import Atom.uri
import Atom.term

import java.net.URI

object ArticleRenderer {
  val siteName: String = "The USS Quad Damage"
  val categoriesString: String = "Categories"
  val authorsString: String = "Authors"

  val xmlhead = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"

  val authorUri = new URI("/authors/")
  val catUri = new URI("/categories/")

  def appendHtml(uri: URI): URI = uri.resolve(uri.getPath + ".html")

  def dateUri(pubDate: DateTime): URI = new URI("/" + SaveToFile.formatDate(pubDate) + "/")

  def getFirstArticlePubDate(items: List[CompleteArticle]): String =
    items.headOption.map(x => x.article.pubDate.toString).getOrElse(new DateTime(0).toString)

  def renderCategoryAtom(cat: Category, items: List[CompleteArticle], absUrl: URI): String =
    renderAtomList(items, absUrl, appendHtml(cat.uri), cat.name + ": " + siteName)

  def renderAuthorAtom(author: Author, items: List[CompleteArticle], absUrl: URI): String =
    renderAtomList(items, absUrl, author.uri.map(appendHtml).getOrElse(new URI("/")),
      author.name + ": " + siteName)

  def renderIndexAtom(items: List[CompleteArticle], absUrl: URI): String =
    renderAtomList(items, absUrl, new URI("/"), siteName)

  def renderAtomList(items: List[CompleteArticle], absurl: URI, typeurl: URI, name: String): String =
    xmlhead + feed(xmlns := "http://www.w3.org/2005/Atom", title(name),
      id(typeurl.toString), link(href := absurl.resolve(typeurl).toString),
      updated(getFirstArticlePubDate(items)),
      items.map(x => renderAtomEntry(x, absurl)))

  def renderAtomEntry(item: CompleteArticle, absUrl: URI): Modifier =
    entry(title(item.article.heading), link(href := appendHtml(
      absUrl.resolve(dateUri(item.article.pubDate)).resolve(item.article.uri)).toString),
      updated(item.article.pubDate.toString), author(Atom.name(item.author.name),
        item.author.uri.toList.map(x => uri(appendHtml(absUrl.resolve(x)).toString))),
      id(appendHtml(dateUri(item.article.pubDate).resolve(item.article.uri)).toString),
      item.article.extract.toList.map(x => summary(x)),
      Atom.content(renderArticleText(item.article)),
      item.categories.map(x => category(term := x.name, scheme := absUrl.resolve(x.uri).toString)))

  // TODO render footer with palette attribution, categories, authors, etc.

  def renderIndex(items: List[CompleteArticle]): String =
    docType + html(renderHead(siteName), body(h1(siteName), items.map(item =>
        div(`class` := "h-entry", renderEntryWithPermalink(item))
      )))

  def renderCategories(items: Map[Category, List[CompleteArticle]]): String =
    docType + html(renderHead(categoriesString + ": " + siteName),
        body(h1(siteName), h2(categoriesString), items.toList.flatMap(item =>
            List(renderCategoryHeading(item._1), renderArticleList(item._2))
        ))
      )

  def renderAuthors(items: Map[Author, List[CompleteArticle]]): String =
    docType + html(renderHead(authorsString + ": " + siteName),
        body(h1(siteName), h2(authorsString), items.toList.flatMap(item =>
            List(renderAuthorHeading(item._1), renderArticleList(item._2))
        ))
      )

  def renderCategory(cat: Category, articles: List[CompleteArticle]): String =
    docType + html(renderHead(cat.name + " :: " + categoriesString + ": " + siteName), body(
        h1(cat.name), renderArticleList(articles)
      ))

  def renderAuthor(author: Author, articles: List[CompleteArticle]): String =
    docType + html(renderHead(author.name + " :: " + authorsString + ": " + siteName), body(
        h1(author.name), renderArticleList(articles)
      ))

  def renderArticleList(articles: List[CompleteArticle]) =
    ul(articles.map(art => li(`class` := "h-entry", renderArticleHeaderForList(art))))

  def renderAuthorHeading(author: Author): Modifier =
    h3(author.uri.map(x => a(href := appendHtml(authorUri.resolve(x)).toString, author.name)).getOrElse(author.name))

  def renderCategoryHeading(cat: Category): Modifier = h3(a(href := appendHtml(catUri.resolve(cat.uri)).toString,
    cat.name))
  
  def renderArticleText(article: Article): String = article.textFilter match {
    case Textile => MarkWrap.parserFor(MarkupType.Textile).parseToHTML(article.text);
    case Html => article.text
  }

  def renderDate(date: DateTime): String = DateTimeFormat.forPattern("dd MMM yyyy").print(date)

  def renderArticleHeaderForList(articleInfo: CompleteArticle): List[Modifier] =
    p(`class` := "p-name", a(href := appendHtml(
      dateUri(articleInfo.article.pubDate).resolve(articleInfo.article.uri)).toString,
      articleInfo.article.heading)) :: renderEntryHeader(articleInfo)

  def renderEntryHeader(articleInfo: CompleteArticle): List[Modifier] = {
    val article = articleInfo.article
     List(
      article.extract.map(extract => p(`class` := "extract, p-summary")(extract)).getOrElse(""),
	  p(`class` := "byline", "by ", articleInfo.author.uri.map {
        uri => a(`class`:= "p-author", href := appendHtml(authorUri.resolve(uri)).toString,
          (articleInfo.author.name))
	  }.getOrElse(span(`class`:= "p-author")(articleInfo.author.name)),
	  " on ", span(`class` := "dt-published", renderDate(article.pubDate)))
	)
  }
  
  def renderPullquote(article: Article): Modifier =
    article.pullquote.map(x => span(`class` := "pullquote", x)).getOrElse("")

  def renderEntryBody(article: Article): List[Modifier] =
    List(div(`class` := "e-content", renderPullquote(article), raw(renderArticleText(article))))
  
  def renderEntryWithPermalink(articleInfo: CompleteArticle): List[Modifier] =
    a(href := appendHtml(dateUri(articleInfo.article.pubDate).resolve(articleInfo.article.uri)).toString,
      h2(`class` := "p-name", articleInfo.article.heading)) ::
    renderEntryHeader(articleInfo) ::: renderEntryBody(articleInfo.article)

  def renderEntry(articleInfo: CompleteArticle): List[Modifier] =
    h2(`class` := "p-name", articleInfo.article.heading) ::
    renderEntryHeader(articleInfo) ::: renderEntryBody(articleInfo.article)
  
  def renderMeta = List(meta(charset := "utf-8"),
      meta(httpEquiv := "X-UA-Compatible", content := "IE=edge,chrome=1"),
      meta(name := "description", content := ""),
      meta(name := "viewport", content := "width=device-width")) 
  
  def renderStylesheets = List(link(rel := "stylesheet", href := "/css/normalize.css"),
      link(rel := "stylesheet", href := "/css/main.css"))
      
  def renderHead(heading: String) = head(title(heading), renderMeta, renderStylesheets)

  val docType: String = "<!DOCTYPE html>"

  def render(articleInfo: CompleteArticle): String = {
    val article = articleInfo.article
    docType + html(renderHead(article.heading + ": " + siteName), body(
      h1(siteName),
        div(`class` := "h-entry", renderEntry(articleInfo))
    ))
  }

}
