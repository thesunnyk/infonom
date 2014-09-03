import java.net.URLEncoder

case class Config(url: String)

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

class Couchdb(host: String, port: Int) {
}

