var express = require('express');
var browserid = require('express-browserid');
var apFactory = require('./src/article_provider');
var app = express();

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

browserid.plugAll(app, {audience: 'http://localhost:3000'});

app.get('/node/articles', articles);

app.use("/", express.static(__dirname + "/static"));

app.listen(process.env.PORT);
console.log('Listening on port ' + process.env.PORT);
