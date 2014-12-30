package org.teamchoko.infonom

import scalatags.Text.all._

object ArticleRenderer {
  
  def render(article: Article) = html(head(), body())

}