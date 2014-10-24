package org.teamchoko.infonom

import scala.collection.immutable.Stream
import java.net.URLEncoder
import java.util.UUID
import scalaz.stream.Process
import scalaz.concurrent.Task
import java.net.URI
import org.http4s.Status
import org.http4s.Method
import org.http4s.EntityDecoder

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

class Couchdb(host: URI) {

  def getArticles(count: Int = 10, offset: Int = 0) : Process[Task, CouchEntity[Article]] = ???
    // Method.GET("https://www.google.com").on(Status.Ok)(EntityDecoder.text)

  def getCompleteArticle(article: CouchEntity[Article]) : CompleteArticle = ???

  def updateArticle(article: CouchEntity[Article], newArticle: Article) : CouchEntity[Article] = ???

  def getCategories(count: Int = 10, offset: Int = 0) : Process[Task, CouchEntity[Category]] = ???
}

