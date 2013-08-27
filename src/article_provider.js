
exports.ArticleProvider = ArticleProvider;

function ArticleProvider(dbConn) {
    this.dbConn = dbConn;
    this.findAll = findAll;
    this.byDate = byDate;
    this.save = save;
}

function save(articles, callback) {
    if (typeof(articles.length) == "undefined") {
        articles = [articles];
    }
    
    this.dbConn.db.save(articles, function(error, result) {
        if (error) callback(error);
        else callback(null, articles);
    });
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
    this.dbConn.db.view('articles/by_date', {skip: offset, limit: count},
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
