
exports.ArticleProvider = ArticleProvider;

/**
 * Creates an article provider.
 * @param dbConn the database connection.
 * @return an Article Provider.
 */
function ArticleProvider(dbConn) {
    this.dbConn = dbConn;
    this.findAll = findAll;
    this.byDate = byDate;
    this.bookmarked = bookmarked;
    this.save = save;
}

/**
 * Save an item to the database.
 * @param item the item, as received by the opml reader.
 */
function save(item) {
    var article = {
        type: 'article',
        title: item.title,
        description: item.description,
        author: item.author,
        summary: item.summary,
        link: item.link,
        date: item.date,
        image: item.image,
        fromfeedurl: item.fromfeedurl
    };

    this.dbConn.db.save(article);
}

/**
 * Saves the given article to the database.
 * @param item the article to save.
 */
function saveArticleData(id, item) {
    var articleData = {
        type: 'articleData',
        guid: item.guid,
        bookmarked: item.bookmarked,
        starred: item.starred,
    };

    this.dbConn.db.save(article);
}

/**
 * Gets the global word soup. This can be used to calculate points for
 * articles.
 */
function getGlobalWordSoup() {
}

/**
 * Gets the global link soup. This can be used to calculate points for links.
 */
function getGlobalLinkSoup() {
}

/**
 * Calculates the word soup for the given item.
 * @param item the article to calculate word soup for.
 */
function calculateWordSoup(item) {
}

/**
 * Calculates the link soup for the given item.
 * @param item the article to calculate word soup for.
 */
function calculateLinkSoup(item) {
}

/**
 * Calculate the link score for the item.
 * @param item the article to calculate word soup for.
 */
function calculateLinkScore(item) {
}

/**
 * Calculate the word score for the item.
 * @param item the article to calculate word soup for.
 */
function calculateWordScore(item) {
}

/**
 * Gets all the articles.
 * @param callback the callback to return all articles to.
 */
function findAll(callback) {
    this.dbConn.db.view('articles/all', function(error, result) {
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
 * Gets all articles, sorted by date.
 * @param offset the offset of the articles.
 * @param count the number of articles to retrieve.
 * @param callback the callback function to return articles to.
 */
function byDate(offset, count, callback) {
    this.dbConn.db.view('articles/by_date', {skip: offset, limit: count, descending: true},
        function(error, result) {
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
 * Gets all bookmarked articles.
 * @param offset the offset of the articles.
 * @param count the number of articles to retrieve.
 * @param callback the callback function to return articles to.
 */
function bookmarked(offset, count, callback) {
    this.dbConn.db.view('articles/bookmarked', {skip: offset, limit: count, descending: true},
        function(error, result) {
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
