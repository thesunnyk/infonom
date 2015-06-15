package org.teamchoko.infonom.tomato

import java.net.URI
import java.sql.Timestamp

import scala.reflect.runtime.universe

import org.joda.time.DateTime
import org.teamchoko.infonom.carrot.Articles.Article
import org.teamchoko.infonom.carrot.Articles.Author
import org.teamchoko.infonom.carrot.Articles.Category
import org.teamchoko.infonom.carrot.Articles.Comment
import org.teamchoko.infonom.carrot.Articles.CompleteArticle
import org.teamchoko.infonom.carrot.Articles.CompleteArticleCase
import org.teamchoko.infonom.carrot.Articles.CompleteComment
import org.teamchoko.infonom.carrot.Articles.CompleteCommentCase
import org.teamchoko.infonom.carrot.Articles.Html
import org.teamchoko.infonom.carrot.Articles.TextFilter
import org.teamchoko.infonom.carrot.Articles.Textile

import doobie.imports.ConnectionIO
import doobie.imports.DriverManagerTransactor
import doobie.imports.Meta
import doobie.imports.Query0
import doobie.imports.Update0
import doobie.imports.toMoreConnectionIOOps
import doobie.imports.toSqlInterpolator

import scalaz._
import scalaz.Scalaz._
import scalaz.concurrent.Task

object DbConn {
  def xa = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:file:./test.db", "sa", "")

  implicit val UriMeta: Meta[URI] =
    Meta[String].nxmap(x => new URI(x), x => x.toASCIIString())

  implicit val DateTimeMeta: Meta[DateTime] =
    Meta[Timestamp].nxmap(x => new DateTime(x), x => new Timestamp(x.getMillis()))

  implicit val TextFilterMeta: Meta[TextFilter] =
    Meta[String].nxmap(
      x => x.toLowerCase match {
        case x if x == "html" => Html
        case x if x == "textile" => Textile
      },
      x => x match {
        case Html => "html"
        case Textile => "textile"
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

    def getIdByName(name: String) = sql"select id from author where name = $name".query[Int]

    override val getAllItems = sql"select name, email, uri from author".query[Author]

    override def create(a: Author) = sql"""
        insert into author (name, email, uri)
        values (${a.name}, ${a.email}, ${a.uri})
      """.update

    def update(id: Int, a: Author) = sql"""
        update author set name=${a.name}, email=${a.email}, uri=${a.uri}
        where id=${id}
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

  def createAuthorAndGetId(a: Author) = for {
      _ <- AuthorCrud.create(a).run
      authorId <- lastVal
    } yield authorId

  def saveOrUpdateAuthor(a: Author) = for {
      maybeAuthorId <- AuthorCrud.getIdByName(a.name).option
      authorId <- maybeAuthorId.fold(createAuthorAndGetId(a))(aid => for {
        _ <- AuthorCrud.update(aid, a).run
      } yield aid)
    } yield authorId

  object CategoryCrud extends DbBasicCrud[Category] with DbSearch[Category] {
    override val getAllItems = sql"select name, uri from category".query[Category]

    override def getById(aid: Int) = sql"select name, uri from category where id = $aid".query[Category]

    def getIdByName(name: String) = sql"select id from category where name = $name".query[Int]

    def update(id: Int, a: Category) = sql"""
        update category set name=${a.name}, uri=${a.uri}
        where id=${id}
      """.update

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

  def createCategoryAndGetId(c: Category) = for {
      _ <- CategoryCrud.create(c).run
      categoryId <- lastVal
    } yield categoryId

  def saveOrUpdateCategory(c: Category) = for {
      maybeCategoryId <- CategoryCrud.getIdByName(c.name).option
      categoryId <- maybeCategoryId.fold(createCategoryAndGetId(c))(cid => for {
        _ <- CategoryCrud.update(cid, c).run
      } yield cid)
    } yield categoryId

  case class ArticleCategoryDb(completearticleid: Int, categoryid: Int)

  object ArticleCategoryCrud extends DbBasicCrud[ArticleCategoryDb] {
    override def getById(aid: Int) = sql"""
        select completearticleid, categoryid from articlecategory where id = $aid
      """.query[ArticleCategoryDb]

    override def create(ac: ArticleCategoryDb) = sql"""
        insert into articlecategory (completearticleid, categoryid)
        values (${ac.completearticleid}, ${ac.categoryid})
      """.update

    override def deleteById(aid: Int) = sql"""
        delete from articlecategory where id = ${aid}
      """.update

    def getCategoriesByCompleteArticleId(a: Int) = sql"""
      select c.name, c.uri
      from category as c, articlecategory as ac
      where ac.completearticleid = $a and c.id = ac.categoryid
      """.query[Category]

    def getAllLinks = sql"""
      select ac.completearticleid, ac.categoryid
      from articlecategory as ac
      """.query[ArticleCategoryDb]

    override val createTable = sql"""
        create table articlecategory (
          id serial primary key,
          completearticleid int not null,
          categoryid int not null
        )
      """.update
  }

  def getCompleteArticleIdsForCategoryId(cid: Int): Query0[Int] = sql"""
        select completearticleid
        from articlecategory
        where categoryid = $cid
    """.query[Int]

  def linkCategory(c: Category, completeArticleId: Int) = for {
      categoryId <- saveOrUpdateCategory(c)
      acdb = ArticleCategoryDb(completeArticleId, categoryId)
      _ <- ArticleCategoryCrud.create(acdb).run
    } yield ()

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

  def createCommentAndGetId(c: Comment) = for {
      _ <- CommentCrud.create(c).run
      commentId <- lastVal
    } yield commentId

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

    def getCompleteComments(aid: Int) = sql"""
        select c.body, c.pubdate, a.name, a.email, a.uri
        from completecomment as cc, comment as c, author as a
        where cc.completearticleid = $aid and cc.commentid = c.id and cc.authorid = a.id
      """.query[CompleteCommentCase]

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

  def createNewCompleteComment(c: CompleteComment, completeArticleId: Int) = for {
      authorId <- saveOrUpdateAuthor(c.author)
      commentId <- createCommentAndGetId(c.comment)
      comment = CompleteCommentDb(completeArticleId, commentId, authorId)
      _ <- CompleteCommentCrud.create(comment).run
      commentId <- lastVal
    } yield commentId

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

  def createArticleAndGetId(c: Article) = for {
      _ <- ArticleCrud.create(c).run
      articleId <- lastVal
    } yield articleId

  case class CompleteArticleDb(articleid: Int, authorid: Int)

  // TODO Sorting? Order by date but the date is in regular article.
  object CompleteArticleCrud extends DbBasicCrud[CompleteArticleDb] with DbSearch[CompleteArticleDb] {
    override def getById(aid: Int) = sql"""
        select articleid, authorid
        from completearticle
        where id = $aid
      """.query[CompleteArticleDb]

    override def create(a: CompleteArticleDb) = sql"""
        insert into completearticle (articleid, authorid)
        values (${a.articleid}, ${a.authorid})
      """.update

    override def deleteById(id: Int) = sql"delete from completearticle where id = ${id}".update

    override def getAllItems = sql"""
        select articleid, authorid from completearticle
      """.query[CompleteArticleDb]

    override val createTable: Update0 = sql"""
        create table completearticle (
          id serial,
          articleid int not null,
          authorid int not null
        )
      """.update
  }

  def getAllCompleteArticleIds: Query0[Int] = sql"""
        select id
        from completearticle
  """.query[Int]


  def createNewCompleteArticleAndGetId(a: CompleteArticleDb) = for {
      _ <- CompleteArticleCrud.create(a).run
      completeArticleId <- lastVal
    } yield completeArticleId
  

  // TODO replace these folds with a sequence or something.
  def linkCategories(cats: List[Category], artId: Int): ConnectionIO[Unit] =
    cats.map(category => linkCategory(category, artId)).fold(().point[ConnectionIO])((x, y) => x >>= ((_) => y))
    
  def createCompleteComments(comments: List[CompleteComment], artId: Int): ConnectionIO[Int] =
    comments.map(comment => createNewCompleteComment(comment, artId))
      .fold((0).point[ConnectionIO])((x, y) => x >>= ((_) => y))

  def persistCompleteArticle(a: CompleteArticle) = for {
      authorId <- saveOrUpdateAuthor(a.author)
      articleId <- createArticleAndGetId(a.article)
      completeArticleDb = CompleteArticleDb(articleId, authorId)
      completeArticleId <- createNewCompleteArticleAndGetId(completeArticleDb)
      _ <- linkCategories(a.categories, completeArticleId)
      _ <- createCompleteComments(a.comments, completeArticleId)
    } yield (completeArticleId)

  def getCompleteCommentByDb(comdb: CompleteCommentDb) = for {
    comment <- CommentCrud.getById(comdb.commentid).unique
    author <- AuthorCrud.getById(comdb.authorid).unique
    item = CompleteCommentCase(comment, author)
  } yield item

  def getAllCompleteArticles(): ConnectionIO[List[CompleteArticleCase]] = for {
    items <- getAllCompleteArticleIds.list
    articles <- getCompleteArticlesByIds(items)
  } yield articles

  def getCompleteArticlesByIds(ids: List[Int]): ConnectionIO[List[CompleteArticleCase]] = (for {
    id <- ids
  } yield getCompleteArticleById(id)).sequence
  
  def getCompleteArticleById(a: Int): ConnectionIO[CompleteArticleCase] = for {
    cad <- CompleteArticleCrud.getById(a).unique
    article <- ArticleCrud.getById(cad.articleid).unique
    author <- AuthorCrud.getById(cad.authorid).unique
    categories <- ArticleCategoryCrud.getCategoriesByCompleteArticleId(cad.articleid).list
    comments <- CompleteCommentCrud.getCompleteComments(a).list
    item = CompleteArticleCase(article, comments, categories, author)
  } yield item

  def initialiseDb = (for {
      _ <- AuthorCrud.createTable.run
      _ <- CategoryCrud.createTable.run
      _ <- CommentCrud.createTable.run
      _ <- CompleteCommentCrud.createTable.run
      _ <- ArticleCrud.createTable.run
      _ <- ArticleCategoryCrud.createTable.run
      _ <- CompleteArticleCrud.createTable.run
    } yield ())

}
