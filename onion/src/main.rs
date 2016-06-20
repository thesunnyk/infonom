extern crate chrono;
extern crate serde_json;

mod model;

mod transfer;

use self::model::CompleteArticle;
use self::model::CompleteComment;
use self::model::Author;
use self::model::Category;
use self::model::Comment;
use self::model::ArticleChunk;
use self::model::Article;

use self::chrono::offset::local::Local;

use self::serde_json::ser;

fn main() {
    let author = Author::new("Sunny Kalsi".to_string(),
                             Some("thesunnyk@gmail.com".to_string()),
                             Some("/thesunnyk".to_string()));

    let category = Category::new("Fun and Games".to_string(), "/fun-n-games".to_string());

    let comment = Comment::new("this is a comment".to_string(),
                               Local::now());

    let chunk = ArticleChunk::textile("Things are built of lice".to_string());
    
    let article = Article::new("this is a heading".to_string(), vec![chunk], None,
               Local::now(), "this-is-a-heading".to_string());

    let complete_comment = CompleteComment::new(comment, author.clone());

    let complete_article = CompleteArticle::new("things-are-built-of-lice".to_string(), article,
                                                vec![complete_comment], vec![category], author.clone());

    println!("{}", ser::to_string_pretty(&complete_article).unwrap());

    let author_new_str = ser::to_string_pretty(&author).unwrap();
    let author_new: Author = serde_json::from_str(&author_new_str).unwrap();

    println!("{}", author_new.name);
    println!("{}", author_new.email.unwrap_or("unknown".to_string()));
    println!("{}", author_new.uri.unwrap_or("unknown".to_string()));
}
