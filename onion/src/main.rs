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
use gtk::{ Entry, Label };
use gtk::{ Box, Orientation };
use gtk::{ Window, WindowType, Inhibit };

struct HeaderBarData {
    new_file: Button,
    open_file: Button,
    save_file: Button,
    save_as_file: Button,
    bar: HeaderBar
}

fn create_header() -> HeaderBarData {
    let header = HeaderBar::new();
    header.set_subtitle(Some("Onion blog viewer"));
    header.set_title(Some("New File"));
    let new_file = Button::new_with_label("New");
    let open_file = Button::new_with_label("Open");
    let save_file = Button::new_with_label("Save");
    let save_as_file = Button::new_with_label("Save As");
    header.add(&new_file);
    header.add(&open_file);
    header.pack_end(&save_file);
    header.pack_end(&save_as_file);
    header.set_show_close_button(true);
    HeaderBarData {
        new_file: new_file,
        open_file: open_file,
        save_file: save_file,
        save_as_file: save_as_file,
        bar: header
    }
}

struct EntryWithLabel {
    pub label: Label,
    pub entry: Entry,
    pub container: Box
}

fn create_entry_with_label(text: &str) -> EntryWithLabel {
    let label = Label::new(Some(text));
    let entry = Entry::new();
    let container = Box::new(Orientation::Horizontal, 4);
    container.add(&label);
    container.add(&entry);
    EntryWithLabel {
        label: label,
        entry: entry,
        container: container
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
    let id_entry = create_entry_with_label("id");
    let heading_entry = create_entry_with_label("heading");
    let abox = Box::new(Orientation::Vertical, 4);

    let header = create_header();

    abox.add(&id_entry.container);
    abox.add(&heading_entry.container);
    abox.add(&text_view);

    window.set_titlebar(Some(&header.bar));
    window.add(&abox);
    window.resize(640, 480);
    window.show_all();
    window.connect_delete_event(|_, _| {
        gtk::main_quit();
        Inhibit(false)
    });
    gtk::main();
}
