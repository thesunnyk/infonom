extern crate chrono;
extern crate serde_json;
extern crate gtk;

mod model;

mod transfer;

use std::fs::File;
use std::io::Read;
use std::io::BufReader;
use std::cell::RefCell;
use std::rc::Rc;

use self::model::CompleteArticle;
use self::model::LocalDateTime;
use self::model::Article;
use self::model::Author;
use gtk::Builder;
use gtk::{ WindowExt, WidgetExt };
use gtk::HeaderBar;
use gtk::{ Button, ButtonExt };
use gtk::{ FileChooserDialog, FileChooserExt, FileChooserAction };
use gtk::DialogExt;
use gtk::prelude::DialogExtManual;
use gtk::{ Entry, EntryExt };
use gtk::{ Window, Inhibit };


fn attach_open(builder: Rc<Builder>, article: Rc<RefCell<ArticleEntry>>) {
    let open_file: Button = builder.get_object("open").unwrap();
    let wc: Window = builder.get_object("app").unwrap();
    open_file.connect_clicked(move |_| {
        let chooser = FileChooserDialog::new(Some("Open file"), Some(&wc), FileChooserAction::Open);
        chooser.add_buttons(&[
            ("Open", gtk::ResponseType::Ok.into()),
            ("Cancel", gtk::ResponseType::Cancel.into()),
        ]);

        if chooser.run() == gtk::ResponseType::Ok.into() {
            let filename = chooser.get_filename().unwrap();
            let file = File::open(&filename).unwrap();

            let mut reader = BufReader::new(file);
            let mut contents = String::new();
            let _ = reader.read_to_string(&mut contents);

            let article_new: CompleteArticle = serde_json::from_str(&contents).unwrap();
            article.borrow().set_article(&article_new);
        }
        chooser.destroy();
    });
}

fn attach_save(builder: Rc<Builder>, article: Rc<RefCell<ArticleEntry>>) {
    let save_file: Button = builder.get_object("save").unwrap();
    save_file.connect_clicked(move |_| {
        let a = article.borrow().get_article();
        println!("article: {:?}", &a);
    });
}

fn attach_new(builder: Rc<Builder>, ae: Rc<RefCell<ArticleEntry>>) {
    let new_file: Button = builder.get_object("new").unwrap();
    new_file.connect_clicked(move |_| {
        let article = CompleteArticle::empty();
        ae.borrow().set_article(&article);
    });
}

fn attach_save_as(builder: Rc<Builder>) {
    let save_as_file: Button = builder.get_object("save_as").unwrap();
    let wc: Window = builder.get_object("app").unwrap();

    save_as_file.connect_clicked(move |_| {
        let chooser = FileChooserDialog::new(Some("Save as file"), Some(&wc),
            FileChooserAction::Save);
        chooser.add_buttons(&[
            ("Save", gtk::ResponseType::Ok.into()),
            ("Cancel", gtk::ResponseType::Cancel.into()),
        ]);

        chooser.connect_response(move |x, r| {
            println!("Responded with {:?}, {:?}", x.get_filename(), r);
        });
        chooser.run();
        chooser.destroy();
    });
}

#[derive(Debug, Clone)]
struct ArticleEntry {
    builder: Rc<Builder>
}

impl ArticleEntry {
    fn new(builder: Rc<Builder>) -> ArticleEntry {
        ArticleEntry {
            builder: builder
        }
    }

    fn set_article(&self, article: &CompleteArticle) {
        let id: Entry = self.builder.get_object("id").unwrap();
        let uri: Entry = self.builder.get_object("uri").unwrap();
        let heading: Entry = self.builder.get_object("heading").unwrap();
        let extract: Entry = self.builder.get_object("extract").unwrap();

        id.set_text(&article.id);
        uri.set_text(&article.id);
        heading.set_text(&article.article.heading);
        let e_str = article.article.extract.clone();
        extract.set_text(&e_str.unwrap_or("".to_string()));
    }

    fn get_article(&self) -> CompleteArticle {
        let id_e: Entry = self.builder.get_object("id").unwrap();
        let uri_e: Entry = self.builder.get_object("uri").unwrap();
        let heading_e: Entry = self.builder.get_object("heading").unwrap();
        let extract_e: Entry = self.builder.get_object("extract").unwrap();

        let id = id_e.get_text().unwrap_or("".to_string());
        let uri = uri_e.get_text().unwrap_or("".to_string());
        let heading = heading_e.get_text().unwrap_or("".to_string());
        let extract = extract_e.get_text();

        let article = Article::new(heading, Vec::new(), extract,
               LocalDateTime::empty(), uri);

        CompleteArticle::new(id, article, Vec::new(),
               Vec::new(), Author::empty())
    }

}

fn main() {
    // let mut f: File = File::open(fname).unwrap();
    // let mut s = String::new();
    // f.read_to_string(&mut s).unwrap();
    
    // let article_new: CompleteArticle = serde_json::from_str(&s).unwrap();

    gtk::init().unwrap();

    let glade_src = include_str!("ui.glade");

    let builder = Rc::new(Builder::new_from_string(glade_src));

    let window: Window = builder.get_object("app").unwrap();
    let header: HeaderBar = builder.get_object("header").unwrap();

    window.set_titlebar(Some(&header));

    let ae = Rc::new(RefCell::new(ArticleEntry::new(builder.clone())));

    attach_new(builder.clone(), ae.clone());
    attach_open(builder.clone(), ae.clone());
    attach_save(builder.clone(), ae.clone());
    attach_save_as(builder.clone());

    window.show_all();
    window.connect_delete_event(|_, _| {
        gtk::main_quit();
        Inhibit(false)
    });
    gtk::main();
}
