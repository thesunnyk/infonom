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
import org.teamchoko.infonom.tomato.DbConn.DbBasicCrud
import org.teamchoko.infonom.tomato.DbConn.DbSearch

import DbConn.CompleteArticleDb
import DbConn.ArticleCategoryDb
import DbConn.CompleteCommentDb
import DbConn.DbBasicCrud
import DbConn.DbSearch
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

  def typecheckQueryTable(query: String, createTable: Update0, queryTable: Query0[_],
    ignoreTypechecking: Boolean = false) = {

    if (ignoreTypechecking) {
      ignore should "pass analysis for " + query in {
        val analysis = (for {
          _ <- createTable.run
          analysisVal <- queryTable.analysis
        } yield analysisVal).transact(xaTest).run

        analysis.alignmentErrors should equal(Nil)
      }
    } else {
      it should "pass analysis for " + query in {
        val analysis = (for {
          _ <- createTable.run
          analysisVal <- queryTable.analysis
        } yield analysisVal).transact(xaTest).run

        analysis.alignmentErrors should equal(Nil)
      }
    }
  }

  def typecheckUpdateTable(update: String, createTable: Update0, updateTable: Update0,
    ignoreTypechecking: Boolean = false) = {

    if (ignoreTypechecking) {
	    ignore should "pass analysis for " + update in {
	      val analysis = (for {
	        _ <- createTable.run
	        analysisVal <- updateTable.analysis
	      } yield analysisVal).transact(xaTest).run
	
	      analysis.alignmentErrors should equal(Nil)
	    }
    } else {
	    it should "pass analysis for " + update in {
	      val analysis = (for {
	        _ <- createTable.run
	        analysisVal <- updateTable.analysis
	      } yield analysisVal).transact(xaTest).run
	
	      analysis.alignmentErrors should equal(Nil)
	    }
    }

  }

  def doBasicCrud[T](crud: DbBasicCrud[T], item: T, item2: T, ignoreTypechecking: Boolean = false) = {

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
    it should behave like typecheckUpdateTable("create", crud.createTable, crud.create(item), ignoreTypechecking)
    it should behave like typecheckQueryTable("get", crud.createTable, crud.getById(123), ignoreTypechecking)
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

    it should behave like typecheckQueryTable("get all", crud.createTable, crud.getAllItems)
  }

  val author = Author("things", None, Some(new URI("/author/things")))
  val author2 = Author("things", None, Some(new URI("/author/dthing")))

  "Author SQL" should behave like doBasicCrud(DbConn.AuthorCrud, author, author2)
  it should behave like listAllItems(DbConn.AuthorCrud, author, author2)

  val category = Category("name", new URI("/test"))
  val category2 = Category("name", new URI("/test/alt"))

  "Category SQL" should behave like doBasicCrud(DbConn.CategoryCrud, category, category2)
  it should behave like listAllItems(DbConn.CategoryCrud, category, category2)

  val comment = Comment("text", new DateTime(0))
  val comment2 = Comment("text", new DateTime(1232))

  "Comments SQL" should behave like doBasicCrud(DbConn.CommentCrud, comment, comment2)

  val article = Article("heading", "content", Textile(), false, None, None, new DateTime(0), new URI("/tmp"))
  val article2 = Article("heading", "content 2", Textile(), false, None, None, new DateTime(0), new URI("/tmp"))


  "Article SQL" should behave like doBasicCrud(DbConn.ArticleCrud, article, article2, true)

  val completeComment = CompleteCommentDb(6, 6, 6)
  val completeComment2 = CompleteCommentDb(6, 7, 8)

  "Complete Comment SQL" should behave like doBasicCrud(DbConn.CompleteCommentCrud, completeComment, completeComment2)

  it should behave like typecheckQueryTable("get for articleid", DbConn.CompleteCommentCrud.createTable,
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
  // TODO Test getIdByName for Author and category
  // TODO Test updating author and category
  // TODO test persist complete article
  // TODO test complete article from category ID
  // TODO test complete categories from article ID

}
