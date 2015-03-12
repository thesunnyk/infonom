package org.teamchoko.infonom.tomato

import doobie.imports._
import scalaz.concurrent.Task
import java.util.Date
import org.teamchoko.infonom.carrot.Articles.{Category, Author, Comment, Article}
import org.teamchoko.infonom.carrot.Articles.{TextFilter, Html, Textile}
import java.net.URI
import org.joda.time.DateTime
import java.sql.Timestamp

object DbConn {
  def xa = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:file:test.db", "sa", "")

  implicit val UriMeta: Meta[URI] =
    Meta[String].nxmap(x => new URI(x), x => x.toASCIIString())

  implicit val DateTimeMeta: Meta[DateTime] =
    Meta[Timestamp].nxmap(x => new DateTime(x), x => new Timestamp(x.getMillis()))

  implicit val TextFilterMeta: Meta[TextFilter] =
    Meta[String].nxmap(
      x => x.toLowerCase match {
        case x if x == "html" => Html()
        case x if x == "textile" => Textile()
      },
      x => x match {
        case Html() => "html"
        case Textile() => "textile"
      })

  def getAuthorById(aid: Int) = sql"select name, email, uri from author where id = $aid".query[Author]

  val getAuthors = sql"select name, email, uri from author".query[Author]

  def createAuthor(a: Author) = sql"""
      insert into author (name, email, uri)
      values (${a.name}, ${a.email}, ${a.uri})
    """.update

  val lastVal = sql"select lastval()".query[Int].unique
  
  def deleteAuthorById(aid: Int) = sql"delete from author where id = ${aid}".update

  val createAuthorTable = sql"""
      create table author (
        id serial primary key,
        name varchar not null,
        email varchar,
        uri varchar
      )
    """.update

  val getCategories = sql"select name, uri from category".query[Category]

  def getCategoryById(aid: Int) = sql"select name, uri from category where id = $aid".query[Category]

  def createCategory(cat: Category) = sql"""
      insert into category (name, uri)
      values (${cat.name}, ${cat.uri})
    """.update

  def deleteCategoryById(aid: Int) = sql"""
      delete from category where id = ${aid}
    """.update

  val createCategoryTable = sql"""
      create table category (
        id serial primary key,
        name varchar not null,
        uri varchar not null
      )
    """.update

  def getCommentById(aid: Int) = sql"select body, pubdate from comment where id = $aid".query[Comment]

  def deleteCommentById(aid: Int) = sql"delete from comment where id = $aid".update

  def createComment(c: Comment) = sql"""
      insert into comment (body, pubdate)
      values (${c.text}, ${c.pubDate})
    """.update

  // TODO body should be text, and we should have a way of reading / writing it
  val createCommentTable: Update0 = sql"""
      create table comment (
        id serial primary key,
        body longvarchar not null,
        pubdate timestamp not null
      )
    """.update
  
  case class CompleteCommentDb(articleid: Long, commentid: Long, authorid: Long)

  def getCompleteCommentById(aid: Int) = sql"""
      select articleid, commentid, authorid
      from completecomment
      where id = $aid
    """.query[CompleteCommentDb].list
    
  val createCompleteCommentTable: Update0 = sql"""
      create table completecomment (
        id serial,
        articleid long,
        commentid long,
        authorid long
      )
    """.update

  def getArticleById(aid: Long) = sql"""
      select heading, body, textfilter, draft, extract, pullquote, pubdate, uri
      from article
      where id = $aid
    """.query[Article]

  def createArticle(a: Article) = sql"""
      insert into article (heading, body, textfilter, draft, extract, pullquote, pubdate, uri)
      values (${a.heading}, ${a.text}, ${a.textFilter}, ${a.draft}, ${a.extract},
        ${a.pullquote}, ${a.pubDate}, ${a.uri})
    """.update

  def deleteArticleById(aid: Long) = sql"delete from article where id = $aid".update

  // TODO body should be text, and we should have a way of reading / writing it
  val createArticleTable: Update0 = sql"""
      create table article (
        id serial,
        heading varchar,
        body longvarchar,
        textFilter varchar,
        draft bool not null,
        extract longvarchar,
        pullquote longvarchar,
        pubdate timestamp,
        uri varchar
      )
    """.update

  case class CompleteArticleDb(articleId: Long, completecommentid: Long, categoryid: Long, authorid: Long)

  // TODO Sorting? Order by date but the date is in regular article.
  val getCompleteArticle = sql"""
      select articleid, completecommentid, categoryid, authorid
      from completearticle
    """.query[CompleteArticleDb].list

  val createCompleteArticleTable: Update0 = sql"""
      create table completearticle {
        id serial,
        articleid long,
        completecommentid long,
        categoryid long,
        authorid long
      }
    """.update
}
