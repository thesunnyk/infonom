var express = require('express');
var apFactory = require("./src/article_provider");
var app = express();

function home(req, res) {
    res.redirect("/static/index.html");
};

var articleProvider = new apFactory.ArticleProvider("infonom.iriscouch.com", 80);

var articles = function(req, res) {
    articleProvider.findAll(function(err, docs) {
        if (err) {
            console.log('error', err);
            res.json({ error: 'error'});
        } else {
            res.json(docs);
        }
    });
};

app.use(express.bodyParser());

require("./src/express-browserid").plugAll(app);

app.get('/', home);

app.get('/articles', articles);

app.use("/static", express.static(__dirname + "/static"));

app.listen(process.env.PORT);
console.log('Listening on port ' + process.env.PORT);
