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
use self::model::LocalDateTime;

use self::chrono::offset::local::Local;

use self::serde_json::ser;

fn main() {
    // let article_new: CompleteArticle = serde_json::from_str(&article_new_str).unwrap();

    // println!("{:?}", article_new);
    println!("test")
}
