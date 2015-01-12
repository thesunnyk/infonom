package org.teamchoko.infonom.tomato

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import java.net.URI
import org.teamchoko.infonom.carrot.Author
import org.teamchoko.infonom.tomato.JsonArticles._
import argonaut._
import Argonaut._

class JsonArticlesSpec extends FlatSpec with Matchers {
	"Json encode" should "create author" in {
    val author = Author("things", None, Some(new URI("/author/things")))

    val jsonAuthor = author.asJson
    jsonAuthor.nospaces should equal("{\"name\":\"things\",\"email\":null,\"uri\":\"/author/things\"}")
	}

	"Json decode" should "parse author" in {
    val authorString = "{\"name\":\"things\",\"uri\":\"/author/things\"}"
    val author = Author("things", None, Some(new URI("/author/things")))

    val fromJsonAuthor = authorString.decodeOption[Author]

    fromJsonAuthor should equal(Some(author))
	}

	"Json decode" should "parse null email" in {
    val authorString = "{\"name\":\"things\",\"email\":null,\"uri\":\"/author/things\"}"
    val author = Author("things", None, Some(new URI("/author/things")))

    val fromJsonAuthor = authorString.decodeOption[Author]

    fromJsonAuthor should equal(Some(author))
	}
}
