package org.teamchoko.infonom.tomato.render

import Atom.author
import Atom.category
import Atom.entry
import Atom.feed
import Atom.id
import Atom.scheme
import Atom.summary
import Atom.term
import Atom.updated
import Atom.uri
import java.net.URI
import org.clapper.markwrap.MarkupType
import org.clapper.markwrap.MarkWrap
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.teamchoko.infonom.carrot.Articles.Article
import org.teamchoko.infonom.carrot.Articles.ArticleChunk
import org.teamchoko.infonom.carrot.Articles.Author
import org.teamchoko.infonom.carrot.Articles.Category
import org.teamchoko.infonom.carrot.Articles.CompleteArticle
import org.teamchoko.infonom.carrot.Articles.HtmlText
import org.teamchoko.infonom.carrot.Articles.PullQuote
import org.teamchoko.infonom.carrot.Articles.TextileText
import scalatags.Text.all.`class`
import scalatags.Text.all.`type`
import scalatags.Text.all.a
import scalatags.Text.all.body
import scalatags.Text.all.charset
import scalatags.Text.all.content
import scalatags.Text.all.div
import scalatags.Text.all.footer
import scalatags.Text.all.h1
import scalatags.Text.all.h2
import scalatags.Text.all.h3
import scalatags.Text.all.head
import scalatags.Text.all.header
import scalatags.Text.all.href
import scalatags.Text.all.html
import scalatags.Text.all.httpEquiv
import scalatags.Text.all.lang
import scalatags.Text.all.li
import scalatags.Text.all.link
import scalatags.Text.all.meta
import scalatags.Text.all.Modifier
import scalatags.Text.all.name
import scalatags.Text.all.p
import scalatags.Text.all.raw
import scalatags.Text.all.rel
import scalatags.Text.all.SeqNode
import scalatags.Text.all.span
import scalatags.Text.all.stringAttr
import scalatags.Text.all.stringFrag
import scalatags.Text.all.ul
import scalatags.Text.all.xmlns
import scalatags.Text.tags2.section
import scalatags.Text.tags2.title

object ArticleRenderer {
  val siteName: String = "The USS Quad Damage"
  val categoriesString: String = "Categories"
  val authorsString: String = "Authors"

  val xmlhead = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"

  val authorUri = new URI("/authors/")
  val catUri = new URI("/categories/")

  def appendHtml(uri: URI): URI = uri.resolve(uri.getPath + ".html")

  def getFirstArticlePubDate(items: List[CompleteArticle]): String =
    items.headOption.map(x => x.article.pubDate.toString).getOrElse(new DateTime(0).toString)

  def renderCategoryAtom(cat: Category, items: List[CompleteArticle], absUrl: URI): String =
    renderAtomList(items, absUrl, appendHtml(cat.uri), cat.name + ": " + siteName)

  def renderAuthorAtom(author: Author, items: List[CompleteArticle], absUrl: URI): String =
    renderAtomList(items, absUrl, appendHtml(author.uri.get), author.name + ": " + siteName)

  def renderIndexAtom(items: List[CompleteArticle], absUrl: URI): String =
    renderAtomList(items, absUrl, new URI("/"), siteName)

  def renderAtomList(items: List[CompleteArticle], absurl: URI, typeurl: URI, name: String): String =
    xmlhead + feed(xmlns := "http://www.w3.org/2005/Atom", title(name),
      id(typeurl.toString), link(href := absurl.resolve(typeurl).toString),
      updated(getFirstArticlePubDate(items)),
      items.map(x => renderAtomEntry(x, absurl)))

  // Returns strings because of escaped vs raw rendering.
  def renderChunks(chunks: List[ArticleChunk]): String = chunks.map(renderChunk).mkString("\n")

  def renderChunk(chunk: ArticleChunk): String = chunk match {
    case TextileText(text) => MarkWrap.parserFor(MarkupType.Textile).parseToHTML(text);
    case HtmlText(text) => text
    case PullQuote(text) => renderPullquote(text)
  }

  def renderPullquote(pullQuote: String): String =
    if (!pullQuote.trim.isEmpty)
      span(`class` := "pullquote", pullQuote).toString
    else
      ""

  def renderAtomEntry(item: CompleteArticle, absUrl: URI): Modifier =
    entry(title(item.article.heading), link(href :=
      absUrl.resolve("/").resolve(item.article.uri).toString),
      updated(item.article.pubDate.toString), author(Atom.name(item.author.name),
        Atom.uri(appendHtml(absUrl.resolve("/").resolve(item.author.uri.get)).toString)),
      id(new URI("/").resolve(item.article.uri).toString),
      item.article.extract.toList.map(x => summary(x)),
      Atom.content(renderChunks(item.article.content)),
      item.categories.map(x => category(term := x.name, scheme := absUrl.resolve("/").resolve(x.uri).toString)))

  def renderIndex(items: List[CompleteArticle]): String =
    docType + html(lang := "en-AU", renderHeadWithAtom(siteName, new URI("/index.atom")), body(header(h1(siteName)),
      section(items.map(item =>
        div(`class` := "h-entry", renderEntryWithPermalink(item))
      )), renderFooter))

  def renderCategories(items: Map[Category, List[CompleteArticle]]): String =
    docType + html(lang := "en-AU", renderHead(categoriesString + ": " + siteName),
        body(header(h1(siteName)), section(h2(categoriesString), items.toList.flatMap(item =>
            List(renderCategoryHeading(item._1), renderArticleList(item._2))
        )), renderFooter)
      )

  def renderAuthors(items: Map[Author, List[CompleteArticle]]): String =
    docType + html(lang := "en-AU", renderHead(authorsString + ": " + siteName),
        body(header(h1(siteName)), section(h2(authorsString), items.toList.flatMap(item =>
            List(renderAuthorHeading(item._1), renderArticleList(item._2))
        )), renderFooter)
      )

  def renderCategory(cat: Category, articles: List[CompleteArticle]): String =
    docType + html(lang := "en-AU", renderHeadWithAtom(cat.name + " :: " + categoriesString + ": " + siteName,
      new URI("/categories/").resolve(cat.uri.toString + ".atom")), body(
        header(h1(cat.name)), section(renderArticleList(articles)), renderFooter
      ))

  def renderAuthor(author: Author, articles: List[CompleteArticle]): String =
    docType + html(lang := "en-AU", renderHeadWithAtom(author.name + " :: " + authorsString + ": " + siteName,
      new URI("/authors/").resolve(author.uri.toString + ".atom")), body(
        header(h1(author.name)), section(renderArticleList(articles)), renderFooter
      ))

  def renderArticleList(articles: List[CompleteArticle]) =
    ul(articles.map(art => li(`class` := "h-entry", renderArticleHeaderForList(art))))

  def renderAuthorHeading(author: Author): Modifier =
    h3(a(href := appendHtml(authorUri.resolve(author.uri.get)).toString, author.name))

  def renderCategoryHeading(cat: Category): Modifier = h3(a(href := appendHtml(catUri.resolve(cat.uri)).toString,
    cat.name))
  
  def renderDate(date: DateTime): String = DateTimeFormat.forPattern("dd MMM yyyy").print(date)

  def renderArticleHeaderForList(articleInfo: CompleteArticle): List[Modifier] =
    p(`class` := "p-name", a(href := new URI("/").resolve(articleInfo.article.uri).toString,
      articleInfo.article.heading)) :: renderEntryHeader(articleInfo)

  def renderEntryHeader(articleInfo: CompleteArticle): List[Modifier] = {
    val article = articleInfo.article
     List(
      article.extract.map(extract => p(`class` := "extract, p-summary")(extract)).getOrElse(""),
	  p(`class` := "byline", "by ", a(`class`:= "p-author",
        href := appendHtml(authorUri.resolve(articleInfo.author.uri.get)).toString,
        (articleInfo.author.name)), " on ", span(`class` := "dt-published", renderDate(article.pubDate)))
	)
  }
  
  def renderEntryBody(article: Article): List[Modifier] =
    List(div(`class` := "e-content", raw(renderChunks(article.content))))
  
  def renderEntryWithPermalink(articleInfo: CompleteArticle): List[Modifier] =
    a(href := new URI("/").resolve(articleInfo.article.uri).toString,
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
      
  def renderHead(heading: String, article: Option[CompleteArticle] = None) =
    head(renderHeadList(heading, article))
  
  def renderHeadWithAtom(heading: String, atomUri: URI) = head(renderHeadList(heading), renderAtomLink(atomUri))

  def renderHeadList(heading: String, article: Option[CompleteArticle] = None): List[Modifier] =
    title(heading) :: renderMeta ::: article.map(x => twitterSummary(x)).getOrElse(List[Modifier]()) :::
      renderStylesheets

  def renderAtomLink(ref: URI): Modifier = link(rel := "alternate", `type` := "application/atom+xml",
    href := ref.toString)

  val docType: String = "<!DOCTYPE html>"

  def twitterSummary(articleInfo: CompleteArticle): List[Modifier] = List(
    meta(name := "twitter:card", content := "summary"),
    meta(name := "twitter:site", content := "@thesunnyk"),
    meta(name := "twitter:title", content := articleInfo.article.heading),
    meta(name := "twitter:description", content := articleInfo.article.extract.
      getOrElse("Amazing stories of the 21st century"))
  )

  def render(articleInfo: CompleteArticle): String = {
    val article = articleInfo.article
    docType + html(lang := "en-AU", renderHead(article.heading + ": " + siteName, Some(articleInfo)), body(
      header(h1(siteName)), section(div(`class` := "h-entry", renderEntry(articleInfo))),
      renderFooter))
  }

  def renderFooter = footer(a(href := "/", "Home"), " | ", a(href := "/categories/", "Categories"),
    " | ", a(href := "/authors/", "Authors"), " | ",
    a(href := "http://www.colourlovers.com/palette/1369317/Waiheke_Island", "Colour Scheme"))

}
