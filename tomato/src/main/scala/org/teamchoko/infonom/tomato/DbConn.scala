package org.teamchoko.infonom.tomato

import doobie.imports._
import scalaz.concurrent.Task
import java.util.Date
import org.teamchoko.infonom.carrot.Articles.{Category, Author, Comment, Article}
import org.teamchoko.infonom.carrot.Articles.{TextFilter, Html, Textile}
import java.net.URI
import org.joda.time.DateTime

object DbConn {
  def xa = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:file:test.db", "sa", "")

  implicit val UriMeta: Meta[URI] =
    Meta[String].nxmap(x => new URI(x), x => x.toASCIIString())

  implicit val DateTimeMeta: Meta[DateTime] =
    Meta[Date].nxmap(x => new DateTime(x), x => x.toDate())

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
  
  def deleteAuthor(a: Author) = sql"delete from author where name = ${a.name}".update

  val createAuthorTable = sql"""
      create table author (
        id serial primary key,
        name varchar not null,
        email varchar,
        uri varchar
      )
    """.update

  val getCategory = sql"select name, uri from category".query[Category]

  def getCategoryById(aid: Long) = sql"select name, uri from category where id = $aid".query[Category]

  val createCategoryTable: Update0 = sql"""
      create table category {
        id serial,
        name varchar not null,
        uri varchar not null
      }
    """.update

  def getCommentById(aid: Long) = sql"select body, pubdate from comment where id = $aid".query[Comment].list

  val createCommentTable: Update0 = sql"""
      create table comment {
        id serial,
        body text,
        pubdate timestamp
      }
    """.update
  
  case class CompleteCommentDb(articleid: Long, commentid: Long, authorid: Long)

  def getCompleteCommentById(aid: Long) = sql"""
      select articleid, commentid, authorid
      from completecomment
      where id = $aid
    """.query[CompleteCommentDb].list
    
  val createCompleteCommentTable: Update0 = sql"""
      create table completecomment {
        id serial,
        articleid long,
        commentid long,
        authorid long
      }
    """.update

  def getArticleById(aid: Long) = sql"""
      select heading, body, textfilter, draft, extract, pullquote, pubdate, uri
      from article
      where id = $aid
    """.query[Article].list

  val createArticleTable: Update0 = sql"""
      create table article {
        id serial,
        heading varchar,
        body text,
        textFilter varchar,
        draft bool,
        extract text,
        pullquote text,
        pubdate timestamp,
        uri varchar
      }
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
