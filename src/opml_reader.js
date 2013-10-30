
var OpmlParser = require('opmlparser');
var FeedParser = require('feedparser');
var request = require('request');

exports.OPMLReader = OPMLReader;

function OPMLReader(conn) {
    this.conn = conn;
    this.addOPML = addOPML;
    this.updateAll = updateAll;
    this.updateFeed = updateFeed;
    this.addFeed = addFeed;
    this.findAll = findAll;
}

function addOPML(stream) {
    stream.pipe(new OpmlParser())
        .on('feed', function feed(item) {
            var toSave = {
                type: "feed_data",
                folder: item.folder,
                htmlurl: item.htmlurl,
                xmlurl: item.xmlurl,
                title: item.title,
                text: item.text,
                lastupdate: 0
            };

            this.conn.db.save(toSave);
         }.bind(this));
}

function getArticle(feedxmlurl) {
    return function(item) {
        var article = {
            type: 'article',
            title: item.title,
            description: item.description,
            author: item.author,
            summary: item.summary,
            link: item.link,
            date: new Date(item.date),
            image: item.image,
            fromfeedurl: feedxmlurl,
        };
        this.conn.db.save(item.guid, article);
    }
}

function addFeed(xmlurl, folder) {
    console.log("adding feed: " + xmlurl);
    var feedstream = request(xmlurl);
    feedstream.pipe(new FeedParser())
        .on('meta', function saveData(item) {
            var now = new Date();
            var toSave = {
                type: "feed_data",
                folder: folder,
                htmlurl: item.link,
                xmlurl: xmlurl,
                title: item.title,
                text: item.description,
                lastupdate: now.getTime()
            }
            this.conn.db.save(toSave);
        }.bind(this))
        .on('data', getArticle(xmlurl).bind(this));
}

function updateFeed(feedxmlurl, feedstream) {
    feedstream.pipe(new FeedParser())
        .on('data', getArticle(feedxmlurl).bind(this));
}

function findAll(callback) {
    this.conn.db.view('feeds/all', function(error, result) {
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
                    this.updateFeed(row.xmlurl, stream);
                    row.lastupdate = now.getTime();
                    this.conn.db.save(row);
                }
            }.bind(this));
        }
    }.bind(this));
}
