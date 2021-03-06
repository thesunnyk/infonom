extern crate serde;
extern crate serde_json;

use model::CompleteArticle;
use model::CompleteComment;
use model::Author;
use model::Category;
use model::Comment;
use model::ArticleChunk;
use model::Article;
use model::LocalDateTime;

use chrono::datetime::DateTime;
use chrono::offset::local::Local;

impl serde::Serialize for Author {
    fn serialize<S>(&self, serializer: &mut S) -> Result<(), S::Error>
        where S: serde::Serializer
    {
        serializer.serialize_struct("Author", ObjectVisitor::new(self))
    }
}

struct ObjectVisitor<'a, T: 'a> {
    value: &'a T,
    state: u8
}

impl<'a, T> ObjectVisitor<'a, T> {
    fn new(t: &'a T) -> ObjectVisitor<'a, T> {
        ObjectVisitor {
            value: t,
            state: 0
        }
    }
}

impl<'a> serde::ser::MapVisitor for ObjectVisitor<'a, Author> {
    fn visit<S>(&mut self, serializer: &mut S) -> Result<Option<()>, S::Error>
        where S: serde::Serializer
    {
        match self.state {
            0 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("name", &self.value.name))))
            }
            1 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("email", &self.value.email))))
            }
            2 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("uri", &self.value.uri))))
            }
            _ => { Ok(None) }
        }
    }
}

impl serde::Deserialize for Author {
    fn deserialize<D>(deserializer: &mut D) -> Result<Author, D::Error>
        where D: serde::Deserializer
    {
        static FIELDS: &'static [&'static str] = &["name", "email", "uri"];
        deserializer.deserialize_struct("Author", FIELDS, AuthorVisitor)
    }
}

struct AuthorVisitor;

impl serde::de::Visitor for AuthorVisitor {
    type Value = Author;

    fn visit_map<V>(&mut self, mut visitor: V) -> Result<Author, V::Error>
        where V: serde::de::MapVisitor
    {
        let mut name = None;
        let mut email = None;
        let mut uri = None;

        loop {
            let key = try!(visitor.visit_key::<String>());
            match key.iter().next().map(|x| x.as_ref()) {
                Some("name") => { name = try!(visitor.visit_value()); }
                Some("email") => { email = try!(visitor.visit_value()); }
                Some("uri") => { uri = try!(visitor.visit_value()); }
                Some(_) => { /* ignore extra fields. */ }
                None => { break; }
            }
        }

        let name = match name {
            Some(x) => x,
            None => try!(visitor.missing_field("name"))
        };

        try!(visitor.end());

        Ok(Author::new(name, email, uri))
    }
}

impl serde::Serialize for Category {
    fn serialize<S>(&self, serializer: &mut S) -> Result<(), S::Error>
        where S: serde::Serializer
    {
        serializer.serialize_struct("Category", ObjectVisitor::new(self))
    }
}

impl<'a> serde::ser::MapVisitor for ObjectVisitor<'a, Category> {
    fn visit<S>(&mut self, serializer: &mut S) -> Result<Option<()>, S::Error>
        where S: serde::Serializer
    {
        match self.state {
            0 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("name", &self.value.name))))
            }
            1 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("uri", &self.value.uri))))
            }
            _ => { Ok(None) }
        }
    }
}

impl serde::Deserialize for Category {
    fn deserialize<D>(deserializer: &mut D) -> Result<Category, D::Error>
        where D: serde::Deserializer
    {
        static FIELDS: &'static [&'static str] = &["name", "uri"];
        deserializer.deserialize_struct("Category", FIELDS, CategoryVisitor)
    }
}

struct CategoryVisitor;

impl serde::de::Visitor for CategoryVisitor {
    type Value = Category;

    fn visit_map<V>(&mut self, mut visitor: V) -> Result<Category, V::Error>
        where V: serde::de::MapVisitor
    {
        let mut name = None;
        let mut uri = None;

        loop {
            let key = try!(visitor.visit_key::<String>());
            match key.iter().next().map(|x| x.as_ref()) {
                Some("name") => { name = try!(visitor.visit_value()); }
                Some("uri") => { uri = try!(visitor.visit_value()); }
                Some(_) => { /* ignore extra fields. */ }
                None => { break; }
            }
        }

        let name = match name {
            Some(x) => x,
            None => try!(visitor.missing_field("name"))
        };

        let uri = match uri {
            Some(x) => x,
            None => try!(visitor.missing_field("uri"))
        };

        try!(visitor.end());

        Ok(Category::new(name, uri))
    }
}

impl serde::Serialize for LocalDateTime {
    fn serialize<S>(&self, serializer: &mut S) -> Result<(), S::Error>
        where S: serde::Serializer
    {
        serializer.serialize_str(&self.datetime.to_rfc3339())
    }
}

impl serde::Deserialize for LocalDateTime {
    fn deserialize<D>(deserializer: &mut D) -> Result<LocalDateTime, D::Error>
        where D: serde::Deserializer
    {
        deserializer.deserialize_string(DateVisitor)
    }
}

struct DateVisitor;

impl serde::de::Visitor for DateVisitor {
    type Value = LocalDateTime;

    fn visit_str<E>(&mut self, val: &str) -> Result<LocalDateTime, E>
        where E: serde::de::Error
    {
        DateTime::parse_from_rfc3339(val)
            .map(|dt| LocalDateTime::new(dt.with_timezone(&Local)))
            .map_err(|_| serde::de::Error::invalid_value("Invalid datetime"))
    }
}

impl serde::Serialize for Comment {
    fn serialize<S>(&self, serializer: &mut S) -> Result<(), S::Error>
        where S: serde::Serializer
    {
        serializer.serialize_struct("Comment", ObjectVisitor::new(self))
    }
}

impl<'a> serde::ser::MapVisitor for ObjectVisitor<'a, Comment> {
    fn visit<S>(&mut self, serializer: &mut S) -> Result<Option<()>, S::Error>
        where S: serde::Serializer
    {
        match self.state {
            0 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("text", &self.value.text))))
            }
            1 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("pubDate", &self.value.pub_date))))
            }
            _ => { Ok(None) }
        }
    }
}

impl serde::Deserialize for Comment {
    fn deserialize<D>(deserializer: &mut D) -> Result<Comment, D::Error>
        where D: serde::Deserializer
    {
        static FIELDS: &'static [&'static str] = &["name", "uri"];
        deserializer.deserialize_struct("Comment", FIELDS, CommentVisitor)
    }
}

struct CommentVisitor;

impl serde::de::Visitor for CommentVisitor {
    type Value = Comment;

    fn visit_map<V>(&mut self, mut visitor: V) -> Result<Comment, V::Error>
        where V: serde::de::MapVisitor
    {
        let mut text = None;
        let mut pub_date = None;

        loop {
            let key = try!(visitor.visit_key::<String>());
            match key.iter().next().map(|x| x.as_ref()) {
                Some("text") => { text = try!(visitor.visit_value()); }
                Some("pubDate") => { pub_date = try!(visitor.visit_value()); }
                Some(_) => { /* ignore extra fields. */ }
                None => { break; }
            }
        }

        let text = match text {
            Some(x) => x,
            None => try!(visitor.missing_field("name"))
        };

        let pub_date = match pub_date {
            Some(x) => x,
            None => try!(visitor.missing_field("uri"))
        };

        try!(visitor.end());

        Ok(Comment::new(text, pub_date))
    }
}

impl serde::Serialize for ArticleChunk {
    fn serialize<S>(&self, serializer: &mut S) -> Result<(), S::Error>
        where S: serde::Serializer
    {
        serializer.serialize_struct("ArticleChunk", ObjectVisitor::new(self))
    }
}

impl<'a> serde::ser::MapVisitor for ObjectVisitor<'a, ArticleChunk> {
    fn visit<S>(&mut self, serializer: &mut S) -> Result<Option<()>, S::Error>
        where S: serde::Serializer
    {
        match self.state {
            0 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("type", match self.value {
                    &ArticleChunk::HtmlText(_) => { "htmltext" }
                    &ArticleChunk::TextileText(_) => { "textiletext" }
                    &ArticleChunk::PullQuote(_) => { "pullquote" }
                }))))
            }
            1 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("text", match self.value {
                    &ArticleChunk::HtmlText(ref text) => { text }
                    &ArticleChunk::TextileText(ref text) => { text }
                    &ArticleChunk::PullQuote(ref text) => { text }
                }))))
            }
            _ => { Ok(None) }
        }
    }

}

impl serde::Deserialize for ArticleChunk {
    fn deserialize<D>(deserializer: &mut D) -> Result<ArticleChunk, D::Error>
        where D: serde::Deserializer
    {
        static FIELDS: &'static [&'static str] = &["type", "text"];
        deserializer.deserialize_struct("ArticleChunk", FIELDS, ArticleChunkVisitor)
    }
}

struct ArticleChunkVisitor;

impl serde::de::Visitor for ArticleChunkVisitor {
    type Value = ArticleChunk;

    fn visit_map<V>(&mut self, mut visitor: V) -> Result<ArticleChunk, V::Error>
        where V: serde::de::MapVisitor
    {
        let mut the_type: Option<String> = None;
        let mut text: Option<String> = None;

        loop {
            let key = try!(visitor.visit_key::<String>());
            match key.iter().next().map(|x| x.as_ref()) {
                Some("type") => { the_type = try!(visitor.visit_value()); }
                Some("text") => { text = try!(visitor.visit_value()); }
                Some(_) => { /* ignore extra fields. */ }
                None => { break; }
            }
        }

        let the_type = match the_type {
            Some(x) => x,
            None => try!(visitor.missing_field("type"))
        };

        let text = match text {
            Some(x) => x,
            None => try!(visitor.missing_field("text"))
        };

        try!(visitor.end());

        match the_type.as_ref() {
            "htmltext" => { Ok(ArticleChunk::html(text)) }
            "pullquote" => { Ok(ArticleChunk::pullquote(text)) }
            "textiletext" => { Ok(ArticleChunk::textile(text)) }
            _ => { Err(serde::de::Error::invalid_value("invalid article chunk type")) }
        }
    }
}

impl serde::Serialize for Article {
    fn serialize<S>(&self, serializer: &mut S) -> Result<(), S::Error>
        where S: serde::Serializer
    {
        serializer.serialize_struct("Article", ObjectVisitor::new(self))
    }
}

impl<'a> serde::ser::MapVisitor for ObjectVisitor<'a, Article> {
    fn visit<S>(&mut self, serializer: &mut S) -> Result<Option<()>, S::Error>
        where S: serde::Serializer
    {

        match self.state {
            0 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("heading", &self.value.heading))))
            }
            1 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("content", &self.value.content))))
            }
            2 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("extract", &self.value.extract))))
            }
            3 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("pubDate", &self.value.pub_date))))
            }
            4 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("uri", &self.value.uri))))
            }
            _ => { Ok(None) }
        }
    }
}

impl serde::Deserialize for Article {
    fn deserialize<D>(deserializer: &mut D) -> Result<Article, D::Error>
        where D: serde::Deserializer
    {
        static FIELDS: &'static [&'static str] = &["heading", "content", "extract", "pubDate", "uri"];
        deserializer.deserialize_struct("Article", FIELDS, ArticleVisitor)
    }
}

struct ArticleVisitor;

impl serde::de::Visitor for ArticleVisitor {
    type Value = Article;

    fn visit_map<V>(&mut self, mut visitor: V) -> Result<Article, V::Error>
        where V: serde::de::MapVisitor
    {
        let mut heading: Option<String> = None;
        let mut content: Option<Vec<ArticleChunk>> = None;
        let mut extract: Option<String> = None;
        let mut pub_date: Option<LocalDateTime> = None;
        let mut uri: Option<String> = None;

        loop {
            let key = try!(visitor.visit_key::<String>());
            match key.iter().next().map(|x| x.as_ref()) {
                Some("heading") => { heading = try!(visitor.visit_value()); }
                Some("content") => { content = try!(visitor.visit_value()); }
                Some("extract") => { extract = try!(visitor.visit_value()); }
                Some("pubDate") => { pub_date = try!(visitor.visit_value()); }
                Some("uri") => { uri = try!(visitor.visit_value()); }
                Some(_) => { /* ignore extra fields. */ }
                None => { break; }
            }
        }

        let heading = match heading {
            Some(x) => x,
            None => try!(visitor.missing_field("heading"))
        };

        let content = match content {
            Some(x) => x,
            None => try!(visitor.missing_field("content"))
        };

        let pub_date = match pub_date {
            Some(x) => x,
            None => try!(visitor.missing_field("pub_date"))
        };

        let uri = match uri {
            Some(x) => x,
            None => try!(visitor.missing_field("uri"))
        };

        try!(visitor.end());

        Ok(Article::new(heading, content, extract, pub_date, uri))
    }
}


impl serde::Serialize for CompleteComment {
    fn serialize<S>(&self, serializer: &mut S) -> Result<(), S::Error>
        where S: serde::Serializer
    {
        serializer.serialize_struct("CompleteComment", ObjectVisitor::new(self))
    }
}

impl<'a> serde::ser::MapVisitor for ObjectVisitor<'a, CompleteComment> {
    fn visit<S>(&mut self, serializer: &mut S) -> Result<Option<()>, S::Error>
        where S: serde::Serializer
    {
        match self.state {
            0 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("comment", &self.value.comment))))
            }
            1 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("author", &self.value.author))))
            }
            _ => { Ok(None) }
        }
    }
}

impl serde::Deserialize for CompleteComment {
    fn deserialize<D>(deserializer: &mut D) -> Result<CompleteComment, D::Error>
        where D: serde::Deserializer
    {
        static FIELDS: &'static [&'static str] = &["comment", "author"];
        deserializer.deserialize_struct("CompleteComment", FIELDS, CompleteCommentVisitor)
    }
}

struct CompleteCommentVisitor;

impl serde::de::Visitor for CompleteCommentVisitor {
    type Value = CompleteComment;

    fn visit_map<V>(&mut self, mut visitor: V) -> Result<CompleteComment, V::Error>
        where V: serde::de::MapVisitor
    {
        let mut comment: Option<Comment> = None;
        let mut author: Option<Author> = None;

        loop {
            let key = try!(visitor.visit_key::<String>());
            match key.iter().next().map(|x| x.as_ref()) {
                Some("comment") => { comment = try!(visitor.visit_value()); }
                Some("author") => { author = try!(visitor.visit_value()); }
                Some(_) => { /* ignore extra fields. */ }
                None => { break; }
            }
        }

        let comment = match comment {
            Some(x) => x,
            None => try!(visitor.missing_field("comment"))
        };

        let author = match author {
            Some(x) => x,
            None => try!(visitor.missing_field("author"))
        };

        try!(visitor.end());

        Ok(CompleteComment::new(comment, author))
    }
}

impl serde::Serialize for CompleteArticle {
    fn serialize<S>(&self, serializer: &mut S) -> Result<(), S::Error>
        where S: serde::Serializer
    {
        serializer.serialize_struct("CompleteArticle", ObjectVisitor::new(self))
    }
}

impl<'a> serde::ser::MapVisitor for ObjectVisitor<'a, CompleteArticle> {
    fn visit<S>(&mut self, serializer: &mut S) -> Result<Option<()>, S::Error>
        where S: serde::Serializer
    {
        match self.state {
            0 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("id", &self.value.id))))
            }
            1 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("article", &self.value.article))))
            }
            2 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("comments", &self.value.comments))))
            }
            3 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("categories", &self.value.categories))))
            }
            4 => {
                self.state += 1;
                Ok(Some(try!(serializer.serialize_struct_elt("author", &self.value.author))))
            }
            _ => { Ok(None) }
        }
    }
}

impl serde::Deserialize for CompleteArticle {
    fn deserialize<D>(deserializer: &mut D) -> Result<CompleteArticle, D::Error>
        where D: serde::Deserializer
    {
        static FIELDS: &'static [&'static str] = &["comment", "author"];
        deserializer.deserialize_struct("CompleteArticle", FIELDS, CompleteArticleVisitor)
    }
}

struct CompleteArticleVisitor;

impl serde::de::Visitor for CompleteArticleVisitor {
    type Value = CompleteArticle;

    fn visit_map<V>(&mut self, mut visitor: V) -> Result<CompleteArticle, V::Error>
        where V: serde::de::MapVisitor
    {
        let mut id = None;
        let mut article = None;
        let mut comments = None;
        let mut categories = None;
        let mut author = None;

        loop {
            let key = try!(visitor.visit_key::<String>());
            match key.iter().next().map(|x| x.as_ref()) {
                Some("id") => { id = try!(visitor.visit_value()); }
                Some("article") => { article = try!(visitor.visit_value()); }
                Some("comments") => { comments = try!(visitor.visit_value()); }
                Some("categories") => { categories = try!(visitor.visit_value()); }
                Some("author") => { author = try!(visitor.visit_value()); }
                Some(_) => { /* ignore extra fields. */ }
                None => { break; }
            }
        }

        let id = match id {
            Some(x) => x,
            None => try!(visitor.missing_field("id"))
        };

        let article = match article {
            Some(x) => x,
            None => try!(visitor.missing_field("article"))
        };

        let comments = match comments {
            Some(x) => x,
            None => try!(visitor.missing_field("comments"))
        };

        let categories = match categories {
            Some(x) => x,
            None => try!(visitor.missing_field("categories"))
        };

        let author = match author {
            Some(x) => x,
            None => try!(visitor.missing_field("author"))
        };

        try!(visitor.end());

        Ok(CompleteArticle::new(id, article, comments, categories, author))
    }
}

#[test]
fn test_transfer_complete_article() {
    let author = Author::new("Sunny Kalsi".to_string(),
                             Some("thesunnyk@gmail.com".to_string()),
                             Some("/thesunnyk".to_string()));

    let category = Category::new("Fun and Games".to_string(), "/fun-n-games".to_string());

    let comment = Comment::new("this is a comment".to_string(),
                               LocalDateTime::new(Local::now()));

    let chunk = ArticleChunk::textile("Things are built of lice".to_string());
    
    let article = Article::new("this is a heading".to_string(), vec![chunk], None,
               LocalDateTime::new(Local::now()), "this-is-a-heading".to_string());

    let complete_comment = CompleteComment::new(comment, author.clone());

    let complete_article = CompleteArticle::new("things-are-built-of-lice".to_string(), article,
                                                vec![complete_comment], vec![category], author.clone());

    let article_new_str = ser::to_string_pretty(&complete_article).unwrap();
    let article_new: CompleteArticle = serde_json::from_str(&article_new_str).unwrap();

    assert_eq!(complete_article, article_new);
}
