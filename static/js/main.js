
var infonom_loc = "infonom.iriscouch.com";

// Overall viewmodel for this screen, along with initial state
function ArticlesViewModel() {
    var self = this;
    self.articles = ko.observable();

    $.get("/articles", function(data) {
        console.log("Got Data: " + data)
        self.articles(data);
    });
};

ko.applyBindings(new ArticlesViewModel());
