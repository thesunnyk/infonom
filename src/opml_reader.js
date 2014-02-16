
var OpmlParser = require('opmlparser');
var FeedParser = require('feedparser');
var ArticleProvider = require('./article_provider');
var request = require('request');

exports.OPMLReader = OPMLReader;

/**
 * Handles the reading in of OPML files and creating feeds from them, and
 * creation of OPML files from a series of feeds.
 * @param conn the connection to the database.
 * @param articleProvider the article provider to use for signalling article
 * updates
 * @returns an OPML Reader object.
 */
function OPMLReader(conn, articleProvider) {
    this.conn = conn;
    this.articleProvider = articleProvider;
    this.addOPML = addOPML;
    this.updateAll = updateAll;
    this.updateFeed = updateFeed;
    this.addFeed = addFeed;
    this.findAll = findAll;
}

/**
 * Adds an OPML stream to the database. This will extract all the feeds and
 * save them to the database
 * @param stream The OPML stream.
 */
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

/**
 * Gets a function for saving an article for the given feed url.
 * @param feedxmlurl the XML URL for the feed (as distinct from the HTML link
 * to the website).
 * @return a function which can be used to save articles.
 */
function getArticle(feedxmlurl) {
    return function(item) {
        var article = {
            guid: item.guid,
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
        this.articleProvider.save(article);
    }
}

/**
 * Adds the given feed to the database. This will also update the database with
 * the latest articles from the feed URL.
 * @param xmlurl the XML URL for the feed.
 * @param folder the folder to categorise the feed in.
 */
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

/**
 * Updates a given feed, adding all new articles to the database.
 * @param feedxmlurl the XML URL for the feed.
 * @param feedstream The stream of the feed to update.
 */
function updateFeed(feedxmlurl, feedstream) {
    feedstream.pipe(new FeedParser())
        .on('data', getArticle(feedxmlurl).bind(this));
}

/**
 * Finds all feeds.
 * @param callback the callback to call with (error, docs) with a list of
 * documents containing all the feeds.
 */
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

/**
 * Updates all the feeds that haven't been updated recently.
 */
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
