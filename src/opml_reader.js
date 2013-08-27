
var OpmlParser = require('opmlparser');
var FeedParser = require('feedparser');
var request = require('request');

exports.OPMLReader = OPMLReader;

function OPMLReader(conn) {
    this.conn = conn;
    this.addOPML = addOPML;
    this.updateAll = updateAll;
    this.updateFeed = updateFeed;
}

function addOPML(stream) {
    stream.pipe(new OpmlParser())
        .on('feed', function feed(item) {
            var toSave = {};
            toSave.type = "feed_data";
            toSave.folder = item.folder;
            toSave.htmlurl = item.htmlurl;
            toSave.xmlurl = item.xmlurl;
            toSave.title = item.title;
            toSave.text = item.text;
            toSave.lastupdate = 0;
            this.conn.db.save(toSave);
         }.bind(this));

}

function updateFeed(feedstream) {
    feedstream.pipe(new FeedParser())
        .on('data', function article(item) {
            var article = {};
            article.type = 'article';
            article.title = item.title;
            article.description = item.description;
            article.author = item.author;
            article.summary = item.summary;
            article.link = item.link;
            article.date = item.date;
            article.image = item.image;
            article.fromfeedurl = item.xmlurl;
            this.conn.db.save(item.guid, article);
        }.bind(this));
}

function updateAll() {
    this.conn.db.view('feeds/all', function(error, result) {
        if (error) {
        } else {
            var docs = [];
            result.forEach(function (row) {
                var now = new Date();
                if (((now.getTime() - row.lastupdate)  / 1000) > (3600 * 24)) {
                    var stream = request(row.xmlurl);
                    this.updateFeed(stream);
                    row.lastupdate = now.getTime();
                    this.conn.db.save(row);
                }
            }.bind(this));
        }
    }.bind(this));
}
