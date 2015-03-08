package org.teamchoko.infonom.tomato

import doobie.imports._
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import java.net.URI
import scalaz.concurrent.Task
import DbConn._
import org.teamchoko.infonom.carrot.Articles.{Author, Category, Comment}
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
      table <- DbConn.createAuthorTable.run
      thing <- DbConn.createAuthor(author).run
      authId <- DbConn.lastVal
      retrievedAuthor <- DbConn.getAuthorById(authId).unique
    } yield retrievedAuthor).transact(xaTest).run

    fromDb should equal(author)
  }

  it should "discern different authors by id" in {
    val authorOne = Author("one", None, Some(new URI("/author/things")))
    val authorTwo = Author("two", None, Some(new URI("/author/things")))

    val fromDb: Author = (for {
      _ <- DbConn.createAuthorTable.run
      _ <- DbConn.createAuthor(authorOne).run
      authId <- DbConn.lastVal
      _ <- DbConn.createAuthor(authorTwo).run
      retrievedAuthor <- DbConn.getAuthorById(authId).unique
    } yield retrievedAuthor).transact(xaTest).run

    fromDb should equal(authorOne)
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

  it should "typecheck getCategory" in {
    val analysis = (for {
      _ <- DbConn.createCategoryTable.run
      analysisVal <- DbConn.getCategory.analysis
    } yield analysisVal).transact(xaTest).run

    analysis.alignmentErrors should equal(Nil)
  }

  it should "typecheck addCategory" in {
    val cat = Category("name", new URI("/test"))
    val analysis = (for {
      _ <- DbConn.createCategoryTable.run
      analysisVal <- DbConn.addCategory(cat).analysis
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

  // TODO actually insert and remove categories.

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

  // TODO Actually insert and remove comments.
  // TODO Fix up articles, CompleteComment, and CompleteArticle

}
