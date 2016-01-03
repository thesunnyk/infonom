package org.teamchoko.infonom.tomato

import doobie.imports.ConnectionIO
import doobie.imports.DriverManagerTransactor
import doobie.imports.Meta
import doobie.imports.Query0
import doobie.imports.toMoreConnectionIOOps
import doobie.imports.toSqlInterpolator
import doobie.imports.Update0
import java.net.URI
import java.sql.Timestamp
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
import scala.reflect.runtime.universe
import scalaz._
import scalaz.concurrent.Task
import scalaz.Scalaz._


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

  val lastVal: ConnectionIO[Int] = sql"select lastval()".query[Int].unique

  trait DbBasicCrud[T] {
    def analyse: ConnectionIO[Unit]
    def getById(id: Int): ConnectionIO[Option[T]]
    def create(a: T): ConnectionIO[Int]
    def deleteById(id: Int): ConnectionIO[Int]
    def createTable: ConnectionIO[Int]
  }

  trait DbSearch[T] extends DbBasicCrud[T] {
    def getAllItems: ConnectionIO[List[T]]
  }

  object AuthorCrud extends DbBasicCrud[Author] with DbSearch[Author] {
    val testAuthor = Author("", None, None)
    override def analyse: ConnectionIO[Unit] = for {
      _ <- getByIdSql(0).analysis
      _ <- getIdByNameSql("").analysis
      _ <- getAllItemsSql.analysis
      _ <- createSql(testAuthor).analysis
      _ <- updateSql(0, testAuthor).analysis
      _ <- deleteByIdSql(0).analysis
      _ <- createTableSql.analysis
    } yield ()

    override def getById(aid: Int) = getByIdSql(aid).option

    private def getByIdSql(aid: Int) =
      sql"select name, email, uri from author where id = $aid".query[Author]

    def getIdByName(name: String): ConnectionIO[Option[Int]] = getIdByNameSql(name).option

    private def getIdByNameSql(name: String) =
      sql"select id from author where name = $name".query[Int]

    override def getAllItems: ConnectionIO[List[Author]] = getAllItemsSql.list

    private val getAllItemsSql = sql"select name, email, uri from author".query[Author]

    override def create(a: Author) = createSql(a).run *> lastVal

    private def createSql(a: Author) = sql"""
        insert into author (name, email, uri)
        values (${a.name}, ${a.email}, ${a.uri})
      """.update

    def update(id: Int, a: Author): ConnectionIO[Int] =
      updateSql(id, a).run *> id.point[ConnectionIO]

    private def updateSql(id: Int, a: Author): Update0 = sql"""
        update author set name=${a.name}, email=${a.email}, uri=${a.uri}
        where id=${id}
      """.update

    override def deleteById(aid: Int) = deleteByIdSql(aid).run

    private def deleteByIdSql(aid: Int) = sql"delete from author where id = ${aid}".update

    override def createTable = createTableSql.run

    private val createTableSql = sql"""
        create table author (
          id serial primary key,
          name varchar not null,
          email varchar,
          uri varchar
        )
      """.update
  }

  def saveOrUpdateAuthor(a: Author): ConnectionIO[Int] = for {
      maybeAuthorId <- AuthorCrud.getIdByName(a.name)
      authorId <- maybeAuthorId.fold(AuthorCrud.create(a))(aid =>
          AuthorCrud.update(aid, a)
        )
    } yield authorId

  object CategoryCrud extends DbBasicCrud[Category] with DbSearch[Category] {
    val testCategory = Category("", new URI(""))

    override def analyse: ConnectionIO[Unit] = for {
      _ <- getByIdSql(0).analysis
      _ <- getIdByNameSql("").analysis
      _ <- getAllItemsSql.analysis
      _ <- createSql(testCategory).analysis
      _ <- updateSql(0, testCategory).analysis
      _ <- deleteByIdSql(0).analysis
      _ <- createTableSql.analysis
    } yield ()

    override def getAllItems = getAllItemsSql.list

    def getAllItemsSql: Query0[Category] = sql"select name, uri from category".query[Category]

    override def getById(aid: Int) = getByIdSql(aid).option

    def getByIdSql(aid: Int): Query0[Category] =
      sql"select name, uri from category where id = $aid".query[Category]

    def getIdByName(name: String): ConnectionIO[Option[Int]] = getIdByNameSql(name).option

    def getIdByNameSql(name: String): Query0[Int] =
      sql"select id from category where name = $name".query[Int]

    def update(id: Int, a: Category) = updateSql(id, a).run

    def updateSql(id: Int, a: Category): Update0 = sql"""
        update category set name=${a.name}, uri=${a.uri}
        where id=${id}
      """.update

    override def create(cat: Category) = createSql(cat).run *> lastVal

    def createSql(cat: Category) = sql"""
        insert into category (name, uri)
        values (${cat.name}, ${cat.uri})
      """.update

    override def deleteById(aid: Int) = deleteByIdSql(aid).run

    def deleteByIdSql(aid: Int) = sql"""
        delete from category where id = ${aid}
      """.update

    override def createTable = createTableSql.run

    def createTableSql = sql"""
        create table category (
          id serial primary key,
          name varchar not null,
          uri varchar not null
        )
      """.update
  }

  def saveOrUpdateCategory(c: Category): ConnectionIO[Int] = for {
      maybeCategoryId <- CategoryCrud.getIdByName(c.name)
      categoryId <- maybeCategoryId.fold(CategoryCrud.create(c)) { cid =>
          CategoryCrud.update(cid, c).map(_ => cid)
      }
    } yield categoryId

  case class ArticleCategoryDb(completearticleid: Int, categoryid: Int)

  object ArticleCategoryCrud extends DbBasicCrud[ArticleCategoryDb] {
    val testAC = ArticleCategoryDb(0, 0)
    override def analyse: ConnectionIO[Unit] = for {
      _ <- getByIdSql(0).analysis
      _ <- createSql(testAC).analysis
      _ <- deleteByIdSql(0).analysis
      _ <- getCategoriesByCompleteArticleIdSql(0).analysis
      _ <- getAllLinksSql.analysis
      _ <- createTableSql.analysis
    } yield ()

    override def getById(aid: Int) =
      getByIdSql(aid).option

    def getByIdSql(aid: Int) = sql"""
        select completearticleid, categoryid from articlecategory where id = $aid
      """.query[ArticleCategoryDb]

    override def create(ac: ArticleCategoryDb) = createSql(ac).run

    def createSql(ac: ArticleCategoryDb): Update0 = sql"""
        insert into articlecategory (completearticleid, categoryid)
        values (${ac.completearticleid}, ${ac.categoryid})
      """.update

    override def deleteById(aid: Int) = deleteByIdSql(aid).run

    def deleteByIdSql(aid: Int): Update0 = sql"""
        delete from articlecategory where id = ${aid}
      """.update

    def getCategoriesByCompleteArticleId(a: Int): ConnectionIO[List[Category]] =
      getCategoriesByCompleteArticleIdSql(a).list

    def getCategoriesByCompleteArticleIdSql(a: Int): Query0[Category] = sql"""
      select c.name, c.uri
      from category as c, articlecategory as ac
      where ac.completearticleid = $a and c.id = ac.categoryid
      """.query[Category]

    def getAllLinks: ConnectionIO[List[ArticleCategoryDb]] = getAllLinksSql.list

    def getAllLinksSql: Query0[ArticleCategoryDb] = sql"""
      select ac.completearticleid, ac.categoryid
      from articlecategory as ac
      """.query[ArticleCategoryDb]

    override def createTable = createTableSql.run

    def createTableSql: Update0 = sql"""
        create table articlecategory (
          id serial primary key,
          completearticleid int not null,
          categoryid int not null
        )
      """.update
  }

  def getCompleteArticleIdsForCategoryId(cid: Int): ConnectionIO[List[Int]] =
    getCompleteArticleIdsForCategoryIdSql(cid).list

  def getCompleteArticleIdsForCategoryIdSql(cid: Int): Query0[Int] = sql"""
        select completearticleid
        from articlecategory
        where categoryid = $cid
    """.query[Int]

  def linkCategory(c: Category, completeArticleId: Int): ConnectionIO[Unit] = for {
      categoryId <- saveOrUpdateCategory(c)
      acdb = ArticleCategoryDb(completeArticleId, categoryId)
      _ <- ArticleCategoryCrud.create(acdb)
    } yield ()

  object CommentCrud extends DbBasicCrud[Comment] {
    val testComment = Comment("", new DateTime())
    override def analyse: ConnectionIO[Unit] = for {
      _ <- getByIdSql(0).analysis
      _ <- createSql(testComment).analysis
      _ <- deleteByIdSql(0).analysis
      _ <- createTableSql.analysis
    } yield ()

    override def getById(aid: Int) = getByIdSql(aid).option

    def getByIdSql(aid: Int): Query0[Comment] = sql"select body, pubdate from comment where id = $aid".query[Comment]

    override def deleteById(aid: Int) = deleteByIdSql(aid).run

    def deleteByIdSql(aid: Int): Update0 = sql"delete from comment where id = $aid".update

    override def create(c: Comment) = createSql(c).run

    def createSql(c: Comment) = sql"""
        insert into comment (body, pubdate)
        values (${c.text}, ${c.pubDate})
      """.update

    override def createTable = createTableSql.run

    // TODO body should be text, and we should have a way of reading / writing it
    def createTableSql: Update0 = sql"""
        create table comment (
          id serial primary key,
          body longvarchar not null,
          pubdate timestamp not null
        )
      """.update
  }

  case class CompleteCommentDb(completearticleid: Int, commentid: Int, authorid: Int)

  object CompleteCommentCrud extends DbBasicCrud[CompleteCommentDb] {
    val testCompleteComment = CompleteCommentDb(0, 0, 0)
    override def analyse: ConnectionIO[Unit] = for {
      _ <- getByIdSql(0).analysis
      _ <- createSql(testCompleteComment).analysis
      _ <- deleteByIdSql(0).analysis
      _ <- getCompleteCommentsSql(0).analysis
      _ <- getForCompleteArticleIdSql(0).analysis
      _ <- createTableSql.analysis
    } yield ()

    override def create(c: CompleteCommentDb) = createSql(c).run
    
    def createSql(c: CompleteCommentDb): Update0 = sql"""
        insert into completecomment (completearticleid, commentid, authorid)
          values (${c.completearticleid}, ${c.commentid}, ${c.authorid})
        """.update

    override def deleteById(aid: Int) = deleteByIdSql(aid).run

    def deleteByIdSql(aid: Int): Update0 = sql"delete from completecomment where id = $aid".update

    override def getById(aid: Int) = getByIdSql(aid).option

    def getByIdSql(aid: Int) = sql"""
        select completearticleid, commentid, authorid
        from completecomment
        where id = $aid
      """.query[CompleteCommentDb]

    def getCompleteComments(aid: Int): ConnectionIO[List[CompleteCommentCase]] =
      getCompleteCommentsSql(aid).list

    def getCompleteCommentsSql(aid: Int): Query0[CompleteCommentCase] = sql"""
        select c.body, c.pubdate, a.name, a.email, a.uri
        from completecomment as cc, comment as c, author as a
        where cc.completearticleid = $aid and cc.commentid = c.id and cc.authorid = a.id
      """.query[CompleteCommentCase]

    def getForCompleteArticleId(aid: Int): ConnectionIO[List[CompleteCommentDb]] =
      getForCompleteArticleIdSql(aid).list

    def getForCompleteArticleIdSql(aid: Int): Query0[CompleteCommentDb] = sql"""
        select completearticleid, commentid, authorid
        from completecomment
        where completearticleid = $aid
      """.query[CompleteCommentDb]

    override def createTable = createTableSql.run

    def createTableSql: Update0 = sql"""
        create table completecomment (
          id serial,
          completearticleid int not null,
          commentid int not null,
          authorid int not null
        )
      """.update
  }

  def createNewCompleteComment(c: CompleteComment, completeArticleId: Int): ConnectionIO[Int] = for {
      authorId <- saveOrUpdateAuthor(c.author)
      commentId <- CommentCrud.create(c.comment)
      comment = CompleteCommentDb(completeArticleId, commentId, authorId)
      _ <- CompleteCommentCrud.create(comment)
      commentId <- lastVal
    } yield commentId

  object ArticleCrud extends DbBasicCrud[Article] {
    val testArticle = Article("", "", None, new DateTime(), new URI(""))
    override def analyse: ConnectionIO[Unit] = for {
      _ <- getByIdSql(0).analysis
      _ <- getIdByUriSql(new URI("/")).analysis
      _ <- getAllArticleIdsSql.analysis
      _ <- createSql(testArticle).analysis
      _ <- deleteByIdSql(0).analysis
      _ <- createTableSql.analysis
    } yield ()

    override def getById(aid: Int) = getByIdSql(aid).option

    def getByIdSql(aid: Int): Query0[Article] = sql"""
        select heading, body, extract, pubdate, uri
        from article
        where id = $aid
      """.query[Article]

    def getIdByUri(uri: URI): ConnectionIO[Option[Int]] = getIdByUriSql(uri).option

    def getIdByUriSql(uri: URI): Query0[Int] = sql"""
        select id
        from article
        where uri = ${uri}
      """.query[Int]

    def getAllArticleIds: ConnectionIO[List[Int]] = getAllArticleIdsSql.list

    def getAllArticleIdsSql: Query0[Int] = sql"""
        select id
        from article
        order by pubdate desc
    """.query[Int]

    override def create(a: Article) = createSql(a).run

    def createSql(a: Article): Update0 = sql"""
        insert into article (heading, body, extract, pubdate, uri)
        values (${a.heading}, ${a.text}, ${a.extract}, ${a.pubDate}, ${a.uri})
      """.update

    override def deleteById(aid: Int) = deleteByIdSql(aid).run

    def deleteByIdSql(aid: Int): Update0 = sql"delete from article where id = $aid".update

    override def createTable = createTableSql.run

    // TODO body should be text, and we should have a way of reading / writing it
    def createTableSql: Update0 = sql"""
        create table article (
          id serial,
          heading varchar not null,
          body longvarchar not null,
          extract longvarchar,
          pubdate timestamp not null,
          uri varchar not null
        )
      """.update
  }

  case class CompleteArticleDb(articleid: Int, authorid: Int)

  // TODO Sorting? Order by date but the date is in regular article.
  object CompleteArticleCrud extends DbSearch[CompleteArticleDb] {
    val testCompleteArticle = CompleteArticleDb(0, 0)
    override def analyse: ConnectionIO[Unit] = for {
      _ <- getByIdSql(0).analysis
      _ <- createSql(testCompleteArticle).analysis
      _ <- deleteByIdSql(0).analysis
      _ <- getAllItemsSql.analysis
      _ <- createTableSql.analysis
    } yield ()

    def getById(aid: Int): ConnectionIO[Option[CompleteArticleDb]] = getByIdSql(aid).option

    def getByIdSql(aid: Int): Query0[CompleteArticleDb] = sql"""
        select articleid, authorid
        from completearticle
        where articleid = $aid
      """.query[CompleteArticleDb]

    def create(a: CompleteArticleDb): ConnectionIO[Int] =
      createSql(a).run

    def createSql(a: CompleteArticleDb): Update0 = sql"""
        insert into completearticle (articleid, authorid)
        values (${a.articleid}, ${a.authorid})
      """.update

    def deleteById(id: Int): ConnectionIO[Int] =
      deleteByIdSql(id).run

    def deleteByIdSql(id: Int): Update0 = sql"delete from completearticle where articleid = ${id}".update

    def getAllItems: ConnectionIO[List[CompleteArticleDb]] = getAllItemsSql.list

    def getAllItemsSql: Query0[CompleteArticleDb] = sql"""
        select articleid, authorid from completearticle
      """.query[CompleteArticleDb]

    def createTable: ConnectionIO[Int] = createTableSql.run

    def createTableSql: Update0 = sql"""
        create table completearticle (
          articleid int not null,
          authorid int not null
        )
      """.update
  }

  def linkCategories(cats: List[Category], artId: Int): ConnectionIO[Unit] =
    cats.traverse(category => linkCategory(category, artId)).map(_ => ())
    
  def createCompleteComments(comments: List[CompleteComment], artId: Int): ConnectionIO[Unit] =
    comments.traverse(comment => createNewCompleteComment(comment, artId)).map(_ => ())

  def persistCompleteArticle(a: CompleteArticle): ConnectionIO[Int] = for {
      authorId <- saveOrUpdateAuthor(a.author)
      articleId <- ArticleCrud.create(a.article)
      completeArticleDb = CompleteArticleDb(articleId, authorId)
      completeArticleId <- CompleteArticleCrud.create(completeArticleDb)
      _ <- linkCategories(a.categories, completeArticleId)
      _ <- createCompleteComments(a.comments, completeArticleId)
    } yield (completeArticleId)

  def getCompleteCommentByDb(comdb: CompleteCommentDb): ConnectionIO[Option[CompleteCommentCase]] = (for {
    comment <- OptionT(CommentCrud.getById(comdb.commentid))
    author <- OptionT(AuthorCrud.getById(comdb.authorid))
    item = CompleteCommentCase(comment, author)
  } yield item).run

  def getAllCompleteArticles(): ConnectionIO[List[CompleteArticleCase]] = for {
    items <- ArticleCrud.getAllArticleIds
    articles <- getCompleteArticlesByIds(items)
  } yield articles

  def getCompleteArticlesByIds(ids: List[Int]): ConnectionIO[List[CompleteArticleCase]] =
    ids.traverse(getCompleteArticleById).map(_.flatten)
  
  def getCompleteArticleById(a: Int): ConnectionIO[Option[CompleteArticleCase]] = (for {
    cad <- OptionT(CompleteArticleCrud.getById(a))
    article <- OptionT(ArticleCrud.getById(cad.articleid))
    author <- OptionT(AuthorCrud.getById(cad.authorid))
    categories <- ArticleCategoryCrud.getCategoriesByCompleteArticleId(cad.articleid).liftM[OptionT]
    comments <- CompleteCommentCrud.getCompleteComments(a).liftM[OptionT]
    item = CompleteArticleCase(article, comments, categories, author)
  } yield item).run

  val cruds: List[DbBasicCrud[_]] = List(AuthorCrud, CategoryCrud, CommentCrud, CompleteCommentCrud,
    ArticleCrud, ArticleCategoryCrud, CompleteArticleCrud)

  def initialiseDb: ConnectionIO[Unit] = cruds.traverse(_.createTable).map(_ => ())
}
