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

class DbConnSpec extends FlatSpec with Matchers {
  def xaTest = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test", "sa", "")

  def typecheckCreateTable(table: Update0) = {
    it should "pass analysis for table creation" in {
      val analysis = table.analysis.transact(xaTest).run
      analysis.alignmentErrors should equal(Nil)
    }
  }

  def typecheckQueryTable(query: String, createTable: ConnectionIO[Int], queryTable: Query0[_]) = {
    it should "pass analysis for " + query in {
      val analysis = (for {
        _ <- createTable
        analysisVal <- queryTable.analysis
      } yield analysisVal).transact(xaTest).run

      analysis.alignmentErrors should equal(Nil)
    }
  }

  def typecheckUpdateTable(update: String, createTable: Update0, updateTable: Update0) = {
    it should "pass analysis for " + update in {
      val analysis = (for {
        _ <- createTable.run
        analysisVal <- updateTable.analysis
      } yield analysisVal).transact(xaTest).run

      analysis.alignmentErrors should equal(Nil)
    }
  }

  def doBasicCrud[T](crud: DbBasicCrud[T], item: T, item2: T) = {

    it should "create and read an item" in {

      val fromDb = (for {
        _ <- crud.createTable.run
        _ <- crud.create(item).run
        itemId <- DbConn.lastVal
        retrievedItem <- crud.getById(itemId).unique
      } yield retrievedItem).transact(xaTest).run

      fromDb should equal(item)
    }

    it should "delete an item" in {
      val fromDb = (for {
        _ <- crud.createTable.run
        _ <- crud.create(item).run
        itemId <- DbConn.lastVal
        _ <- crud.deleteById(itemId).run
        maybeItem <- crud.getById(itemId).option
      } yield maybeItem).transact(xaTest).run

      fromDb should equal(None)
    }

    it should "discern between different items" in {

      val fromDb = (for {
        _ <- crud.createTable.run
        _ <- crud.create(item).run
        itemId <- DbConn.lastVal
        _ <- crud.create(item2).run
        retrievedItem <- crud.getById(itemId).unique
      } yield retrievedItem).transact(xaTest).run

      fromDb should equal(item)
    }

    it should "delete the correct item" in {
      val fromDb = (for {
        _ <- crud.createTable.run
        _ <- crud.create(item).run
        itemId <- DbConn.lastVal
        _ <- crud.create(item2).run
        item2Id <- DbConn.lastVal
        _ <- crud.deleteById(itemId).run
        maybeItem <- crud.getById(itemId).option
        item2 <- crud.getById(item2Id).unique
      } yield (maybeItem, item2)).transact(xaTest).run

      fromDb should equal((None, item2))
    }

    it should behave like typecheckCreateTable(crud.createTable)
    it should behave like typecheckUpdateTable("delete", crud.createTable, crud.deleteById(123))
    it should behave like typecheckUpdateTable("create", crud.createTable, crud.create(item))
    it should behave like typecheckQueryTable("get", crud.createTable.run, crud.getById(123))
  }

  def listAllItems[T](crud: DbSearch[T], item: T, item2: T) = {

    it should "list all items" in {
      val fromDb = (for {
        _ <- crud.createTable.run
        _ <- crud.create(item).run
        _ <- crud.create(item2).run
        retrievedItem <- crud.getAllItems.list
      } yield retrievedItem).transact(xaTest).run

      fromDb should equal(List(item, item2))
    }

    it should behave like typecheckQueryTable("get all", crud.createTable.run, crud.getAllItems)
  }

  def testGetIdByName[T](crud: DbSearch[T], getIdByName: (String) => Query0[Int], item: T, itemName: String) {
    it should s"find an item by name" in {
      val (dbActual, dbName) = (for {
        _ <- crud.createTable.run
        _ <- crud.create(item).run
        actualId <- DbConn.lastVal
        nameId <- getIdByName(itemName).unique
      } yield (actualId, nameId)).transact(xaTest).run

      dbActual should equal(dbName)
    }

    it should s"not find an item by invalid name" in {
      val fromDb = (for {
        _ <- crud.createTable.run
        _ <- crud.create(item).run
        nameId <- getIdByName("invalidname").option
      } yield nameId).transact(xaTest).run

      fromDb should equal(None)
    }

    it should behave like typecheckQueryTable("getIdByName", crud.createTable.run,
      getIdByName("invalidname"))
  }

  def testUpdate[T](crud: DbSearch[T], update: (Int, T) => Update0, item: T, item2: T) {
    it should "update the item" in {
      val fromDb = (for {
        _ <- crud.createTable.run
        _ <- crud.create(item).run
        dbid <- DbConn.lastVal
        _ <- update(dbid, item2).run
        item <- crud.getById(dbid).unique
      } yield item).transact(xaTest).run

      fromDb should equal(item2)
    }

    it should behave like typecheckUpdateTable("update", crud.createTable, update(0, item))
  }

  val author = Author("things", None, Some(new URI("/author/things")))
  val author2 = Author("things", None, Some(new URI("/author/dthing")))

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

  val article = Article("heading", "content", Textile(), false, None, None, new DateTime(0), new URI("/tmp"))
  val article2 = Article("heading", "content 2", Textile(), false, None, None, new DateTime(0), new URI("/tmp"))


  "Article SQL" should behave like doBasicCrud(DbConn.ArticleCrud, article, article2)

  val completeComment = CompleteCommentDb(6, 6, 6)
  val completeComment2 = CompleteCommentDb(6, 7, 8)

  "Complete Comment SQL" should behave like doBasicCrud(DbConn.CompleteCommentCrud, completeComment, completeComment2)

  it should behave like typecheckQueryTable("get for articleid", DbConn.CompleteCommentCrud.createTable.run,
    DbConn.CompleteCommentCrud.getForCompleteArticleId(0))

  it should "get comments for article id" in {
    val completeComment3 = CompleteCommentDb(4, 1, 1)

    val crud = DbConn.CompleteCommentCrud

    val fromDb = (for {
      _ <- crud.createTable.run
      _ <- crud.create(completeComment).run
      _ <- crud.create(completeComment2).run
      _ <- crud.create(completeComment3).run
      retrievedItem <- crud.getForCompleteArticleId(6).list
    } yield retrievedItem).transact(xaTest).run

    fromDb should equal(List(completeComment, completeComment2))
  }

  val completeArticle = CompleteArticleDb(6, 6)
  val completeArticle2 = CompleteArticleDb(6, 9)

  "Complete Article SQL" should behave like doBasicCrud(DbConn.CompleteArticleCrud, completeArticle, completeArticle2)

  it should behave like listAllItems(DbConn.CompleteArticleCrud, completeArticle, completeArticle2)

  val articleCategory = ArticleCategoryDb(4, 5)
  val articleCategory2 = ArticleCategoryDb(4, 8)

  "Article Categories SQL" should behave like doBasicCrud(DbConn.ArticleCategoryCrud, articleCategory, articleCategory2)

  it should "map in categories" in {
    val cat = DbConn.CategoryCrud
    val ac = DbConn.ArticleCategoryCrud

    val retVal = (for {
      _ <- cat.createTable.run
      _ <- ac.createTable.run
      _ <- cat.create(category).run
      cat1Id <- DbConn.lastVal
      acat = ArticleCategoryDb(3, cat1Id)
      _ <- ac.create(acat).run
      _ <- cat.create(category2).run
      cat2Id <- DbConn.lastVal
      acat2 = ArticleCategoryDb(3, cat2Id)
      _ <- ac.create(acat2).run
      categories <- ac.getCategoriesByCompleteArticleId(3).list
    } yield categories).transact(xaTest).run

    retVal should equal(List(category, category2))
  }

  def createACAndCats = for {
    _ <- DbConn.ArticleCategoryCrud.createTable.run
    _ <- DbConn.CategoryCrud.createTable.run
  } yield 0

  it should behave like typecheckQueryTable("getCategoriesByCompleteArticleId", createACAndCats,
      DbConn.ArticleCategoryCrud.getCategoriesByCompleteArticleId(123))

  ////////////////// Test the persistence API

  val extArticleSimp = CompleteArticleCase(article, Nil, Nil, author)

  "Complete Article" should "persist correctly for simple value" in {
    val retArt = (for {
      _ <- DbConn.initialiseDb
      articleid <- DbConn.persistCompleteArticle(extArticleSimp)
      art <- DbConn.getCompleteArticleById(articleid)
    } yield art).transact(xaTest).run

    retArt should equal(extArticleSimp)
  }

  val author3 = Author("dthings", None, Some(new URI("/author/dthing")))
  val extComment = CompleteCommentCase(comment, author3)
  val extArticle = CompleteArticleCase(article, List(extComment), List(category), author)

  it should "persist correctly for value with comments and categories" in {
    val retArt = (for {
      _ <- DbConn.initialiseDb
      articleid <- DbConn.persistCompleteArticle(extArticle)
      art <- DbConn.getCompleteArticleById(articleid)
    } yield art).transact(xaTest).run

    retArt should equal(extArticle)
  }

  val altArticle = CompleteArticleCase(article, List(extComment), List(category), author2)

  ignore should "update author in a new article" in {
    val retArt = (for {
      _ <- DbConn.initialiseDb
      _ <- DbConn.persistCompleteArticle(extArticle)
      articleid <- DbConn.persistCompleteArticle(altArticle)
      art <- DbConn.getCompleteArticleById(articleid)
    } yield art).transact(xaTest).run

    retArt.author should equal(author2)
  }

  ignore should "update author in an old article" in {
    val retArt = (for {
      _ <- DbConn.initialiseDb
      articleid <- DbConn.persistCompleteArticle(extArticle)
      _ <- DbConn.persistCompleteArticle(altArticle)
      art <- DbConn.getCompleteArticleById(articleid)
    } yield art).transact(xaTest).run

    // Author should be updated now.
    retArt.author should equal(author2)
  }

  val catArticle = CompleteArticleCase(article, List(extComment), List(category2), author2)

  ignore should "update category in a new article" in {
    val retArt = (for {
      _ <- DbConn.initialiseDb
      _ <- DbConn.persistCompleteArticle(extArticle)
      articleid <- DbConn.persistCompleteArticle(catArticle)
      art <- DbConn.getCompleteArticleById(articleid)
    } yield art).transact(xaTest).run

    // Author should be updated now.
    retArt.categories.head should equal(category2)
  }

  ignore should "update category in an old article" in {
    val retArt = (for {
      _ <- DbConn.initialiseDb
      articleid <- DbConn.persistCompleteArticle(extArticle)
      _ <- DbConn.persistCompleteArticle(catArticle)
      art <- DbConn.getCompleteArticleById(articleid)
    } yield art).transact(xaTest).run

    // Author should be updated now.
    retArt.categories.head should equal(category2)
  }

  // TODO test complete article from category ID
  // TODO test complete categories from article ID

}
