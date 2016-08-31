extern crate chrono;
extern crate serde_json;
extern crate gtk;

mod model;

mod transfer;

use std::fs::File;
use std::io::Read;
use std::io::BufReader;

use self::model::CompleteArticle;
use self::model::LocalDateTime;
use self::model::Article;
use self::model::Author;
use gtk::{ ContainerExt, WindowExt, WidgetExt };
use gtk::HeaderBar;
use gtk::{ Button, ButtonExt };
use gtk::TextView;
use gtk::Frame;
use gtk::ComboBox;
use gtk::{ FileChooserDialog, FileChooserExt, FileChooserAction };
use gtk::DialogExt;
use gtk::prelude::DialogExtManual;
use gtk::{ Entry, EntryExt, Label };
use gtk::{ Box, Orientation };
use gtk::{ Window, WindowType, Inhibit };

struct HeaderBarData {
    new_file: Button,
    open_file: Button,
    save_file: Button,
    save_as_file: Button,
}

impl HeaderBarData {

    fn new(window: Window, article: ArticleEntry) -> HeaderBarData {
        let new_file = Button::new_with_label("New");
        let open_file = Button::new_with_label("Open");
        let save_file = Button::new_with_label("Save");
        let save_as_file = Button::new_with_label("Save As");

        HeaderBarData::attach_open(&open_file, window.clone(), article.clone());
        HeaderBarData::attach_save_as(&save_as_file, window.clone());
        HeaderBarData::attach_save(&save_file, article.clone());
        HeaderBarData::attach_new(&new_file, article.clone());

        HeaderBarData {
            new_file: new_file,
            open_file: open_file,
            save_file: save_file,
            save_as_file: save_as_file,
        }
    }

    fn header_bar(&self) -> HeaderBar {
        let header = HeaderBar::new();
        header.set_subtitle(Some("Onion blog viewer"));
        header.set_title(Some("New File"));
        header.add(&self.new_file);
        header.add(&self.open_file);
        header.pack_end(&self.save_file);
        header.pack_end(&self.save_as_file);
        header.set_show_close_button(true);
        header
    }

    fn attach_open(open_file: &Button, wc: Window, article: ArticleEntry) {
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
                article.set_article(&article_new);
            }
            chooser.destroy();
        });
    }

    fn attach_save(save_file: &Button, article: ArticleEntry) {
        save_file.connect_clicked(move |_| {
            let a = article.get_article();
            println!("article: {:?}", &a);
        });
    }

    fn attach_new(new_file: &Button, ae: ArticleEntry) {
        new_file.connect_clicked(move |_| {
            let article = CompleteArticle::empty();
            ae.set_article(&article);
        });
    }

    fn attach_save_as(save_as_file: &Button, wc: Window) {

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
}

#[derive(Debug, Clone)]
struct ArticleEntry {
    id: Entry,
    uri: Entry,
    heading: Entry,
    date: Entry,
    author: ComboBox,
    categories: ComboBox,
    extract: Entry
}

impl ArticleEntry {
    fn new() -> ArticleEntry {
        ArticleEntry {
            id: Entry::new(),
            uri: Entry::new(),
            heading: Entry::new(),
            date: Entry::new(),
            author: ComboBox::new(),
            categories: ComboBox::new(),
            extract: Entry::new(),
        }
    }

    fn set_article(&self, article: &CompleteArticle) {
        self.id.set_text(&article.id);
        self.uri.set_text(&article.id);
        self.heading.set_text(&article.article.heading);
        let extract = article.article.extract.clone();
        self.extract.set_text(&extract.unwrap_or("".to_string()));
    }

    fn get_article(&self) -> CompleteArticle {
        let id = self.id.get_text().unwrap_or("".to_string());
        let uri = self.uri.get_text().unwrap_or("".to_string());
        let heading = self.heading.get_text().unwrap_or("".to_string());
        let extract = self.extract.get_text();

        let article = Article::new(heading, Vec::new(), extract,
               LocalDateTime::empty(), uri);

        CompleteArticle::new(id, article, Vec::new(),
               Vec::new(), Author::empty())
    }

    fn first_line(&self) -> Box {
        let container = Box::new(Orientation::Horizontal, 4);
        container.add(&Label::new(Some("ID")));
        container.add(&self.id);
        container.add(&Label::new(Some("URI")));
        container.add(&self.uri);
        container.add(&Label::new(Some("Heading")));
        container.add(&self.heading);
        container
    }

    fn second_line(&self) -> Box {
        let container = Box::new(Orientation::Horizontal, 4);

        container.add(&Label::new(Some("Date")));
        container.add(&self.date);
        container.add(&Label::new(Some("Author")));
        container.add(&self.author);
        container.add(&Label::new(Some("Categories")));
        container.add(&self.categories);
        container
    }

    fn third_line(&self) -> Box {
        let container = Box::new(Orientation::Horizontal, 4);
        container.add(&Label::new(Some("Extract")));
        container.add(&self.extract);
        container
    }
}

fn main() {
    // let mut f: File = File::open(fname).unwrap();
    // let mut s = String::new();
    // f.read_to_string(&mut s).unwrap();
    
    // let article_new: CompleteArticle = serde_json::from_str(&s).unwrap();

    gtk::init().unwrap();

    let window = Window::new(WindowType::Toplevel);
    window.set_title("Onion blog viewer");

    let text_view = TextView::new();

    let abox = Box::new(Orientation::Vertical, 4);

    let add_button = Button::new_with_label("Add");
    let add_box = Box::new(Orientation::Horizontal, 4);
    add_box.add(&add_button);
    let frame = Frame::new(Some("Textile"));
    frame.add(&text_view);

    let article = ArticleEntry::new();

    let header_data = HeaderBarData::new(window.clone(), article.clone());

    let header = header_data.header_bar();

    abox.add(&article.first_line());
    abox.add(&article.second_line());
    abox.add(&article.third_line());
    abox.add(&frame);
    abox.add(&add_box);

    window.set_titlebar(Some(&header));
    window.add(&abox);
    window.resize(640, 480);
    window.show_all();
    window.connect_delete_event(|_, _| {
        gtk::main_quit();
        Inhibit(false)
    });
    gtk::main();
}
