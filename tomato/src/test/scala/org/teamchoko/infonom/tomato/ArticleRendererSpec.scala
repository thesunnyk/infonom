package org.teamchoko.infonom.tomato

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.joda.time.DateTime
import java.net.URI
import org.teamchoko.infonom.carrot.Articles._

class ArticleRendererSpec extends FlatSpec with Matchers {
    val complete = new CompleteArticle {
      val article: Article = Article("heading", "some _things_ are *stuff*", Textile, false, None, None,
        new DateTime(2013, 5, 10, 10, 10, 10), new URI("/articles/article"))
      val comments: List[CompleteComment] = Nil
      val categories: List[Category] = Nil
      val author: Author = Author("name", None, None)
    }

    val author2: Author = Author("altname", None, Some(new URI("/alt")))

	"ArticleRenderer" should "render" in {
      ArticleRenderer.render(complete) should equal("<!DOCTYPE html><html><head><title>heading" +
        ": The USS Quad Damage</title>" +
        "<meta charset=\"utf-8\" /><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\" />" +
        "<meta name=\"description\" content=\"\" /><meta name=\"viewport\" content=\"width=device-width\" />" +
        "<link rel=\"stylesheet\" href=\"/css/normalize.css\" /><link rel=\"stylesheet\" href=\"/css/main.css\" />" +
        "</head><body><h1>The USS Quad Damage</h1><div class=\"h-entry\"><h2 class=\"p-name\">heading</h2><p>by " +
        "<span class=\"p-author\">name</span> on <span class=\"dt-published\">10 May 2013</span></p>" +
        "<div class=\"e-content\"><p>some <em>things</em> are <strong>stuff</strong></p></div></div></body></html>")
	}

    val cat = Category("Test", new URI("/test"))

    it should "render category" in {
      ArticleRenderer.renderCategory(cat, List(complete)) should equal("<!DOCTYPE html><html><head><title>Test" +
        " :: Categories: The USS Quad Damage</title>" +
        "<meta charset=\"utf-8\" /><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\" />" +
        "<meta name=\"description\" content=\"\" /><meta name=\"viewport\" content=\"width=device-width\" />" +
        "<link rel=\"stylesheet\" href=\"/css/normalize.css\" /><link rel=\"stylesheet\" href=\"/css/main.css\" />" +
        "</head><body><h1>Test</h1><ul><li class=\"h-entry\"><p class=\"p-name\">heading</p><p>by " +
        "<span class=\"p-author\">name</span> on <span class=\"dt-published\">10 May 2013</span></p>" +
        "</li></ul></body></html>")
    }

    it should "render categories" in {
      ArticleRenderer.renderCategories(Map(cat -> List(complete))) should equal("<!DOCTYPE html><html><head><title>" +
        "Categories: The USS Quad Damage</title>" +
        "<meta charset=\"utf-8\" /><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\" />" +
        "<meta name=\"description\" content=\"\" /><meta name=\"viewport\" content=\"width=device-width\" />" +
        "<link rel=\"stylesheet\" href=\"/css/normalize.css\" /><link rel=\"stylesheet\" href=\"/css/main.css\" />" +
        "</head><body><h1>The USS Quad Damage</h1><h2>Categories</h2><h3><a href=\"/test\">Test</a></h3>" +
        "<ul><li class=\"h-entry\"><p class=\"p-name\">heading</p><p>by " +
        "<span class=\"p-author\">name</span> on <span class=\"dt-published\">10 May 2013</span></p>" +
        "</li></ul></body></html>")
    }

    it should "render authors" in {
      ArticleRenderer.renderAuthors(Map(author2 -> List(complete))) should equal("<!DOCTYPE html><html><head><title>" +
        "Authors: The USS Quad Damage</title>" +
        "<meta charset=\"utf-8\" /><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\" />" +
        "<meta name=\"description\" content=\"\" /><meta name=\"viewport\" content=\"width=device-width\" />" +
        "<link rel=\"stylesheet\" href=\"/css/normalize.css\" /><link rel=\"stylesheet\" href=\"/css/main.css\" />" +
        "</head><body><h1>The USS Quad Damage</h1><h2>Authors</h2><h3><a href=\"/alt\">altname</a></h3>" +
        "<ul><li class=\"h-entry\"><p class=\"p-name\">heading</p><p>by " +
        "<span class=\"p-author\">name</span> on <span class=\"dt-published\">10 May 2013</span></p>" +
        "</li></ul></body></html>")
    }

    it should "render authors without url" in {
      ArticleRenderer.renderAuthors(Map(complete.author -> List(complete))) should equal(
        "<!DOCTYPE html><html><head><title>" +
        "Authors: The USS Quad Damage</title>" +
        "<meta charset=\"utf-8\" /><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\" />" +
        "<meta name=\"description\" content=\"\" /><meta name=\"viewport\" content=\"width=device-width\" />" +
        "<link rel=\"stylesheet\" href=\"/css/normalize.css\" /><link rel=\"stylesheet\" href=\"/css/main.css\" />" +
        "</head><body><h1>The USS Quad Damage</h1><h2>Authors</h2><h3>name</h3>" +
        "<ul><li class=\"h-entry\"><p class=\"p-name\">heading</p><p>by " +
        "<span class=\"p-author\">name</span> on <span class=\"dt-published\">10 May 2013</span></p>" +
        "</li></ul></body></html>")
    }

    it should "render index" in {
      ArticleRenderer.renderIndex(List(complete)) should equal("<!DOCTYPE html><html><head><title>" +
        "The USS Quad Damage</title>" +
        "<meta charset=\"utf-8\" /><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\" />" +
        "<meta name=\"description\" content=\"\" /><meta name=\"viewport\" content=\"width=device-width\" />" +
        "<link rel=\"stylesheet\" href=\"/css/normalize.css\" /><link rel=\"stylesheet\" href=\"/css/main.css\" />" +
        "</head><body><h1>The USS Quad Damage</h1><div class=\"h-entry\"><a href=\"/articles/article\">" +
        "<h2 class=\"p-name\">heading</h2></a><p>by " +
        "<span class=\"p-author\">name</span> on <span class=\"dt-published\">10 May 2013</span></p>" +
        "<div class=\"e-content\"><p>some <em>things</em> are <strong>stuff</strong></p></div></div></body></html>")
    }

    it should "render index rss" in {
      ArticleRenderer.renderIndexAtom(List(complete), new URI("http://www.example.com")) should equal(
        "<?xml version=\"1.0\" encoding=\"utf-8\"?><feed xmlns=\"http://www.w3.org/2005/Atom\">" +
        "<title>The USS Quad Damage</title>" +
        "<id>/</id><link href=\"http://www.example.com/\" /><updated>2013-05-10T10:10:10.000+10:00</updated>" +
        "<entry><title>heading</title><link href=\"http://www.example.com/articles/article\" />" +
        "<updated>2013-05-10T10:10:10.000+10:00</updated><author><name>name</name></author>" +
        "<id>/articles/article</id><content>&lt;p&gt;some &lt;em&gt;things&lt;/em&gt; are " +
        "&lt;strong&gt;stuff&lt;/strong&gt;&lt;/p&gt;</content>" +
        "</entry></feed>")
    }
}
