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
use std::ops::Deref;

use self::model::CompleteArticle;
use self::model::LocalDateTime;
use self::model::Article;
use self::model::Author;
use self::model::Category;
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
use gtk::{ Button, ButtonExt, ToolButton, ToolButtonExt };
use gtk::{ FileChooserDialog, FileChooserExt, FileChooserAction };
use gtk::DialogExt;
use gtk::ContainerExt;
use gtk::prelude::DialogExtManual;
use gtk::ListBox;
use gtk::ListBoxRow;
use gtk::Orientation;
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
    DeleteRow(usize),
    HeadingUpdated(String),
    ExtractUpdated(String),
    DateUpdated(LocalDateTime),
    CategoriesUpdated(String),
    AuthorUpdated(String),
    ItemUpdated(String)
}

// TODO Add click-drag-ability
#[derive(Debug, Clone)]
enum Show {
    SetTextBuffer(String),
    SetArticle {
        heading: String, extract: String, author: String, categories: String, date: LocalDateTime
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
        view.attach_metadata();
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
                    Show::SetArticle { heading, extract, author, categories, date } =>
                        me.set_article(heading, extract, author, categories, date),
                    Show::UpdateList(items) => me.update_list(items)
                }
            }
            Continue(true)
        });

        gtk::main();
    }

    fn update_text(&self, text: String) {
        let buf = TextBuffer::new(None);
        buf.set_text(text.as_ref());

        let text_view: TextView = self.builder.get_object("text_view").unwrap();
        text_view.set_buffer(Some(&buf));
        text_view.show_all();
    }


    fn set_article(&self, heading: String, extract: String, author: String, categories: String, date: LocalDateTime) {
        let heading_e: Entry = self.builder.get_object("heading").unwrap();
        let extract_e: Entry = self.builder.get_object("extract").unwrap();
        let author_e: Entry = self.builder.get_object("author").unwrap();
        let categories_e: Entry = self.builder.get_object("categories").unwrap();
        let date_e: Entry = self.builder.get_object("date").unwrap();

        heading_e.set_text(&heading);
        extract_e.set_text(&extract);
        author_e.set_text(&author);
        categories_e.set_text(&categories);
        date_e.set_text(&date.to_string());
    }

    fn attach_metadata(&self) {
        let heading_e: Entry = self.builder.get_object("heading").unwrap();
        let tx = self.tx.clone();
        heading_e.connect_focus_out_event(move |h, _| {
            let heading = h.get_text().unwrap_or("".to_string());

            tx.borrow().send(Actions::HeadingUpdated(heading)).unwrap();
            Inhibit(false)
        });

        let extract_e: Entry = self.builder.get_object("extract").unwrap();
        let tx = self.tx.clone();
        extract_e.connect_focus_out_event(move |e, _| {
            let extract = e.get_text().unwrap_or("".to_string());

            tx.borrow().send(Actions::ExtractUpdated(extract)).unwrap();
            Inhibit(false)
        });

        let author_e: Entry = self.builder.get_object("author").unwrap();
        let tx = self.tx.clone();
        author_e.connect_focus_out_event(move |e, _| {
            let author = e.get_text().unwrap_or("".to_string());

            tx.borrow().send(Actions::AuthorUpdated(author)).unwrap();
            Inhibit(false)
        });

        let categories_e: Entry = self.builder.get_object("categories").unwrap();
        let tx = self.tx.clone();
        categories_e.connect_focus_out_event(move |e, _| {
            let categories = e.get_text().unwrap_or("".to_string());

            tx.borrow().send(Actions::CategoriesUpdated(categories)).unwrap();
            Inhibit(false)
        });

    }

    fn update_list(&self, items: Vec<String>) {
        let list: ListBox = self.builder.get_object("item_list").unwrap();
        for item in list.get_children() {
            list.remove(&item);
        }
        for content in &items {
            let list_item = Rc::new(RefCell::new(ListBoxRow::new()));
            let list_item_box = gtk::Box::new(Orientation::Horizontal, 0);
            list_item.borrow_mut().add(&list_item_box);
            let content_label = Label::new(Some(content));
            content_label.set_hexpand(true);
            list_item_box.add(&content_label);

            let delete_button = ToolButton::new_from_stock("gtk-delete");

            let tx = self.tx.clone();
            let litem = list_item.clone();
            delete_button.connect_clicked(move |_| {
                tx.borrow().send(Actions::DeleteRow(litem.borrow_mut().get_index() as usize)).unwrap();
            });
            list_item_box.add(&delete_button);
            list.add(list_item.borrow().deref());
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
        let tv: TextView = self.builder.get_object("text_view").unwrap();
        let tx = self.tx.clone();
        list.connect_row_selected(move |_, row| {
            row.as_ref().map(|rr| {
                    let content = tv.get_buffer().and_then(|b| {
                        let (start, end) = b.get_bounds();
                        b.get_text(&start, &end, false)
                    }).unwrap();

                    tx.borrow().send(Actions::ItemUpdated(content)).unwrap();
                    tx.borrow().send(Actions::SelectRow(rr.get_index() as usize)).unwrap()
                }
            );
        });
    }

    fn attach_add(&self) {
        let textile_button: Button = self.builder.get_object("add_textile_item").unwrap();
        let textile_tx = self.tx.clone();
        textile_button.connect_clicked(move |_| {
            textile_tx.borrow().send(Actions::AddRow(ItemType::Textile)).unwrap();
        });

        let html_button: Button = self.builder.get_object("add_html_item").unwrap();
        let html_tx = self.tx.clone();
        html_button.connect_clicked(move |_| {
            html_tx.borrow().send(Actions::AddRow(ItemType::Html)).unwrap();
        });

        let pullquote_button: Button = self.builder.get_object("add_pullquote_item").unwrap();
        let pullquote_tx = self.tx.clone();
        pullquote_button.connect_clicked(move |_| {
            pullquote_tx.borrow().send(Actions::AddRow(ItemType::Pullquote)).unwrap();
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
    date: LocalDateTime,
    categories: Vec<Category>,
    author: Author,
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
            categories: Vec::new(),
            author: Author::new("".to_string(), None, None),
            date: LocalDateTime::empty(),
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
                    Actions::DeleteRow(row) => { me.delete_row(row) },
                    Actions::HeadingUpdated(heading) => { me.heading = heading; },
                    Actions::AuthorUpdated(author) => { me.set_author(author); },
                    Actions::CategoriesUpdated(categories) => { me.set_categories(categories); },
                    Actions::ExtractUpdated(extract) => { me.extract = Some(extract); },
                    Actions::DateUpdated(date) => { me.date = date; },
                    Actions::ItemUpdated(content) => { me.update_row(content) }
                }
            }
        });
    }

    fn set_author(&mut self, author: String) {
        let mut split = author.split("<");
        let name = split.next().map(|x| x.trim().to_string());
        let email = split.next().and_then(|x| x.split(">").next()).map(|x| x.to_string());
        let uri = email.clone().map(|x| x.to_string()).and_then(
            |x| x.split("@").next().map(|y| y.to_string())
            );

        if name.is_some() {
            self.author = Author::new(name.unwrap(), email, uri);
            print!("Author: {:?}\n", self.author);
        }
    }

    fn set_categories(&mut self, categories: String) {
        self.categories = categories.split(",").map(
            |x| Category::new(x.trim().to_string(), x.trim().replace(" ", "_").to_lowercase())
            ).collect();
        print!("Categories: {:?}\n", self.categories);
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
        let uri: String = self.fname.as_ref().unwrap().to_str().unwrap().to_string().replace(".json$", "");
        let heading = self.heading.clone();
        let extract = self.extract.clone();

        let article = Article::new(heading, self.items.clone(), extract,
               self.date.clone(), uri);

        CompleteArticle::new(self.id.clone(), article, Vec::new(),
               self.categories.clone(), self.author.clone())
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

    fn delete_row(&mut self, row: usize) {
        self.items.remove(row);

        self.update_list();
    }

    fn get_item_type(&self) -> ItemType {
        match self.items.get(self.selected_row.unwrap()).unwrap() {
            &ArticleChunk::HtmlText(_) => ItemType::Html,
            &ArticleChunk::PullQuote(_) => ItemType::Pullquote,
            &ArticleChunk::TextileText(_) => ItemType::Textile
        }
    }

    fn update_row(&mut self, content: String) {
        if self.selected_row.is_some() {
            let item = match self.get_item_type() {
                ItemType::Html => ArticleChunk::html(content),
                ItemType::Textile => ArticleChunk::textile(content),
                ItemType::Pullquote => ArticleChunk::pullquote(content)
            };
            self.items.push(item);
            self.items.swap_remove(self.selected_row.unwrap());
        }
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
        // TODO add row instead of updating complete list.
        self.update_list();
    }

    fn update_article(&self) {
        let mut author = self.author.name.clone();
        if self.author.email.is_some() {
            author.push_str(" <");
            author.push_str(self.author.email.as_ref().unwrap());
            author.push('>');
        }
        let category_vec: Vec<String> = self.categories.iter().map(|x| x.clone().name).collect();
        let categories = category_vec.iter().fold(String::new(), |acc, ref next| {
            acc + ", " + next.as_ref()
        });
        self.tx.send(Show::SetArticle {
            heading: self.heading.clone(),
            extract: self.extract.clone().unwrap_or("".to_string()),
            categories: categories,
            author: author,
            date: self.date.clone()
        }).unwrap();
    }

    fn update_list(&self) {
        self.tx.send(Show::UpdateList(self.items.iter().map(|item| match item {
            &ArticleChunk::HtmlText(_) => "Html".to_string(),
            &ArticleChunk::PullQuote(_) => "Pullquote".to_string(),
            _ => "Textile".to_string()
        }).collect())).unwrap();
    }

    fn set_article(&mut self, article: CompleteArticle) {
        self.heading = article.article.heading.clone();
        self.extract = article.article.extract.clone();
        self.date = article.article.pub_date.clone();
        self.author = article.author.clone();
        self.categories = article.categories.clone();
        self.update_article();

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
