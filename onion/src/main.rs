extern crate uuid;
extern crate chrono;
extern crate serde_json;
extern crate gtk;

mod model;

mod transfer;

use std::thread;
use std::sync::mpsc::{ Receiver, Sender, channel };
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

use uuid::Uuid;

use gtk::Builder;
use gtk::Continue;
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

#[derive(Debug, Clone)]
enum ItemType {
    Html,
    Textile,
    Pullquote
}

#[derive(Debug, Clone)]
enum Actions {
    New,
    Open(PathBuf),
    Save,
    SaveAs(PathBuf),
    SelectRow(usize),
    AddRow(ItemType),
    DetailsUpdated {
        heading: String, extract: String
    },
    ItemUpdated(ArticleChunk)
}

// TODO Add a thing to update the list appropriately.
#[derive(Debug, Clone)]
enum Show {
    SetTextBuffer(String),
    SetArticle {
        heading: String, extract: String
    },
    UpdateList(Vec<String>)
}

struct View {
    rx: Rc<RefCell<Receiver<Show>>>,
    tx: Rc<RefCell<Sender<Actions>>>,
    builder: Rc<Builder>,
}

impl View {
    fn new(rx: Receiver<Show>, tx: Sender<Actions>, builder: Rc<Builder>) -> View {
        let view = View {
            rx: Rc::new(RefCell::new(rx)),
            tx: Rc::new(RefCell::new(tx)),
            builder: builder
        };

        view.attach_header();
        view.attach_new();
        view.attach_open();
        view.attach_save();
        view.attach_save_as();
        view.attach_list();
        view.attach_add();
        view
    }

    fn main(self) {
        gtk::timeout_add(100, move || {
            let ref me = self;
            let rx = me.rx.clone();
            while let Ok(i) = rx.borrow().try_recv() {
                println!("{:?}", i);
                match i {
                    Show::SetTextBuffer(text) => me.update_text(text),
                    Show::SetArticle { heading, extract } => me.set_article(heading, extract),
                    Show::UpdateList(items) => me.update_list(items)
                }
            }
            Continue(true)
        });

        gtk::main();
    }

    fn get_selected_item(&self) {
        let item_type: ComboBoxText = self.builder.get_object("item_type").unwrap();
        let text_view: TextView = self.builder.get_object("text_view").unwrap();

        let content = text_view.get_buffer().and_then(|b| {
            let (start, end) = b.get_bounds();
            b.get_text(&start, &end, false)
        }).unwrap();

        let chunk = match item_type.get_active_text().unwrap().as_ref() {
            "Html" => ArticleChunk::html(content),
            "Pullquote" => ArticleChunk::pullquote(content),
            _ => ArticleChunk::textile(content)
        };

        self.tx.borrow().send(Actions::ItemUpdated(chunk)).unwrap();
    }

    fn update_text(&self, text: String) {
        let buf = TextBuffer::new(None);
        buf.set_text(text.as_ref());

        let text_view: TextView = self.builder.get_object("text_view").unwrap();
        text_view.set_buffer(Some(&buf));
        text_view.show_all();
    }


    fn set_article(&self, heading: String, extract: String) {
        let heading_e: Entry = self.builder.get_object("heading").unwrap();
        let extract_e: Entry = self.builder.get_object("extract").unwrap();

        heading_e.set_text(&heading);
        extract_e.set_text(&extract);
    }

    fn get_article(&self) {
        let heading_e: Entry = self.builder.get_object("heading").unwrap();
        let extract_e: Entry = self.builder.get_object("extract").unwrap();

        let heading = heading_e.get_text().unwrap_or("".to_string());
        let extract = extract_e.get_text().unwrap_or("".to_string());

        self.tx.borrow().send(Actions::DetailsUpdated {
            heading: heading, extract: extract
        }).unwrap();
    }

    fn update_list(&self, items: Vec<String>) {
        let list: ListBox = self.builder.get_object("item_list").unwrap();
        for item in list.get_children() {
            list.remove(&item);
        }
        for content in &items {
            let list_item = ListBoxRow::new();
            list_item.add(&Label::new(Some(content)));
            list.add(&list_item);
        }
        list.show_all();
    }

    fn attach_header(&self) {
        let window: Window = self.builder.get_object("app").unwrap();
        let header: HeaderBar = self.builder.get_object("header").unwrap();

        window.set_titlebar(Some(&header));

        window.show_all();
        window.connect_delete_event(|_, _| {
            gtk::main_quit();
            Inhibit(false)
        });
    }

    fn attach_open(&self) {
        let open_file: Button = self.builder.get_object("open").unwrap();
        let wc: Window = self.builder.get_object("app").unwrap();
        let tx = self.tx.clone();
        open_file.connect_clicked(move |_| {
            let chooser = FileChooserDialog::new(Some("Open file"), Some(&wc), FileChooserAction::Open);
            chooser.add_buttons(&[
                ("Open", gtk::ResponseType::Ok.into()),
                ("Cancel", gtk::ResponseType::Cancel.into()),
            ]);

            if chooser.run() == gtk::ResponseType::Ok.into() {
                chooser.get_filename().map(
                    |filename| tx.borrow().send(Actions::Open(filename)).unwrap()
                );
            }
            chooser.destroy();
        });
    }

    fn attach_save(&self) {
        let save_file: Button = self.builder.get_object("save").unwrap();
        let tx = self.tx.clone();
        save_file.connect_clicked(move |_| {
            tx.borrow().send(Actions::Save).unwrap();
        });
    }

    fn attach_new(&self) {
        let new_file: Button = self.builder.get_object("new").unwrap();
        let tx = self.tx.clone();
        new_file.connect_clicked(move |_| {
            tx.borrow().send(Actions::New).unwrap();
        });
    }

    fn attach_save_as(&self) {
        let save_as_file: Button = self.builder.get_object("save_as").unwrap();
        let wc: Window = self.builder.get_object("app").unwrap();
        let tx = self.tx.clone();

        save_as_file.connect_clicked(move |_| {
            let chooser = FileChooserDialog::new(Some("Save as file"), Some(&wc),
                FileChooserAction::Save);
            chooser.add_buttons(&[
                ("Save", gtk::ResponseType::Ok.into()),
                ("Cancel", gtk::ResponseType::Cancel.into()),
            ]);

            if chooser.run() == gtk::ResponseType::Ok.into() {
                chooser.get_filename().map(|fname| tx.borrow().send(Actions::SaveAs(fname)).unwrap());
            }
            chooser.destroy();
        });
    }

    fn attach_list(&self) {
        let list: ListBox = self.builder.get_object("item_list").unwrap();
        let tx = self.tx.clone();
        list.connect_row_selected(move |_, row| {
            row.as_ref().map(|rr|
                tx.borrow().send(Actions::SelectRow(rr.get_index() as usize)).unwrap()
            );
        });
    }

    fn attach_add(&self) {
        let button: Button = self.builder.get_object("add_item").unwrap();
        let tx = self.tx.clone();
        button.connect_clicked(move |_| {
            tx.borrow().send(Actions::AddRow(ItemType::Textile)).unwrap();
        });
    }

}

#[derive(Debug)]
struct ArticleEntry {
    tx: Sender<Show>,
    rx: Receiver<Actions>,
    fname: Option<PathBuf>,
    selected_row: Option<usize>,
    id: String,
    heading: String,
    extract: Option<String>,
    items: Vec<ArticleChunk>
}

impl ArticleEntry {
    fn new(tx: Sender<Show>, rx: Receiver<Actions>) -> ArticleEntry {
        let uuid = Uuid::new_v4();
        ArticleEntry {
            tx: tx,
            rx: rx,
            fname: None,
            selected_row: None,
            id: uuid.to_string(),
            heading: "Empty".to_string(),
            extract: None,
            items: Vec::new()
        }
    }

    fn run(self) {
        thread::spawn(move|| {
            let mut me = self;
            while let Ok(i) = me.rx.recv() {
                println!("{:?}", i);
                match i {
                    Actions::New => {
                        let article = CompleteArticle::empty();
                        me.set_article(article)
                    },
                    Actions::Open(file) => me.open_file(&file),
                    Actions::Save => me.save(),
                    Actions::SaveAs(file) => me.save_as(&file),
                    Actions::SelectRow(row) => { me.set_row(row) },
                    Actions::AddRow(item_type) => { me.add_row(item_type) },
                    Actions::DetailsUpdated { heading, extract } => {
                        // TODO None for empty extract
                        me.set_article_details(heading, Some(extract))
                    },
                    Actions::ItemUpdated(chunk) => {
                        me.update_row(chunk)
                    }
                }
            }
        });
    }

    fn open_file(&mut self, filename: &PathBuf) {
        println!("Open file");
        self.fname = Some(filename.clone());
        let file = File::open(&filename).unwrap();

        let mut reader = BufReader::new(file);
        let mut contents = String::new();
        let _ = reader.read_to_string(&mut contents);

        let article_new: CompleteArticle = serde_json::from_str(&contents).unwrap();
        self.set_article(article_new);
    }

    fn save(&self) {
        println!("Save file");
        let fname = self.fname.clone();
        fname.map(|fname| {
            let json = serde_json::to_string_pretty(&self.gen_article()).unwrap();
            let file = File::create(&fname).unwrap();
            let mut writer = BufWriter::new(file);
            writer.write(json.as_bytes()).expect("Could not write to file");
        });
    }

    fn gen_article(&self) -> CompleteArticle {
        // TODO basename fname .json
        let uri: String = self.fname.as_ref().unwrap().to_str().unwrap().to_string();
        let heading = self.heading.clone();
        let extract = self.extract.clone();

        let article = Article::new(heading, self.items.clone(), extract,
               LocalDateTime::empty(), uri);

        CompleteArticle::new(self.id.clone(), article, Vec::new(),
               Vec::new(), Author::empty())
    }

    fn save_as(&mut self, filename: &PathBuf) {
       println!("Save as file");
       self.fname = Some(filename.clone());
       self.save();
    }

    fn set_row(&mut self, row: usize) {
        self.selected_row = Some(row);
        let item = self.items.get(row).unwrap();
        let text = match item {
            &TextileText(ref x) => x,
            &HtmlText(ref x) => x,
            &PullQuote(ref x) => x
        };

        self.tx.send(Show::SetTextBuffer(text.clone())).unwrap();
    }

    fn update_row(&mut self, item: ArticleChunk) {
        let row = self.selected_row.unwrap();
        self.items.push(item);
        self.items.swap_remove(row);
    }

    fn add_row(&mut self, item_type: ItemType) {
        let content: String = "".to_string();

        let row = self.selected_row.unwrap_or(0);

        let item = match item_type {
            ItemType::Html => ArticleChunk::html(content),
            ItemType::Pullquote => ArticleChunk::pullquote(content),
            _ => ArticleChunk::textile(content)
        };

        if row < self.items.len() {
            self.items.insert(row + 1, item);
        } else {
            self.items.push(item);
        }
        // TODO add row.
        self.update_list();
    }

    fn update_article(&self) {
        self.tx.send(Show::SetArticle {
            heading: self.heading.clone(),
            extract: self.extract.clone().unwrap_or("".to_string())
        }).unwrap();
    }

    fn update_list(&self) {
        self.tx.send(Show::UpdateList(self.items.iter().map(|item| match item {
            &ArticleChunk::HtmlText(_) => "Html".to_string(),
            &ArticleChunk::PullQuote(_) => "Pullquote".to_string(),
            _ => "Textile".to_string()
        }).collect())).unwrap();
    }

    fn set_article_details(&mut self, heading: String, extract: Option<String>) {
        self.heading = heading;
        self.extract = extract;
        self.update_article();
    }

    fn set_article(&mut self, article: CompleteArticle) {
        self.set_article_details(article.article.heading.clone(), article.article.extract.clone());

        // Also set ID and items, because when opening a file these should be kept.
        self.id = article.id.clone();
        self.selected_row = None;
        self.items = article.article.content.clone();

        // NOTE: We clobber the URI, as this should depend on the filename.

        self.update_list();
    }

}

fn main() {
    gtk::init().unwrap();

    let glade_src = include_str!("ui.glade");

    let builder = Rc::new(Builder::new_from_string(glade_src));

    let (tx, rx) = channel();
    let (tx2, rx2) = channel();

    let ae = ArticleEntry::new(tx, rx2);

    let view = View::new(rx, tx2, builder);

    ae.run();
    view.main();

}
