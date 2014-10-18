package org.teamchoko.infonom

import java.net.URLEncoder

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

class Couchdb(host: String, port: Int) {
  def getArticles(count: Int = 10, offset: Int = 0) : Seq[Article] = ???
}

