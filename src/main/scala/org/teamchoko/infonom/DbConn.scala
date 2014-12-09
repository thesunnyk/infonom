package org.teamchoko.infonom

import scala.collection.immutable.Stream

import java.net.URLEncoder
import java.util.UUID
import org.joda.time.DateTime

import scalaz.stream.Process
import scalaz.concurrent.Task
import java.net.URI
import org.http4s.{Response, Status, Method, Message, MediaType, Request, EntityBody, EntityDecoder, Uri, ParseException, ParseResult}
import org.http4s.util.CaseInsensitiveString
import org.http4s.client.Client
import org.http4s.client.Client.Result
import org.http4s.client.blaze.SimpleHttp1Client

object DesignDocs {
  val ArticleData = Map {
	  "all" -> Map {
	    "map" ->
	      """function(doc) {
	    	|	if (doc.type === "article_data") {
	    	|       emit(doc._id, doc);
	    	|	}
	    	|}""".stripMargin
	  };
	  "by_guid" -> Map {
	    "map" ->
	      """function(doc) {
	    	|	if (doc.type === "article_data") {
	    	|       emit(doc.guid, doc);
	    	|	}
	    	|}""".stripMargin
	  };
	  "bookmarked" -> Map {
	    "map" ->
	      """function(doc) {
	    	|	if (doc.type === "article_data" && doc.bookmarked) {
	    	|       emit(doc.date, doc);
	    	|	}
	    	|}""".stripMargin
	  };
	  "by_link_score" -> Map {
	    "map" ->
	      """function(doc) {
	    	|	if (doc.type === "article_data") {
	    	|       emit(doc.linkScore, doc);
	    	|	}
	    	|}""".stripMargin
	  };
	  "by_word_score" -> Map {
	    "map" ->
	      """function(doc) {
	    	|	if (doc.type === "article_data") {
	    	|       emit(doc.wordScore, doc);
	    	|	}
	    	|}""".stripMargin
	  }
  }

}


object CouchUri {
  def sep = "/"
  def encode(s: String) = URLEncoder.encode(s)
  def path(parts: String*) = sep + parts.map(encode).mkString(sep)
  def query(kv: String*) = if (kv.isEmpty) "" else {
    "?" + kv.mkString("&")
  }
  def db(db: String) = path(db)
  def view(db: String, designDocId: String, viewName: String, kvs: List[String]) =
    path(db, "_design", designDocId, "_view", viewName) + query(kvs:_*)
}

// Use argonaut for JSON parsing.

case class CouchEntity[T](item: T, id: UUID, rev: UUID, entityType: String)

object Couchdb {
  def apply(host: URI): ParseResult[Couchdb] = Uri.fromString(host.toASCIIString()).map(new Couchdb(_))
}

class Couchdb(host: Uri) {

  def decodeArticle(msg: Message): Task[CouchEntity[Article]] = {
    val id = new UUID(123L, 234L)
    val rev = new UUID(123L, 234L)
    val entityType = "article"
    val article = Article("heading",
                   "text",
                   Textile(),
                   true,
                   None,
                   None,
                   DateTime.now(),
                   URI.create("/test"))
    Task.now(CouchEntity(article, id, rev, entityType))
  }
  
  def articleDecoder(resp: Response): EntityDecoder[CouchEntity[Article]] = EntityDecoder(decodeArticle,
      MediaType.`application/json`)
  
  def getArticles(count: Int = 10, offset: Int = 0) : Task[Result[CouchEntity[Article]]] = {
    SimpleHttp1Client.decode(Request(Method.GET, host))(articleDecoder)
  }

  def getCompleteArticle(article: CouchEntity[Article]) : CompleteArticle = ???

  def updateArticle(article: CouchEntity[Article], newArticle: Article) : CouchEntity[Article] = ???

  def getCategories(count: Int = 10, offset: Int = 0) : Process[Task, CouchEntity[Category]] = ???
}

