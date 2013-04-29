
var infonom_loc = "infonom.iriscouch.com";

// Overall viewmodel for this screen, along with initial state
function ArticlesViewModel() {
    var self = this;
    self.articles = ko.observable();
    self.email = ko.observable();

    self.email(currentUser);
    $.get("/node/articles", function(data) {
        self.articles(data);
    });
};

articlesModel = new ArticlesViewModel();

articlesModel.email();

ko.applyBindings(articlesModel);
