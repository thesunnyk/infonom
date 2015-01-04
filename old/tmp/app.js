
var requirejs = require('requirejs');

requirejs.config({
    baseUrl: 'src',
    nodeRequire: require
});

requirejs(['express', 'express-browserid', 'article_provider',
        'db_conn', 'opml_reader', 'fs', 'url'],
        function(express, browserid, apFactory, dbConnFactory, opmlFactory, fs, url) {

    var app = express();

    var dbConn = new dbConnFactory.DBConn("127.0.0.1", 5984);
    var articleProvider = new apFactory.ArticleProvider(dbConn);
    var opmlReader = new opmlFactory.OPMLReader(dbConn, articleProvider);

    function returnJson(res) {
        return function jsonResult(docs) {
            res.json(docs);
        }
    }

    function returnError(res) {
        return function jsonError(err) {
            console.log('error', err);
            res.json({ error: 'error'});
        }
    }

    function articles(req, res) {
        articleProvider.byDate(0, 20).then(returnJson(res)).fail(returnError(res));
    }

    function bookmarked(req, res) {
        articleProvider.bookmarked(0, 20).then(returnJson(res)).fail(returnError(res));
    }

    function popular(req, res) {
        articleProvider.byLinkScore(0, 20).then(returnJson(res)).fail(returnError(res));
    }

    function interesting(req, res) {
        articleProvider.byWordScore(0, 20).then(returnJson(res)).fail(returnError(res));
    }

    function updateArticle(req, res) {
        var article = req.body
        articleProvider.update(article);
    }

    function feeds(req, res) {
        opmlReader.findAll().then(returnJson(res)).fail(returnError(res));
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


    var intId = setInterval(opmlReader.updateAll, 3600 * 1000);

    app.use(express.bodyParser());

    browserid.plugAll(app, {audience: 'http://localhost:3000'});

    app.get('/node/articles', articles);
    app.get('/node/bookmarked', bookmarked);
    app.get('/node/popular', popular);
    app.get('/node/interesting', interesting);
    app.get('/node/feeds', feeds);

    app.post('/node/upload', upload);
    app.put('/node/updatearticle', updateArticle);

    app.get('/node/addfeed', addFeed);

    app.use("/", express.static(__dirname + "/static"));

    // TODO Add 404 support.

    app.listen(process.env.PORT);
    console.log('Listening on port ' + process.env.PORT);
    // Update all the feeds once ready.
    opmlReader.updateAll();

});
