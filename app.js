var express = require('express');
var app = express();
var dbMan = require('cradle');

var ArticleProvider = function(host, port) {
    this.connection= new (dbMan.Connection)(host, port, {
        cache: true,
        raw: false
    });
    this.db = this.connection.database('test');
    var db = this.db;
    this.db.exists(function(err, exists) {
        if (err) {
            console.log('error', err);
        } else if(exists) {
            console.log("connect successful.");
        } else {
            console.log("creating DB.");
            db.create();
            this.installDb();
        }
    });
    
};

ArticleProvider.prototype.installDb = function() {
    
};

ArticleProvider.prototype.save = function(articles, callback) {
    if (typeof(articles.length) == "undefined") {
        articles = [articles];
    }
    
    this.db.save(articles, function(error, result) {
        if (error) callback(error);
        else callback(null, articles);
    });
};

ArticleProvider.prototype.findAll = function(callback) {
    this.db.view('articles/all', function(error, result) {
        if (error) {
            callback(error);
        } else {
            var docs = [];
            result.forEach(function (row) {
                docs.push(row);
            });
            callback(null, docs);
        }
    });
};

var home = function(req, res) {
    res.send('Hello World');
};

var articleProvider = new ArticleProvider("infonom.iriscouch.com", 80);

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

app.get('/', home);

app.get('/articles', articles);

app.use("/static", express.static(__dirname + "/static"));

app.listen(process.env.PORT);
console.log('Listening on port ' + process.env.PORT);
