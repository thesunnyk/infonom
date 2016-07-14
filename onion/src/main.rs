extern crate chrono;
extern crate serde_json;
extern crate gtk;

mod model;

mod transfer;

use std::fs::File;
use std::io::Read;

use self::model::CompleteArticle;
use gtk::{ ContainerExt, WindowExt, WidgetExt };
use gtk::HeaderBar;
use gtk::Button;
use gtk::TextView;
use gtk::Frame;
use gtk::ComboBox;
use gtk::{ Entry, Label };
use gtk::{ Box, Orientation };
use gtk::{ Window, WindowType, Inhibit };

struct HeaderBarData {
    new_file: Button,
    open_file: Button,
    save_file: Button,
    save_as_file: Button
}

impl HeaderBarData {

    fn new() -> HeaderBarData {
        let new_file = Button::new_with_label("New");
        let open_file = Button::new_with_label("Open");
        let save_file = Button::new_with_label("Save");
        let save_as_file = Button::new_with_label("Save As");
        HeaderBarData {
            new_file: new_file,
            open_file: open_file,
            save_file: save_file,
            save_as_file: save_as_file
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
}

struct ArticleEntry {
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
            uri: Entry::new(),
            heading: Entry::new(),
            date: Entry::new(),
            author: ComboBox::new(),
            categories: ComboBox::new(),
            extract: Entry::new(),
        }
    }

    fn first_line(&self) -> Box {
        let first_container = Box::new(Orientation::Horizontal, 4);
        first_container.add(&Label::new(Some("URI")));
        first_container.add(&self.uri);
        first_container.add(&Label::new(Some("Heading")));
        first_container.add(&self.heading);
        first_container.add(&Label::new(Some("Date")));
        first_container.add(&self.date);
        first_container
    }

    fn second_line(&self) -> Box {
        let second_container = Box::new(Orientation::Horizontal, 4);

        second_container.add(&Label::new(Some("Author")));
        second_container.add(&self.author);
        second_container.add(&Label::new(Some("Categories")));
        second_container.add(&self.categories);
        second_container
    }

    fn third_line(&self) -> Box {
        let third_container = Box::new(Orientation::Horizontal, 4);
        third_container.add(&Label::new(Some("Extract")));
        third_container.add(&self.extract);
        third_container
    }
}

fn main() {
    // let mut f: File = File::open(fname).unwrap();
    // let mut s = String::new();
    // f.read_to_string(&mut s).unwrap();
    
    // let article_new: CompleteArticle = serde_json::from_str(&s).unwrap();

    // println!("{:?}", article_new);

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

    let headerData = HeaderBarData::new();
    let header = headerData.header_bar();

    let article = ArticleEntry::new();

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
