package org.teamchoko.infonom.tomato.render

import scalatags.generic.Util
import scalatags.Text.Cap
import scalatags.text.Builder

object Atom extends Util[Builder, String, String] with Cap {
  val feed = "feed".tag[String]
  val updated = "updated".tag[String]
  val author = "author".tag[String]
  val name = "name".tag[String]
  val uri = "uri".tag[String]
  val id = "id".tag[String]

  val category = "category".tag[String]
  val contributor = "contributor".tag[String]
  val generator = "generator".tag[String]
  val icon = "icon".tag[String]
  val logo = "logo".tag[String]
  val rights = "rights".tag[String]
  val subtitle = "subtitle".tag[String]

  val entry = "entry".tag[String]
  val content = "content".tag[String]
  val summary = "summary".tag[String]

  val term = "term".attr
  val scheme = "scheme".attr
  val label = "label".attr

}
