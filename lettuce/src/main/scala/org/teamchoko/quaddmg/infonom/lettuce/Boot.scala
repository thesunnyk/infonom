package org.teamchoko.infonom.lettuce

import org.joda.time.DateTime
import org.teamchoko.infonom.carrot.Articles._

object Boot extends App {
  
  def makeXml(x: Author) = <author id={"id"}><name>{x.name}</name><email>{x.email}</email></author>

  def makeXml(x: Category) =
    <category id={"theid"}><name>{x.name}</name><permalink>{x.uri}</permalink></category>

  def makeCommentXml(x: CompleteComment) = <comment date={niceTime(x.comment.pubDate)}>
    <author><name>{x.author.name}</name><email>{x.author.email}</email><url>{x.author.uri}</url></author>
    <text>{x.comment.text}</text>
    </comment>

  def makeCatIdXml(x: Category) = <category id={x.name}/>

  def filter(x: TextFilter) = x match {
    case Textile() => "Textile";
    case Html() => "Html"
  }

  def niceTime(x: DateTime) = x.toString

  def makeXml(x: CompleteArticle) = <article id={"id"} date={niceTime(x.article.pubDate)}>
    <permalink>{x.article.uri}</permalink>
    <heading>{x.article.heading}</heading>
    <textFilter type={filter(x.article.textFilter)} />
    <extract>{x.article.extract match {
      case Some(x) => x;
      case None => "";
    }}</extract>
    <pullquote>{x.article.pullquote match {
      case Some(x) => x;
      case None => "";
    }}</pullquote>
    <text>{x.article.text}</text>
    <comments>
    {x.comments.map(makeCommentXml)}
    </comments>
    <author id={x.author.toString}/>
    <categories>{x.categories.map(makeCatIdXml)}</categories>
    </article>


  val authors: List[Author] = List()
  val categories: List[Category] = List()
  val entries: List[CompleteArticle] = List()
  
  val xmlData = {
    <xml>
    <authors>{authors.map(makeXml)}</authors>
    <categories>{categories.map(makeXml)}</categories>
    <articles>
      {entries.map(makeXml)}
    </articles>
    </xml>
  }

}

