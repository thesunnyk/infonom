var dbMan = require('cradle');

ArticleProvider.prototype.installDb = installDb;
ArticleProvider.prototype.save = save;
ArticleProvider.prototype.findAll = findAll;

exports.ArticleProvider = ArticleProvider;

function ArticleProvider(host, port) {
    var self = this;
    this.connection= new (dbMan.Connection)(host, port, {
        cache: true,
        raw: false
    });
    this.db = this.connection.database('test');
    this.db.exists(function(err, exists) {
        if (err) {
            console.log('error', err);
        } else if(exists) {
            console.log("connect successful.");
        } else {
            console.log("creating DB.");
            self.db.create();
            self.installDb();
        }
    });
};

function printResult(err, res) {
        if (err) {
            console.log("Error: " + err);
        } else {
            console.log("Success!");
        }
}

function installDb() {
    console.log("installing database");
    this.db.save("_design/articles", {
        language: "javascript",
        views: {
            "all": {
                "map": "function(doc) { emit(doc._id, doc) }",
            }
        },
    }, printResult);
    
};

function save(articles, callback) {
    if (typeof(articles.length) == "undefined") {
        articles = [articles];
    }
    
    this.db.save(articles, function(error, result) {
        if (error) callback(error);
        else callback(null, articles);
    });
};

function findAll(callback) {
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
