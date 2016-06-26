extern crate chrono;
extern crate serde_json;

mod model;

mod transfer;

use std::fs::File;
use std::io::Read;

use self::model::CompleteArticle;

fn main() {
    let args = std::env::args();

    let fname = args.skip(1).next().expect("Expected a filename");

    println!("Opening {}", fname);

    let mut f: File = File::open(fname).unwrap();
    let mut s = String::new();
    f.read_to_string(&mut s).unwrap();
    
    let article_new: CompleteArticle = serde_json::from_str(&s).unwrap();

    println!("{:?}", article_new);
}
