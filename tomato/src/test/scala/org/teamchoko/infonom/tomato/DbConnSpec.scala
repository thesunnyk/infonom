package org.teamchoko.infonom.tomato

import java.net.URI

import org.joda.time.DateTime
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.teamchoko.infonom.carrot.Articles.Article
import org.teamchoko.infonom.carrot.Articles.Author
import org.teamchoko.infonom.carrot.Articles.Category
import org.teamchoko.infonom.carrot.Articles.Comment
import org.teamchoko.infonom.carrot.Articles.Textile
import org.teamchoko.infonom.carrot.Articles.CompleteArticleCase
import org.teamchoko.infonom.carrot.Articles.CompleteCommentCase
import org.teamchoko.infonom.tomato.DbConn.DbBasicCrud
import org.teamchoko.infonom.tomato.DbConn.DbSearch

import DbConn.CompleteArticleDb
import DbConn.ArticleCategoryDb
import DbConn.CompleteCommentDb
import DbConn.DbBasicCrud
import DbConn.DbSearch

import doobie.imports.ConnectionIO
import doobie.imports.DriverManagerTransactor
import doobie.imports.Query0
import doobie.imports.Update0
import doobie.imports.toMoreConnectionIOOps
import scalaz.concurrent.Task
import scalaz.\/-

class DbConnSpec extends FlatSpec with Matchers {
  def xaTest = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test", "sa", "")

  def doBasicCrud[T](crud: DbBasicCrud[T], item: T, item2: T) = {

    it should "create and read an item" in {

      val fromDb = (for {
        _ <- crud.createTable
        itemId <- crud.create(item)
        retrievedItem <- crud.getById(itemId)
      } yield retrievedItem).transact(xaTest).run

      fromDb should contain (item)
    }

    it should "delete an item" in {
      val fromDb = (for {
        _ <- crud.createTable
        itemId <- crud.create(item)
        _ <- crud.deleteById(itemId)
        maybeItem <- crud.getById(itemId)
      } yield maybeItem).transact(xaTest).run

      fromDb should equal(None)
    }

    it should "discern between different items" in {

      val fromDb = (for {
        _ <- crud.createTable
        itemId <- crud.create(item)
        _ <- crud.create(item2)
        retrievedItem <- crud.getById(itemId)
      } yield retrievedItem).transact(xaTest).run

      fromDb should contain (item)
    }

    it should "delete the correct item" in {
      val fromDb = (for {
        _ <- crud.createTable
        itemId <- crud.create(item)
        item2Id <- crud.create(item2)
        _ <- crud.deleteById(itemId)
        maybeItem <- crud.getById(itemId)
        item2 <- crud.getById(item2Id)
      } yield (maybeItem, item2)).transact(xaTest).run

      fromDb should equal((None, Some(item2)))
    }

    it should "pass analysis" in {
      val fromDb = (for {
        _ <- crud.createTable
        _ <- crud.analyse
      } yield ()).transact(xaTest).attemptRun
      fromDb should equal(\/-(()))
    }
  }

  def listAllItems[T](crud: DbSearch[T], item: T, item2: T) = {

    it should "list all items" in {
      val fromDb = (for {
        _ <- crud.createTable
        _ <- crud.create(item)
        _ <- crud.create(item2)
        retrievedItem <- crud.getAllItems
      } yield retrievedItem).transact(xaTest).run

      fromDb should equal(List(item, item2))
    }
  }

  def testGetIdByName[T](crud: DbSearch[T], getIdByName: (String) => ConnectionIO[Option[Int]], item: T,
    itemName: String) {

    it should s"find an item by name" in {
      val (dbActual, dbName) = (for {
        _ <- crud.createTable
        actualId <- crud.create(item)
        nameId <- getIdByName(itemName)
      } yield (actualId, nameId)).transact(xaTest).run

      Some(dbActual) should equal(dbName)
    }

    it should s"not find an item by invalid name" in {
      val fromDb = (for {
        _ <- crud.createTable
        _ <- crud.create(item)
        nameId <- getIdByName("invalidname")
      } yield nameId).transact(xaTest).run

      fromDb should equal(None)
    }
  }

  def testUpdate[T](crud: DbSearch[T], update: (Int, T) => ConnectionIO[Int], item: T, item2: T) {
    it should "update the item" in {
      val fromDb = (for {
        _ <- crud.createTable
        dbid <- crud.create(item)
        _ <- update(dbid, item2)
        item <- crud.getById(dbid)
      } yield item).transact(xaTest).run

      fromDb should equal(Some(item2))
    }
  }

  val author = Author("things", None, new URI("/author/things"))
  val author2 = Author("things", None, new URI("/author/dthing"))

  "Author SQL" should behave like doBasicCrud(DbConn.AuthorCrud, author, author2)
  it should behave like listAllItems(DbConn.AuthorCrud, author, author2)

  it should behave like testGetIdByName(DbConn.AuthorCrud, DbConn.AuthorCrud.getIdByName, author, "things")
  it should behave like testUpdate(DbConn.AuthorCrud, DbConn.AuthorCrud.update, author, author2)

  val category = Category("name", new URI("/test"))
  val category2 = Category("name", new URI("/test/alt"))

  "Category SQL" should behave like doBasicCrud(DbConn.CategoryCrud, category, category2)
  it should behave like listAllItems(DbConn.CategoryCrud, category, category2)

  it should behave like testGetIdByName(DbConn.CategoryCrud, DbConn.CategoryCrud.getIdByName, category, "name")
  it should behave like testUpdate(DbConn.CategoryCrud, DbConn.CategoryCrud.update, category, category2)

  val comment = Comment("text", new DateTime(0))
  val comment2 = Comment("text", new DateTime(1232))

  "Comments SQL" should behave like doBasicCrud(DbConn.CommentCrud, comment, comment2)

  val article = Article("heading", "content", None, new DateTime(0), new URI("/tmp"))
  val article2 = Article("heading", "content 2", None, new DateTime(0), new URI("/tmp"))


  "Article SQL" should behave like doBasicCrud(DbConn.ArticleCrud, article, article2)

  val completeComment = CompleteCommentDb(6, 6, 6)
  val completeComment2 = CompleteCommentDb(6, 7, 8)

  "Complete Comment SQL" should behave like doBasicCrud(DbConn.CompleteCommentCrud, completeComment, completeComment2)

  it should "get comments for article id" in {
    val completeComment3 = CompleteCommentDb(4, 1, 1)

    val crud = DbConn.CompleteCommentCrud

    val fromDb = (for {
      _ <- crud.createTable
      _ <- crud.create(completeComment)
      _ <- crud.create(completeComment2)
      _ <- crud.create(completeComment3)
      retrievedItem <- crud.getForCompleteArticleId(6)
    } yield retrievedItem).transact(xaTest).run

    fromDb should equal(List(completeComment, completeComment2))
  }

  val completeArticle = CompleteArticleDb(6, 6)
  val completeArticle2 = CompleteArticleDb(6, 9)

  "Complete Article SQL" should behave like listAllItems(DbConn.CompleteArticleCrud, completeArticle, completeArticle2)

  val articleCategory = ArticleCategoryDb(4, 5)
  val articleCategory2 = ArticleCategoryDb(4, 8)

  "Article Categories SQL" should behave like doBasicCrud(DbConn.ArticleCategoryCrud, articleCategory, articleCategory2)

  it should "map in categories" in {
    val cat = DbConn.CategoryCrud
    val ac = DbConn.ArticleCategoryCrud

    val retVal = (for {
      _ <- cat.createTable
      _ <- ac.createTable
      cat1Id <- cat.create(category)
      acat = ArticleCategoryDb(3, cat1Id)
      _ <- ac.create(acat)
      cat2Id <- cat.create(category2)
      acat2 = ArticleCategoryDb(3, cat2Id)
      _ <- ac.create(acat2)
      categories <- ac.getCategoriesByCompleteArticleId(3)
    } yield categories).transact(xaTest).run

    retVal should equal(List(category, category2))
  }

  def createACAndCats = for {
    _ <- DbConn.ArticleCategoryCrud.createTable
    _ <- DbConn.CategoryCrud.createTable
  } yield 0

  def createArticlesAndCompleteArticles = for {
    _ <- DbConn.ArticleCrud.createTable
    _ <- DbConn.CompleteArticleCrud.createTable
  } yield 0

  ////////////////// Test the persistence API

  val extArticleSimp = CompleteArticleCase(article, Nil, Nil, author)

  "Complete Article" should "persist correctly for simple value" in {
    val retArt = (for {
      _ <- DbConn.initialiseDb
      articleid <- DbConn.persistCompleteArticle(extArticleSimp)
      art <- DbConn.getCompleteArticleById(articleid)
    } yield art).transact(xaTest).run

    retArt should equal(Some(extArticleSimp))
  }

  val author3 = Author("dthings", None, new URI("/author/dthing"))
  val extComment = CompleteCommentCase(comment, author3)
  val extArticle = CompleteArticleCase(article, List(extComment), List(category), author)

  it should "persist correctly for value with comments and categories" in {
    val retArt = (for {
      _ <- DbConn.initialiseDb
      articleid <- DbConn.persistCompleteArticle(extArticle)
      art <- DbConn.getCompleteArticleById(articleid)
    } yield art).transact(xaTest).run

    retArt should equal(Some(extArticle))
  }

  val altArticle = CompleteArticleCase(article, List(extComment), List(category), author2)

  it should "update author in a new article" in {
    val retArt = (for {
      _ <- DbConn.initialiseDb
      _ <- DbConn.persistCompleteArticle(extArticle)
      articleid <- DbConn.persistCompleteArticle(altArticle)
      art <- DbConn.getCompleteArticleById(articleid)
    } yield art).transact(xaTest).run

    retArt.get.author should equal(author2)
  }

  it should "update author in an old article" in {
    val retArt = (for {
      _ <- DbConn.initialiseDb
      articleid <- DbConn.persistCompleteArticle(extArticle)
      _ <- DbConn.persistCompleteArticle(altArticle)
      art <- DbConn.getCompleteArticleById(articleid)
    } yield art).transact(xaTest).run

    // Author should be updated now.
    retArt.get.author should equal(author2)
  }

  val catArticle = CompleteArticleCase(article, List(extComment), List(category2), author2)

  it should "update category in a new article" in {
    val retArt = (for {
      _ <- DbConn.initialiseDb
      _ <- DbConn.persistCompleteArticle(extArticle)
      articleid <- DbConn.persistCompleteArticle(catArticle)
      art <- DbConn.getCompleteArticleById(articleid)
    } yield art).transact(xaTest).run

    // Author should be updated now.
    retArt.get.categories.head should equal(category2)
  }

  it should "update category in an old article" in {
    val retArt = (for {
      _ <- DbConn.initialiseDb
      articleid <- DbConn.persistCompleteArticle(extArticle)
      _ <- DbConn.persistCompleteArticle(catArticle)
      art <- DbConn.getCompleteArticleById(articleid)
    } yield art).transact(xaTest).run

    // Author should be updated now.
    retArt.get.categories.head should equal(category2)
  }

  val altCategory = Category("altName", new URI("/test"))
  val secArticle = CompleteArticleCase(article, List(extComment), List(category), author3)
  val secArticle2 = CompleteArticleCase(article, List(extComment), List(altCategory), author3)

  // TODO This won't work since CompleteArticle uses Article ID now
  "Category search" should "give all articles by category id" in {

    val retArt = (for {
      _ <- DbConn.initialiseDb
      _ <- DbConn.persistCompleteArticle(extArticle)
      _ <- DbConn.persistCompleteArticle(secArticle)
      _ <- DbConn.persistCompleteArticle(secArticle2)
      cid <- DbConn.CategoryCrud.getIdByName(category.name)
      articleIds <- DbConn.getCompleteArticleIdsForCategoryId(cid.get)
      articles <- DbConn.getCompleteArticlesByIds(articleIds)
    } yield articles).transact(xaTest).run

    retArt should contain(extArticle)
    retArt should contain(secArticle)
    retArt should not contain(secArticle2)
  }

}
