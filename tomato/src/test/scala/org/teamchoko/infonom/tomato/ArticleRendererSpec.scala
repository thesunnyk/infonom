package org.teamchoko.infonom.tomato

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.joda.time.DateTime
import java.net.URI
import org.teamchoko.infonom.carrot.Articles._

class ArticleRendererSpec extends FlatSpec with Matchers {
    val complete = new CompleteArticle {
      val article: Article = Article("heading", "some _things_ are *stuff*", Textile(), false, None, None,
        new DateTime(2013, 5, 10, 10, 10, 10), new URI("http://www.google.com"))
      val comments: List[CompleteComment] = Nil
      val categories: List[Category] = Nil
      val author: Author = Author("name", None, None)
    }

	"ArticleRenderer" should "render" in {
      ArticleRenderer.render(complete) should equal("<!DOCTYPE html><html><head><title>heading" +
        ": The USS Quad Damage</title>" +
        "<meta charset=\"utf-8\" /><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\" />" +
        "<meta name=\"description\" content=\"\" /><meta name=\"viewport\" content=\"width=device-width\" />" +
        "<link rel=\"stylesheet\" href=\"/css/normalize.css\" /><link rel=\"stylesheet\" href=\"/css/main.css\" />" +
        "</head><body><div class=\"h-entry\"><h1 class=\"p-name\">heading</h1><p>by " +
        "<span class=\"p-author\">name</span> on <span class=\"dt-published\">10 May 2013</span></p>" +
        "<div class=\"e-content\"><p>some <em>things</em> are <strong>stuff</strong></p></div></div></body></html>")
	}

    val cat = Category("Test", new URI("/test"))

    "ArticleRenderer" should "render category" in {
      ArticleRenderer.renderCategory(cat, List(complete)) should equal("<!DOCTYPE html><html><head><title>Test" +
        " :: Categories: The USS Quad Damage</title>" +
        "<meta charset=\"utf-8\" /><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\" />" +
        "<meta name=\"description\" content=\"\" /><meta name=\"viewport\" content=\"width=device-width\" />" +
        "<link rel=\"stylesheet\" href=\"/css/normalize.css\" /><link rel=\"stylesheet\" href=\"/css/main.css\" />" +
        "</head><body><h1>Test</h1><ul><li class=\"h-entry\"><p class=\"p-name\">heading</p><p>by " +
        "<span class=\"p-author\">name</span> on <span class=\"dt-published\">10 May 2013</span></p>" +
        "</li></ul></body></html>")
    }

    "ArticleRenderer" should "render categories" in {
      ArticleRenderer.renderCategories(Map(cat -> List(complete))) should equal("<!DOCTYPE html><html><head><title>" +
        "Categories: The USS Quad Damage</title>" +
        "<meta charset=\"utf-8\" /><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\" />" +
        "<meta name=\"description\" content=\"\" /><meta name=\"viewport\" content=\"width=device-width\" />" +
        "<link rel=\"stylesheet\" href=\"/css/normalize.css\" /><link rel=\"stylesheet\" href=\"/css/main.css\" />" +
        "</head><body><h1>Categories</h1><h2><a href=\"/test\">Test</a></h2>" +
        "<ul><li class=\"h-entry\"><p class=\"p-name\">heading</p><p>by " +
        "<span class=\"p-author\">name</span> on <span class=\"dt-published\">10 May 2013</span></p>" +
        "</li></ul></body></html>")
    }
}
