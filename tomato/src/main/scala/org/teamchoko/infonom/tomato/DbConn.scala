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

  val lastVal = sql"select lastval()".query[Int].unique

  trait DbBasicCrud[T] {
    def getById(id: Int): Query0[T]
    def create(a: T): Update0
    def deleteById(id: Int): Update0
    def createTable: Update0
  }

  trait DbSearch[T] extends DbBasicCrud[T] {
    def getAllItems: Query0[T]
  }

  object AuthorCrud extends DbBasicCrud[Author] with DbSearch[Author] {
    def getById(aid: Int) = sql"select name, email, uri from author where id = $aid".query[Author]

    val getAllItems = sql"select name, email, uri from author".query[Author]

    def create(a: Author) = sql"""
        insert into author (name, email, uri)
        values (${a.name}, ${a.email}, ${a.uri})
      """.update

    def deleteById(aid: Int) = sql"delete from author where id = ${aid}".update

    val createTable = sql"""
        create table author (
          id serial primary key,
          name varchar not null,
          email varchar,
          uri varchar
        )
      """.update
  }

  object CategoryCrud extends DbBasicCrud[Category] with DbSearch[Category] {
    val getAllItems = sql"select name, uri from category".query[Category]

    def getById(aid: Int) = sql"select name, uri from category where id = $aid".query[Category]

    def create(cat: Category) = sql"""
        insert into category (name, uri)
        values (${cat.name}, ${cat.uri})
      """.update

    def deleteById(aid: Int) = sql"""
        delete from category where id = ${aid}
      """.update

    val createTable = sql"""
        create table category (
          id serial primary key,
          name varchar not null,
          uri varchar not null
        )
      """.update
  }

  object CommentCrud extends DbBasicCrud[Comment] {
    def getById(aid: Int) = sql"select body, pubdate from comment where id = $aid".query[Comment]

    def deleteById(aid: Int) = sql"delete from comment where id = $aid".update

    def create(c: Comment) = sql"""
        insert into comment (body, pubdate)
        values (${c.text}, ${c.pubDate})
      """.update

    // TODO body should be text, and we should have a way of reading / writing it
    val createTable: Update0 = sql"""
        create table comment (
          id serial primary key,
          body longvarchar not null,
          pubdate timestamp not null
        )
      """.update
  }

  case class CompleteCommentDb(articleid: Int, commentid: Int, authorid: Int)

  object CompleteCommentCrud extends DbBasicCrud[CompleteCommentDb] {
    def create(c: CompleteCommentDb) = sql"""
        insert into completecomment (articleid, commentid, authorid)
          values (${c.articleid}, ${c.commentid}, ${c.authorid})
        """.update

    def deleteById(aid: Int) = sql"delete from completecomment where id = $aid".update

    def getById(aid: Int) = sql"""
        select articleid, commentid, authorid
        from completecomment
        where id = $aid
      """.query[CompleteCommentDb]

    def getForArticleId(aid: Int) = sql"""
        select articleid, commentid, authorid
        from completecomment
        where articleid = $aid
      """.query[CompleteCommentDb]

    val createTable: Update0 = sql"""
        create table completecomment (
          id serial,
          articleid int not null,
          commentid int not null,
          authorid int not null
        )
      """.update
  }

  object ArticleCrud extends DbBasicCrud[Article] {
    def getById(aid: Int) = sql"""
        select heading, body, textfilter, draft, extract, pullquote, pubdate, uri
        from article
        where id = $aid
      """.query[Article]

    def create(a: Article) = sql"""
        insert into article (heading, body, textfilter, draft, extract, pullquote, pubdate, uri)
        values (${a.heading}, ${a.text}, ${a.textFilter}, ${a.draft}, ${a.extract},
          ${a.pullquote}, ${a.pubDate}, ${a.uri})
      """.update

    def deleteById(aid: Int) = sql"delete from article where id = $aid".update

    // TODO body should be text, and we should have a way of reading / writing it
    val createTable: Update0 = sql"""
        create table article (
          id serial,
          heading varchar not null,
          body longvarchar not null,
          textFilter varchar not null,
          draft bool not null,
          extract longvarchar,
          pullquote longvarchar,
          pubdate timestamp not null,
          uri varchar not null
        )
      """.update
  }

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
