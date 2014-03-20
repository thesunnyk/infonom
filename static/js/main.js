
function MenuViewModel(articles, settings, write) {
    this.selected = ko.observable();
    this.selectInteresting = function selectInteresting(data, event) {
        articles.getInteresting();
        this.selected("interesting");
        settings.hide();
        write.hide();
    };
    this.selectBookmarks = function selectBookmarks(data, event) {
        articles.getBookmarks();
        this.selected("bookmarks");
        settings.hide();
        write.hide();
    };
    this.selectPopular = function selectPopular(data, event) {
        articles.getPopular();
        this.selected("popular");
        settings.hide();
        write.hide();
    };
    this.selectLatest = function selectLatest(data, event) {
        articles.getLatest();
        this.selected("latest");
        settings.hide();
        write.hide();
    };
    this.selectSettings = function selectSettings() {
        this.selected("settings");
        articles.hide();
        write.hide();
        settings.show();
    };
    this.selectWrite = function selectWrite() {
        this.selected("write");
        articles.hide();
        settings.hide();
        write.show();
    };
}

function ArticlesViewModel() {
    this.feeds = ko.observable();
    this.items = ko.observableArray();
    this.visible = ko.observable();
 
    this.readHeader = ko.observable();
    this.expand = function expand(item) {
        item.showItem(!item.showItem());
    }

    this.respond = function respond(item) {
        item.showRespond(!item.showRespond());
    }

    this.getFeeds = function getFeeds() {
        $.get("/node/feeds", function(data) {
            var items = [];
            for (index in data) {
                var item = data[index];
                var d = {
                    title: item.title,
                    link: item.htmlurl
                };
                items[item.xmlurl] = d;
            }
            this.feeds(items);
        }.bind(this));
    }

    this.getItem = function getItem(header, url) {
        this.visible(true);
        this.readHeader(header);
        $.get(url, function(data) {
            this.items.removeAll();
            var articles = this;
            for (item in data) {
                var v = data[item];
                d = {
                    showItem: ko.observable(false),
                    showRespond: ko.observable(false),
                    starred: ko.observable(v.starred),
                    bookmarked: ko.observable(v.bookmarked),
                    guid: v.guid,
                    title: v.title,
                    description: v.description,
                    link: v.link,
                    date: v.date,
                    author: v.author,
                    fromfeedurl: v.fromfeedurl,
                    responses: []
                };
                d.publication = ko.computed(function() {
                    var feeds = articles.feeds();
                    if (feeds !== undefined && feeds[this.fromfeedurl] !== undefined) {
                        return feeds[this.fromfeedurl].title;
                    } else {
                        return "Unknown";
                    }
                }, d);
                d.publink = ko.computed(function() {
                    var feeds = articles.feeds();
                    if (feeds !== undefined && feeds[this.fromfeedurl] !== undefined) {
                        return feeds[this.fromfeedurl].link;
                    } else {
                        return "#";
                    }
                }, d);
                d.star = function star() {
                    this.starred(!this.starred());
                    var toUpload = ko.toJSON(this);
                    $.ajax('/node/updatearticle', {
                        type: 'PUT',
                        contentType: 'application/json',
                        data: toUpload
                    });
                }.bind(d);
                d.bookmark = function bookmark() {
                    this.bookmarked(!this.bookmarked());
                    var toUpload = ko.toJSON(this);
                    $.ajax('/node/updatearticle', {
                        type: 'PUT',
                        contentType: 'application/json',
                        data: toUpload
                    });
                }.bind(d);
                this.items.push(d);
            }
        }.bind(this));
    }
 
    this.getInteresting = function getInteresting() {
        this.getItem("Interesting", "/node/interesting");
    }
    this.getPopular = function getPopular() {
        this.getItem("Popular", "/node/popular");
    }
    this.getLatest = function getLatest() {
        this.getItem("Latest", "/node/articles");
    }
    this.getBookmarks = function getBookmarks() {
        this.getItem("Bookmarks", "/node/bookmarked");
    }

    this.hide = function hide() {
        this.visible(false);
    }

}

function SettingsModel(articles) {
    this.visible = ko.observable();
    this.xmlurl = ko.observable();
    this.upload = function upload() {
        $.get("/node/addfeed", {
            folder: "uncategorised",
            feed: this.xmlurl()
        }, function updateArticles() {
            articles.getLatest();
        });
    };

    this.hide = function hide() {
        this.visible(false);
    }
    this.show = function show() {
        this.visible(true);
    }
};

function WriteViewModel() {
    this.visible = ko.observable();
    this.showBody = ko.observable();
    this.hide = function hide() {
        this.visible(false);
    }
    this.show = function show() {
        this.visible(true);
    }
    this.showBody(false);
};

function UserViewModel() {
    this.email = ko.observable();

    this.email(currentUser);
};

articlesModel = new ArticlesViewModel();

articlesModel.getFeeds();

userModel = new UserViewModel();
writeModel = new WriteViewModel();
settingsModel = new SettingsModel(articlesModel);
menuModel = new MenuViewModel(articlesModel, settingsModel, writeModel);


ko.bindingHandlers.hyphenate = {
    init: function(element, valueAccessor, allBindings, viewModel, bindingContext) {},
    update: function(element, valueAccessor, allBindings, viewModel, bindingContext) {
        Hyphenator.hyphenate(element, 'en-gb');
    }
}

ko.applyBindings(menuModel, document.getElementById("menuSection"));
ko.applyBindings(writeModel, document.getElementById("writeSection"));
ko.applyBindings(articlesModel, document.getElementById("readSection"));
ko.applyBindings(settingsModel, document.getElementById("settingsSection"));
ko.applyBindings(userModel, document.getElementById("login"));

menuModel.selectLatest();
