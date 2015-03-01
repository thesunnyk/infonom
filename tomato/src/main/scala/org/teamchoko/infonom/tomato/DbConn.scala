package org.teamchoko.infonom.tomato

import doobie.imports._
import scalaz.concurrent.Task
import java.util.Date

object DbConn {
  val xa = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:test.db", "sa", "")
  
  case class AuthorDb(name: String, email: Option[String], uri: Option[String])

  def getAuthorById(aid: Long) = sql"select name, email, uri from author where id = $aid".query[AuthorDb].list

  val getAuthors = sql"select name, email, uri from author".query[AuthorDb].list
  
  val createAuthorTable: Update0 = sql"""
      create table author {
        id serial,
        name varchar not null
        email varchar,
        uri varchar
      }
    """.update

  case class CategoryDb(name: String, uri: Option[String])

  val getCategory = sql"select name, uri from category".query[CategoryDb].list

  def getCategoryById(aid: Long) = sql"select name, uri from category where id = $aid".query[CategoryDb].list

  val createCategoryTable: Update0 = sql"""
      create table category {
        id serial,
        name varchar,
        uri varchar
      }
    """.update
  
  case class CommentDb(body: String, pubdate: Date)

  def getCommentById(aid: Long) = sql"select body, pubdate from comment where id = $aid".query[CommentDb].list

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

  case class ArticleDb(heading: String, body: String, textfilter: String,
    draft: Boolean, extract: String, pullquote: String, pubdate: Date, uri: String)

  def getArticleById(aid: Long) = sql"""
      select heading, body, textfilter, draft, extract, pullquote, pubdate, uri
      from article
      where id = $aid
    """.query[ArticleDb].list

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
