package org.teamchoko.infonom.tomato

import org.teamchoko.infonom.carrot.Author
import argonaut._, Argonaut._

object JsonArticles {

  case class AuthorJson(name: String, email: String, uri: String)
  implicit def AuthorCodecJson = casecodec3(AuthorJson.apply, AuthorJson.unapply)("name", "email", "uri")

}
