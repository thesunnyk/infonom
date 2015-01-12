package org.teamchoko.infonom.tomato

import java.net.URI
import org.teamchoko.infonom.carrot.Author
import argonaut._, Argonaut._

object JsonArticles {

  implicit val UriEncodeJson: EncodeJson[URI] = new EncodeJson[URI] {
    override def encode(uri: URI) = jString(uri.toASCIIString)
  }

  implicit val UriDecodeJson: DecodeJson[URI] = DecodeJson(js => for {
    item <- js.as[String]
  } yield new URI(item))

  implicit def AuthorCodecJson = casecodec3(Author.apply, Author.unapply)("name", "email", "uri")

}
