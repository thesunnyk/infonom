extern crate chrono;

use self::chrono::datetime::DateTime;
use self::chrono::offset::local::Local;

#[derive(Debug, Clone, PartialEq)]
pub struct LocalDateTime {
    pub datetime: DateTime<Local>
}

impl LocalDateTime {
    pub fn new(datetime: DateTime<Local>) -> LocalDateTime {
        LocalDateTime { datetime: datetime }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub enum ArticleChunk {
    TextileText(String),
    HtmlText(String),
    PullQuote(String)
}

impl ArticleChunk {
    pub fn textile(text: String) -> ArticleChunk {
        ArticleChunk::TextileText(text)
    }

    pub fn html(text: String) -> ArticleChunk {
        ArticleChunk::HtmlText(text)
    }

    pub fn pullquote(text: String) -> ArticleChunk {
        ArticleChunk::PullQuote(text)
    }

}

#[derive(Debug, Clone, PartialEq)]
pub struct Author {
    pub name: String,
    pub email: Option<String>,
    pub uri: Option<String>
}

impl Author {
    pub fn new(name: String, email: Option<String>, uri: Option<String>) -> Author {
        Author {
            name: name,
            email: email,
            uri: uri
        }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub struct Category {
    pub name: String,
    pub uri: String
}

impl Category {
    pub fn new(name: String, uri: String) -> Category {
        Category {
            name: name,
            uri: uri
        }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub struct Comment {
    pub text: String,
    pub pub_date: LocalDateTime
}

impl Comment {
    pub fn new(text: String, pub_date: LocalDateTime) -> Comment {
        Comment {
            text: text,
            pub_date: pub_date
        }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub struct Article {
    pub heading: String,
    pub content: Vec<ArticleChunk>,
    pub extract: Option<String>,
    pub pub_date: LocalDateTime,
    pub uri: String
}

impl Article {
    pub fn new(heading: String, content: Vec<ArticleChunk>, extract: Option<String>,
               pub_date: LocalDateTime, uri: String) -> Article {
        Article {
            heading: heading,
            content: content,
            extract: extract,
            pub_date: pub_date,
            uri: uri
        }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub struct CompleteComment {
    pub comment: Comment,
    pub author: Author
}

impl CompleteComment {
    pub fn new(comment: Comment, author: Author) -> CompleteComment {
        CompleteComment {
            comment: comment,
            author: author
        }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub struct CompleteArticle {
    pub id: String,
    pub article: Article,
    pub comments: Vec<CompleteComment>,
    pub categories: Vec<Category>,
    pub author: Author
}

impl CompleteArticle {
    pub fn new(id: String, article: Article, comments: Vec<CompleteComment>,
               categories: Vec<Category>, author: Author) -> CompleteArticle {
        CompleteArticle {
            id: id,
            article: article,
            comments: comments,
            categories: categories,
            author: author
        }
    }
}

