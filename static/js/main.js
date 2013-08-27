
function ReadListViewModel() {
    this.items = ko.observable();
    this.readHeader = ko.observable();
    this.expand = function expand(item) {
        item.showItem(!item.showItem());
    }

    this.respond = function respond(item) {
        item.showRespond(!item.showRespond());
    }

    this.getInteresting = function getInteresting() {
        this.readHeader("Interesting");
        $.get("/interesting.json", function(data) {
            for (item in data) {
                data[item].showItem = ko.observable(false);
                data[item].showRespond = ko.observable(false);
            }
            this.items(data);
        }.bind(this));
    }
    this.getPopular = function getPopular() {
        this.readHeader("Popular");
        $.get("/popular.json", function(data) {
            for (item in data) {
                data[item].showItem = ko.observable(false);
                data[item].showRespond = ko.observable(false);
            }
            this.items(data);
        }.bind(this));
    }
    this.getLatest = function getLatest() {
        this.readHeader("Latest");
        $.get("/node/articles", function(data) {
            toSet = [];
            for (item in data) {
                console.log(item);
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
}

// Overall viewmodel for this screen, along with initial state
function ArticlesViewModel() {
    this.articles = ko.observable();
    this.email = ko.observable();

    this.email(currentUser);

    this.showBody = ko.observable();
    this.showBody(false);
    
    this.readList = new ReadListViewModel();
    
    this.selectInteresting = function selectInteresting(data, event) {
        this.readList.getInteresting();
    };

    this.selectPopular = function selectPopular(data, event) {
        this.readList.getPopular();
    };
    
    this.selectLatest = function selectLatest(data, event) {
        this.readList.getLatest();
    };

    this.selectSettings = function selectInteresting(data, event) {
    };
    
    this.selectInteresting();
    
    $.get("/node/articles", function(data) {
        this.articles(data);
    }.bind(this));
};

articlesModel = new ArticlesViewModel();

ko.applyBindings(articlesModel);
