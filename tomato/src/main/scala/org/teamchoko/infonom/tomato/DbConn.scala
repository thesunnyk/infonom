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
    override def getById(aid: Int) = sql"select name, email, uri from author where id = $aid".query[Author]

    override val getAllItems = sql"select name, email, uri from author".query[Author]

    override def create(a: Author) = sql"""
        insert into author (name, email, uri)
        values (${a.name}, ${a.email}, ${a.uri})
      """.update

    override def deleteById(aid: Int) = sql"delete from author where id = ${aid}".update

    override val createTable = sql"""
        create table author (
          id serial primary key,
          name varchar not null,
          email varchar,
          uri varchar
        )
      """.update
  }

  object CategoryCrud extends DbBasicCrud[Category] with DbSearch[Category] {
    override val getAllItems = sql"select name, uri from category".query[Category]

    override def getById(aid: Int) = sql"select name, uri from category where id = $aid".query[Category]

    override def create(cat: Category) = sql"""
        insert into category (name, uri)
        values (${cat.name}, ${cat.uri})
      """.update

    override def deleteById(aid: Int) = sql"""
        delete from category where id = ${aid}
      """.update

    override val createTable = sql"""
        create table category (
          id serial primary key,
          name varchar not null,
          uri varchar not null
        )
      """.update
  }

  object CommentCrud extends DbBasicCrud[Comment] {
    override def getById(aid: Int) = sql"select body, pubdate from comment where id = $aid".query[Comment]

    override def deleteById(aid: Int) = sql"delete from comment where id = $aid".update

    override def create(c: Comment) = sql"""
        insert into comment (body, pubdate)
        values (${c.text}, ${c.pubDate})
      """.update

    // TODO body should be text, and we should have a way of reading / writing it
    override val createTable: Update0 = sql"""
        create table comment (
          id serial primary key,
          body longvarchar not null,
          pubdate timestamp not null
        )
      """.update
  }

  case class CompleteCommentDb(completearticleid: Int, commentid: Int, authorid: Int)

  object CompleteCommentCrud extends DbBasicCrud[CompleteCommentDb] {
    override def create(c: CompleteCommentDb) = sql"""
        insert into completecomment (completearticleid, commentid, authorid)
          values (${c.completearticleid}, ${c.commentid}, ${c.authorid})
        """.update

    override def deleteById(aid: Int) = sql"delete from completecomment where id = $aid".update

    override def getById(aid: Int) = sql"""
        select completearticleid, commentid, authorid
        from completecomment
        where id = $aid
      """.query[CompleteCommentDb]

    def getForCompleteArticleId(aid: Int) = sql"""
        select completearticleid, commentid, authorid
        from completecomment
        where completearticleid = $aid
      """.query[CompleteCommentDb]

    override val createTable: Update0 = sql"""
        create table completecomment (
          id serial,
          completearticleid int not null,
          commentid int not null,
          authorid int not null
        )
      """.update
  }

  object ArticleCrud extends DbBasicCrud[Article] {
    override def getById(aid: Int) = sql"""
        select heading, body, textfilter, draft, extract, pullquote, pubdate, uri
        from article
        where id = $aid
      """.query[Article]

    override def create(a: Article) = sql"""
        insert into article (heading, body, textfilter, draft, extract, pullquote, pubdate, uri)
        values (${a.heading}, ${a.text}, ${a.textFilter}, ${a.draft}, ${a.extract},
          ${a.pullquote}, ${a.pubDate}, ${a.uri})
      """.update

    override def deleteById(aid: Int) = sql"delete from article where id = $aid".update

    // TODO body should be text, and we should have a way of reading / writing it
    override val createTable: Update0 = sql"""
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

  case class CompleteArticleDb(articleid: Int, categoryid: Int, authorid: Int)

  // TODO Sorting? Order by date but the date is in regular article.
  object CompleteArticleCrud extends DbBasicCrud[CompleteArticleDb] with DbSearch[CompleteArticleDb] {
    override def getById(aid: Int) = sql"""
        select articleid, categoryid, authorid
        from completearticle
        where id = $aid
      """.query[CompleteArticleDb]

    override def create(a: CompleteArticleDb) = sql"""
        insert into completearticle (articleid, categoryid, authorid)
        values (${a.articleid}, ${a.categoryid}, ${a.authorid})
      """.update

    override def deleteById(id: Int) = sql"delete from completearticle where id = ${id}".update

    override def getAllItems = sql"select articleid, categoryid, authorid from completearticle".query[CompleteArticleDb]

    override val createTable: Update0 = sql"""
        create table completearticle (
          id serial,
          articleid int not null,
          categoryid int not null,
          authorid int not null
        )
      """.update
  }
}
