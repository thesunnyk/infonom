extern crate chrono;
extern crate serde_json;
extern crate gtk;

mod model;

mod transfer;

use std::fs::File;
use std::io::Read;
use std::io::Write;
use std::io::BufReader;
use std::io::BufWriter;
use std::cell::RefCell;
use std::rc::Rc;
use std::path::PathBuf;

use self::model::CompleteArticle;
use self::model::LocalDateTime;
use self::model::Article;
use self::model::Author;
use self::model::ArticleChunk;
use self::model::ArticleChunk::HtmlText;
use self::model::ArticleChunk::PullQuote;
use self::model::ArticleChunk::TextileText;
use gtk::Builder;
use gtk::ComboBoxText;
use gtk::Label;
use gtk::{TextView, TextBuffer};
use gtk::{ WindowExt, WidgetExt };
use gtk::HeaderBar;
use gtk::{ Button, ButtonExt };
use gtk::{ FileChooserDialog, FileChooserExt, FileChooserAction };
use gtk::DialogExt;
use gtk::ContainerExt;
use gtk::prelude::DialogExtManual;
use gtk::ListBox;
use gtk::ListBoxRow;
use gtk::{ Entry, EntryExt };
use gtk::{ Window, Inhibit };

// TODO:
// Add button should work
// Save text when switching list items

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
            chooser.get_filename().map(
                |filename| article.borrow_mut().open_file(&filename)
            );
        }
        chooser.destroy();
    });
}

fn attach_save(builder: Rc<Builder>, article: Rc<RefCell<ArticleEntry>>) {
    let save_file: Button = builder.get_object("save").unwrap();
    save_file.connect_clicked(move |_| {
        article.borrow().save();
    });
}

fn attach_new(builder: Rc<Builder>, ae: Rc<RefCell<ArticleEntry>>) {
    let new_file: Button = builder.get_object("new").unwrap();
    new_file.connect_clicked(move |_| {
        let article = CompleteArticle::empty();
        ae.borrow_mut().set_article(&article);
    });
}

fn attach_save_as(builder: Rc<Builder>, article: Rc<RefCell<ArticleEntry>>) {
    let save_as_file: Button = builder.get_object("save_as").unwrap();
    let wc: Window = builder.get_object("app").unwrap();

    save_as_file.connect_clicked(move |_| {
        let chooser = FileChooserDialog::new(Some("Save as file"), Some(&wc),
            FileChooserAction::Save);
        chooser.add_buttons(&[
            ("Save", gtk::ResponseType::Ok.into()),
            ("Cancel", gtk::ResponseType::Cancel.into()),
        ]);

        if chooser.run() == gtk::ResponseType::Ok.into() {
            chooser.get_filename().map(|fname| article.borrow_mut().save_as(&fname));
        }
        chooser.destroy();
    });
}

fn attach_list(builder: Rc<Builder>, article: Rc<RefCell<ArticleEntry>>) {
    let list: ListBox = builder.get_object("item_list").unwrap();
    list.connect_row_selected(move |_, row| {
        // TODO Fix panic on unwrap()
        article.borrow_mut().set_row(row.as_ref().unwrap().get_index());
    });
}

fn attach_add(builder: Rc<Builder>, article: Rc<RefCell<ArticleEntry>>) {
    let button: Button = builder.get_object("add_item").unwrap();
    button.connect_clicked(move |_| {
        article.borrow_mut().add_row(0);
    });
}

#[derive(Debug, Clone)]
struct ArticleEntry {
    builder: Rc<Builder>,
    fname: Option<PathBuf>,
    items: Vec<ArticleChunk>
}

impl ArticleEntry {
    fn new(builder: Rc<Builder>) -> ArticleEntry {
        ArticleEntry {
            builder: builder,
            fname: None,
            items: Vec::new()
        }
    }

    fn open_file(&mut self, filename: &PathBuf) {
        println!("Open file");
        self.fname = Some(filename.clone());
        let file = File::open(&filename).unwrap();

        let mut reader = BufReader::new(file);
        let mut contents = String::new();
        let _ = reader.read_to_string(&mut contents);

        let article_new: CompleteArticle = serde_json::from_str(&contents).unwrap();
        self.set_article(&article_new);
    }

    fn save(&self) {
        println!("Save file");
        let fname = self.fname.clone();
        fname.map(|fname| {
            let json = serde_json::to_string_pretty(&self.get_article()).unwrap();
            let file = File::create(&fname).unwrap();
            let mut writer = BufWriter::new(file);
            writer.write(json.as_bytes()).expect("Could not write to file");
        });
    }

    fn save_as(&mut self, filename: &PathBuf) {
        println!("Save as file");
        self.fname = Some(filename.clone());
        self.save();
    }

    fn set_row(&mut self, row: i32) {
        let text_view: TextView = self.builder.get_object("text_view").unwrap();

        let item = self.items.get(row as usize).unwrap();
        let text = match item {
            &TextileText(ref x) => x,
            &HtmlText(ref x) => x,
            &PullQuote(ref x) => x
        };
        let buf = TextBuffer::new(None);
        buf.set_text(text.as_ref());
        text_view.set_buffer(Some(&buf));
        text_view.show_all();
    }

    fn update_row(&mut self, row: i32, item: ArticleChunk) {
    }

    fn get_selected_item(&self) -> ArticleChunk {
        let item_type: ComboBoxText = self.builder.get_object("item_type").unwrap();
        let text_view: TextView = self.builder.get_object("text_view").unwrap();

        let content = text_view.get_buffer().and_then(|b| {
            let (start, end) = b.get_bounds();
            b.get_text(&start, &end, false)
        }).unwrap();

        match item_type.get_active_text().unwrap().as_ref() {
            "Html" => ArticleChunk::html(content),
            "Pullquote" => ArticleChunk::pullquote(content),
            _ => ArticleChunk::textile(content)
        }
    }

    fn add_row(&mut self, row: usize) {
        let item_type: ComboBoxText = self.builder.get_object("item_type").unwrap();
        let content: String = "".to_string();

        self.items.insert(row, match item_type.get_active_text().unwrap().as_ref() {
            "Html" => ArticleChunk::html(content),
            "Pullquote" => ArticleChunk::pullquote(content),
            _ => ArticleChunk::textile(content)
        });
        self.update_content();
    }

    fn set_article(&mut self, article: &CompleteArticle) {
        let id: Entry = self.builder.get_object("id").unwrap();
        let uri: Entry = self.builder.get_object("uri").unwrap();
        let heading: Entry = self.builder.get_object("heading").unwrap();
        let extract: Entry = self.builder.get_object("extract").unwrap();

        id.set_text(&article.id);
        uri.set_text(&article.id);
        heading.set_text(&article.article.heading);
        let e_str = article.article.extract.clone();
        extract.set_text(&e_str.unwrap_or("".to_string()));
        self.items = article.article.content.clone();

        self.update_content();
    }

    fn update_content(&mut self) {
        let list: ListBox = self.builder.get_object("item_list").unwrap();
        for item in list.get_children() {
            list.remove(&item);
        }
        for content in &self.items {
            let string = match content {
                &TextileText(_) => "textile",
                &HtmlText(_) => "html",
                &PullQuote(_) => "pullquote"
            };
            let list_item = ListBoxRow::new();
            list_item.add(&Label::new(Some(string)));
            list.add(&list_item);
        }
        list.show_all();
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

        let article = Article::new(heading, self.items.clone(), extract,
               LocalDateTime::empty(), uri);

        CompleteArticle::new(id, article, Vec::new(),
               Vec::new(), Author::empty())
    }

}

fn main() {
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
    attach_save_as(builder.clone(), ae.clone());
    attach_list(builder.clone(), ae.clone());
    attach_add(builder.clone(), ae.clone());

    window.show_all();
    window.connect_delete_event(|_, _| {
        gtk::main_quit();
        Inhibit(false)
    });
    gtk::main();
}
