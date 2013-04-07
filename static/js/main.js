
var infonom_loc = "infonom.iriscouch.com";

var currentUser = null;

// Overall viewmodel for this screen, along with initial state
function ArticlesViewModel() {
    var self = this;
    self.articles = ko.observable();
    self.email = ko.observable();

    self.email(currentUser);
    $.get("/articles", function(data) {
        self.articles(data);
    });
};

articlesModel = new ArticlesViewModel();

articlesModel.email("Not Logged In");

navigator.id.watch({
    loggedInUser: currentUser,
    onlogin: function(assertion) {
        $.ajax({
            type: 'POST',
            url: '/browserid/verify',
            data: { assertion: assertion },
            success: function(res, status, xhr) {
                currentUser = res.email;
                articlesModel.email(currentUser);
            },
            error: function(xhr, status, err) {
                navigator.id.logout();
                currentUser = null;
                articlesModel.email(currentUser);
                alert('Login Failure: ' + err);
            }
        });
    },
    onlogout: function() {
        $.ajax({
            type: 'POST',
            url: '/browserid/logout',
            success: function(res, status, xhr) { window.location.reload(); },
            error: function(xhr, status, err) {
                alert('Logout Failure: ' + err);
            }
        });
    }
});

ko.applyBindings(articlesModel);
