package org.teamchoko.infonom.tomato

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.joda.time.DateTime
import java.net.URI
import org.teamchoko.infonom.carrot.{Html, Textile, CompleteArticle, Article}
import org.teamchoko.infonom.carrot.{Author, Category, Comment}

class ArticleRendererSpec extends FlatSpec with Matchers {
	"ArticleRenderer" should "render" in {
    val complete = new CompleteArticle {
      val article: Article = Article("heading", "some _things_ are *stuff*", Textile(), false, None, None,
        new DateTime(2013, 5, 10, 10, 10, 10), new URI("http://www.google.com"))
      val comments: List[Comment] = Nil
      val categories: List[Category] = Nil
      val author: Author = Author("name", None, None)
    }

    ArticleRenderer.render(complete) should equal("<!DOCTYPE html><html><head><title>heading</title></head>" +
    "<body><div class=\"h-entry\"><h1 class=\"p-name\">heading</h1><p>by<span class=\"p-author\">name</span>" +
    "on<span class=\"dt-published\">10 May 2013</span></p>" +
    "<div class=\"e-content\"><p>some <em>things</em> are <strong>stuff</strong></p></div></div></body></html>")

	}
}
