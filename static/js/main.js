
var infonom_loc = "infonom.iriscouch.com";

function watch() {
    navigator.id.watch({
        loggedInUser: currentUser,
        onlogin: function(assertion) {
            $.ajax({
                type: 'POST',
                url: '/auth/login',
                data: {assertion: assertion},
                success: function(res, status, xhr) {
                    window.location.reload();
                },
                error: function(xhr, status, err) {
                    navigator.id.logout();
                    alert("login failure: " + err);
                }
            });
        },
        onlogout: function() {
            $.ajax({
                type: 'POST',
                url: '/auth/logout',
                success: function(res, status, xhr) {
                    window.location.reload();
                },
                error: function(xhr, status, err) {
                    alert("logout failure: " + err)
                }
            });
        }
    });
}

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
