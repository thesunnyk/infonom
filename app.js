var express = require('express');
var browserid = require('express-browserid');
var apFactory = require('./src/article_provider');
var dbConnFactory = require('./src/db_conn');
var opmlFactory = require('./src/opml_reader');
var fs = require('fs');
var url = require('url');

var app = express();

var dbConn = new dbConnFactory.DBConn("127.0.0.1", 5984);
var articleProvider = new apFactory.ArticleProvider(dbConn);
var opmlReader = new opmlFactory.OPMLReader(dbConn);

function articles(req, res) {
    articleProvider.byDate(0, 20, function(err, docs) {
        if (err) {
            console.log('error', err);
            res.json({ error: 'error'});
        } else {
            res.json(docs);
        }
    });
}

function upload(req, res) {
    var stream = fs.createReadStream(req.files.file.path);
    opmlReader.addOPML(stream);
    res.write("OK");
    res.end();
}

function addFeed(req, res) {
    var params = url.parse(req.url, true).query;
    if (params.feed && params.folder) {
        opmlReader.addFeed(params.feed, params.folder);
    }
    res.write("OK");
    res.end();
}

dbConn.installDb();

var intId = setInterval(opmlReader.updateAll(), 3600 * 1000);

app.use(express.bodyParser());

browserid.plugAll(app, {audience: 'http://localhost:3000'});

app.get('/node/articles', articles);

app.post('/node/upload', upload);

app.get('/node/addfeed', addFeed);

app.use("/", express.static(__dirname + "/static"));

// TODO Add 404 support.

app.listen(process.env.PORT);
console.log('Listening on port ' + process.env.PORT);
