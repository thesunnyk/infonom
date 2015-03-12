package org.teamchoko.infonom.tomato

import doobie.imports._
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import java.net.URI
import scalaz.concurrent.Task
import DbConn._
import org.teamchoko.infonom.carrot.Articles.{Author, Category, Comment, Article, Textile}
import org.joda.time.DateTime

class DbConnSpec extends FlatSpec with Matchers {
  def xaTest = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test", "sa", "")


  "Doobie Author SQL" should "create table correctly" in {
    val analysis = DbConn.createAuthorTable.analysis.transact(xaTest).run
    analysis.alignmentErrors should equal(Nil)
  }

  it should "typecheck delete authors" in {
    val analysis = (for {
      _ <- DbConn.createAuthorTable.run
      analysisVal <- DbConn.deleteAuthorById(0).analysis
    } yield analysisVal).transact(xaTest).run

    analysis.alignmentErrors should equal(Nil)
  }

  it should "typecheck create authors" in {
    val author = Author("things", None, Some(new URI("/author/things")))
    val analysis = (for {
      _ <- DbConn.createAuthorTable.run
      analysisVal <- DbConn.createAuthor(author).analysis
    } yield analysisVal).transact(xaTest).run
    analysis.alignmentErrors should equal(Nil)
  }

  it should "typecheck get authors" in {
    val analysis = (for {
      _ <- DbConn.createAuthorTable.run
      analysisVal <- DbConn.getAuthors.analysis
    } yield analysisVal).transact(xaTest).run
    analysis.alignmentErrors should equal(Nil)
  }

  it should "typecheck get authors by Id" in {
    val analysis = (for {
      _ <- DbConn.createAuthorTable.run
      analysisVal <- DbConn.getAuthorById(123).analysis
    } yield analysisVal).transact(xaTest).run
    
    analysis.alignmentErrors should equal(Nil)
  }

  it should "leave an author in the db" in {
    val author = Author("things", None, Some(new URI("/author/things")))

    val fromDb: Author = (for {
      _ <- DbConn.createAuthorTable.run
      _ <- DbConn.createAuthor(author).run
      authId <- DbConn.lastVal
      retrievedAuthor <- DbConn.getAuthorById(authId).unique
    } yield retrievedAuthor).transact(xaTest).run

    fromDb should equal(author)
  }

  it should "discern different authors by id" in {
    val authorOne = Author("one", None, Some(new URI("/author/things")))
    val authorTwo = Author("one", None, Some(new URI("/author/dthing")))

    val fromDb: Author = (for {
      _ <- DbConn.createAuthorTable.run
      _ <- DbConn.createAuthor(authorOne).run
      authId <- DbConn.lastVal
      _ <- DbConn.createAuthor(authorTwo).run
      retrievedAuthor <- DbConn.getAuthorById(authId).unique
    } yield retrievedAuthor).transact(xaTest).run

    fromDb should equal(authorOne)
  }

  it should "list all authors" in {
    val authorOne = Author("one", None, Some(new URI("/author/things")))
    val authorTwo = Author("two", None, Some(new URI("/author/things")))

    val fromDb = (for {
      _ <- DbConn.createAuthorTable.run
      _ <- DbConn.createAuthor(authorOne).run
      _ <- DbConn.createAuthor(authorTwo).run
      retrievedAuthor <- DbConn.getAuthors.list
    } yield retrievedAuthor).transact(xaTest).run

    fromDb should equal(List(authorOne, authorTwo))
  }

  it should "delete an author" in {
    val author = Author("one", None, Some(new URI("/author/things")))

    val fromDb = (for {
      _ <- DbConn.createAuthorTable.run
      _ <- DbConn.createAuthor(author).run
      authId <- DbConn.lastVal
      _ <- DbConn.deleteAuthorById(authId).run
      retrievedAuthor <- DbConn.getAuthorById(authId).option
    } yield retrievedAuthor).transact(xaTest).run

    fromDb should equal(None)
  }
  
  it should "delete the correct author" in {
    val authorOne = Author("one", None, Some(new URI("/author/things")))
    val authorTwo = Author("two", None, Some(new URI("/author/things")))

    val fromDb = (for {
      _ <- DbConn.createAuthorTable.run
      _ <- DbConn.createAuthor(authorOne).run
      authId <- DbConn.lastVal
      _ <- DbConn.createAuthor(authorTwo).run
      _ <- DbConn.deleteAuthorById(authId).run
      retrievedAuthor <- DbConn.getAuthors.list
    } yield retrievedAuthor).transact(xaTest).run

    fromDb should equal(List(authorTwo))
  }

  "Doobie Category SQL" should "create category table" in {
    val analysis = DbConn.createCategoryTable.analysis.transact(xaTest).run
    analysis.alignmentErrors should equal(Nil)
  }

  it should "typecheck getCategoryById" in {
    val analysis = (for {
      _ <- DbConn.createCategoryTable.run
      analysisVal <- DbConn.getCategoryById(0).analysis
    } yield analysisVal).transact(xaTest).run

    analysis.alignmentErrors should equal(Nil)
  }

  it should "typecheck getCategories" in {
    val analysis = (for {
      _ <- DbConn.createCategoryTable.run
      analysisVal <- DbConn.getCategories.analysis
    } yield analysisVal).transact(xaTest).run

    analysis.alignmentErrors should equal(Nil)
  }

  it should "typecheck createCategory" in {
    val cat = Category("name", new URI("/test"))
    val analysis = (for {
      _ <- DbConn.createCategoryTable.run
      analysisVal <- DbConn.createCategory(cat).analysis
    } yield analysisVal).transact(xaTest).run

    analysis.alignmentErrors should equal(Nil)
  }

  it should "typecheck deleteCategoryById" in {
    val analysis = (for {
      _ <- DbConn.createCategoryTable.run
      analysisVal <- DbConn.deleteCategoryById(0).analysis
    } yield analysisVal).transact(xaTest).run

    analysis.alignmentErrors should equal(Nil)
  }

  it should "add a category correctly" in {
    val cat = Category("name", new URI("/test"))
    val fromDb = (for {
      _ <- DbConn.createCategoryTable.run
      _ <- DbConn.createCategory(cat).run
      catId <- DbConn.lastVal
      catVal <- DbConn.getCategoryById(catId).unique
    } yield catVal).transact(xaTest).run

    fromDb should equal(cat)
  }

  it should "retrieve category by id unambiguously" in {
    val cat = Category("name", new URI("/test"))
    val cat2 = Category("name", new URI("/test/alt"))
    val fromDb = (for {
      _ <- DbConn.createCategoryTable.run
      _ <- DbConn.createCategory(cat).run
      catId <- DbConn.lastVal
      _ <- DbConn.createCategory(cat2).run
      catVal <- DbConn.getCategoryById(catId).unique
    } yield catVal).transact(xaTest).run

    fromDb should equal(cat)
  }

  it should "get all categories correctly" in {
    val cat = Category("name", new URI("/test"))
    val cat2 = Category("name", new URI("/test/alt"))
    val fromDb = (for {
      _ <- DbConn.createCategoryTable.run
      _ <- DbConn.createCategory(cat).run
      _ <- DbConn.createCategory(cat2).run
      catVal <- DbConn.getCategories.list
    } yield catVal).transact(xaTest).run

    fromDb should equal(List(cat, cat2))
  }

  it should "delete a category" in {
    val cat = Category("name", new URI("/test"))
    val fromDb = (for {
      _ <- DbConn.createCategoryTable.run
      _ <- DbConn.createCategory(cat).run
      catId <- DbConn.lastVal
      _ <- DbConn.deleteCategoryById(catId).run
      catVal <- DbConn.getCategoryById(catId).option
    } yield catVal).transact(xaTest).run

    fromDb should equal(None)
  }

  it should "delete the correct category" in {
    val cat = Category("name", new URI("/test"))
    val cat2 = Category("name", new URI("/test/alt"))
    val fromDb = (for {
      _ <- DbConn.createCategoryTable.run
      _ <- DbConn.createCategory(cat).run
      catId <- DbConn.lastVal
      _ <- DbConn.createCategory(cat2).run
      _ <- DbConn.deleteCategoryById(catId).run
      catVal <- DbConn.getCategories.list
    } yield catVal).transact(xaTest).run

    fromDb should equal(List(cat2))
  }

  "Doobie Comment SQL" should "typecheck create comment table" in {
    val analysis = DbConn.createCommentTable.analysis.transact(xaTest).run
    analysis.alignmentErrors should equal(Nil)
  }

  it should "typecheck create comment" in {
    val comment = Comment("text", new DateTime(0))
    val analysis = (for {
      _ <- DbConn.createCommentTable.run
      analysisVal <- DbConn.createComment(comment).analysis
    } yield analysisVal).transact(xaTest).run

    analysis.alignmentErrors should equal(Nil)
  }

  it should "typecheck getCommentById" in {
    val analysis = (for {
      _ <- DbConn.createCommentTable.run
      analysisVal <- DbConn.getCommentById(0).analysis
    } yield analysisVal).transact(xaTest).run

    analysis.alignmentErrors should equal(Nil)
  }

  it should "add a comment correctly" in {
    val comment = Comment("comment text", new DateTime(123))

    val fromDb = (for {
      _ <- DbConn.createCommentTable.run
      _ <- DbConn.createComment(comment).run
      commentId <- DbConn.lastVal
      commentVal <- DbConn.getCommentById(commentId).unique
    } yield commentVal).transact(xaTest).run

    fromDb should equal(comment)
  }

  it should "retrieve comment by id unambiguously" in {
    val comment = Comment("text", new DateTime(123))
    val comment2 = Comment("text", new DateTime(1232))

    val fromDb = (for {
      _ <- DbConn.createCommentTable.run
      _ <- DbConn.createComment(comment).run
      commentId <- DbConn.lastVal
      _ <- DbConn.createComment(comment2).run
      commentVal <- DbConn.getCommentById(commentId).unique
    } yield commentVal).transact(xaTest).run

    fromDb should equal(comment)
  }

  it should "delete a comment correctly" in {
    val comment = Comment("name", new DateTime(123))
    val comment2 = Comment("name", new DateTime(1232))
    val fromDb = (for {
      _ <- DbConn.createCommentTable.run
      _ <- DbConn.createComment(comment).run
      commentId <- DbConn.lastVal
      _ <- DbConn.createComment(comment2).run
      _ <- DbConn.deleteCommentById(commentId).run
      commentVal <- DbConn.getCommentById(commentId).option
    } yield commentVal).transact(xaTest).run

    fromDb should equal(None)
  }

  // TODO Fix up articles, CompleteComment, and CompleteArticle

  "Doobie Article SQL" should "typecheck create article table" in {
    val analysis = DbConn.createArticleTable.analysis.transact(xaTest).run
    analysis.alignmentErrors should equal(Nil)
  }

  // Being fixed in doobie 0.2.1
  ignore should "typecheck create article" in {
    val article = Article("heading", "content", Textile(), false, None, None, new DateTime(0), new URI("/tmp"))
    val analysis = (for {
      _ <- DbConn.createArticleTable.run
      analysisVal <- DbConn.createArticle(article).analysis
    } yield analysisVal).transact(xaTest).run

    analysis.alignmentErrors should equal(Nil)
  }


}
