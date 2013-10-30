
function WriteViewModel() {
    this.showBody = ko.observable();
    this.showBody(false);
}

function MenuViewModel(articles) {
    this.selected = ko.observable();
    this.selectInteresting = function selectInteresting(data, event) {
        articles.getInteresting();
        this.selected("interesting");
    };
    this.selectPopular = function selectPopular(data, event) {
        articles.getPopular();
        this.selected("popular");
    };
    this.selectLatest = function selectLatest(data, event) {
        articles.getLatest();
        this.selected("latest");
    };
    this.selectSettings = function selectSettings() {
        this.selected("settings");
    };

    this.selectInteresting();
}

function ArticlesViewModel() {
    this.feeds = ko.observable();
    this.items = ko.observableArray();
 
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
        this.readHeader(header);
        $.get(url, function(data) {
            this.items.removeAll();
            for (item in data) {
                var v = data[item];
                d = {
                    showItem: ko.observable(false),
                    showRespond: ko.observable(false),
                    title: v.title,
                    extended: v.description,
                    link: v.link,
                    date: v.date,
                    author: v.author,
                    fromfeedurl: v.fromfeedurl,
                    articles: this,
                    responses: []
                };
                d.publication = ko.computed(function() {
                    var feeds = this.articles.feeds();
                    if (feeds !== undefined && feeds[this.fromfeedurl] !== undefined) {
                        return feeds[this.fromfeedurl].title;
                    } else {
                        return "Unknown";
                    }
                }, d);
                d.publink = ko.computed(function() {
                    var feeds = this.articles.feeds();
                    if (feeds !== undefined && feeds[this.fromfeedurl] !== undefined) {
                        return feeds[this.fromfeedurl].link;
                    } else {
                        return "#";
                    }
                }, d);
                this.items.push(d);
            }
        }.bind(this));
    }
 
    this.getInteresting = function getInteresting() {
        this.getItem("Interesting", "/interesting.json");
    }
    this.getPopular = function getPopular() {
        this.getItem("Popular", "/popular.json");
    }
    this.getLatest = function getLatest() {
        this.getItem("Latest", "/node/articles");
    }

}

function UploadFeedModel(articles) {
    this.xmlurl = ko.observable();
    this.upload = function upload() {
        $.get("/node/addfeed", {
            folder: "uncategorised",
            feed: this.xmlurl()
        }, function updateArticles() {
            articles.getLatest();
        });
    };
};

function UserViewModel() {
    this.email = ko.observable();

    this.email(currentUser);
};

articlesModel = new ArticlesViewModel();

articlesModel.getFeeds();

userModel = new UserViewModel();
writeModel = new WriteViewModel();
menuModel = new MenuViewModel(articlesModel);

uploadFeedModel = new UploadFeedModel(articlesModel);

ko.applyBindings(menuModel, document.getElementById("menuSection"));
ko.applyBindings(writeModel, document.getElementById("writeSection"));
ko.applyBindings(articlesModel, document.getElementById("readSection"));
ko.applyBindings(uploadFeedModel, document.getElementById("uploadSection"));
ko.applyBindings(userModel, document.getElementById("login"));
