package org.teamchoko.infonom

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.joda.time.DateTime
import java.net.URI

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
    "<body><h1>heading</h1><p>by<span>name</span>on<span>10 May 2013</span></p>" +
    "<div><p>some <em>things</em> are <strong>stuff</strong></p></div></body></html>")

	}
}
