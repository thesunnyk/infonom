
exports.ArticleProvider = ArticleProvider;

function ArticleProvider(dbConn) {
    this.dbConn = dbConn;
    this.findAll = findAll;
    this.byDate = byDate;
    this.save = save;
}

function save(id, item) {
    var article = {
        type: 'article',
        title: item.title,
        description: item.description,
        bookmarked: item.bookmarked,
        starred: item.starred,
        author: item.author,
        summary: item.summary,
        link: item.link,
        date: item.date,
        image: item.image,
        fromfeedurl: item.fromfeedurl
    };

    this.dbConn.db.save(id, article);
}

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
