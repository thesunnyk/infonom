
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
    this.items = ko.observable();
 
    this.articles = ko.observable();
    this.updateArticles = function updateArticles() {
        $.get("/node/articles", function(data) {
            this.articles(data);
        }.bind(this));
    }
    this.readHeader = ko.observable();
    this.expand = function expand(item) {
        item.showItem(!item.showItem());
    }

    this.respond = function respond(item) {
        item.showRespond(!item.showRespond());
    }

    this.getItem = function getItem(header, url) {
        this.readHeader(header);
        $.get(url, function(data) {
            toSet = [];
            for (item in data) {
                var v = data[item];
                d = {};
                d.showItem = ko.observable(false);
                d.showRespond = ko.observable(false);
                d.title = v.title;
                d.extended = v.description;
                d.responses = [];
                toSet[item] = d;
            }
            this.items(toSet);
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
            articles.updateArticles();
        });
    };
};

function UserViewModel() {
    this.email = ko.observable();

    this.email(currentUser);
};

articlesModel = new ArticlesViewModel();
articlesModel.updateArticles();

userModel = new UserViewModel();
writeModel = new WriteViewModel();
menuModel = new MenuViewModel(articlesModel);

uploadFeedModel = new UploadFeedModel(articlesModel);

ko.applyBindings(menuModel, document.getElementById("menuSection"));
ko.applyBindings(writeModel, document.getElementById("writeSection"));
ko.applyBindings(articlesModel, document.getElementById("readSection"));
ko.applyBindings(uploadFeedModel, document.getElementById("uploadSection"));
ko.applyBindings(userModel, document.getElementById("login"));
