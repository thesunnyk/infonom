
var currentUser = null;

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

