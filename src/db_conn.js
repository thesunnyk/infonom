
var dbMan = require('cradle');

exports.DBConn = DBConn;

function DBConn(host, port) {
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
    
    this.installDb = installDb;
}

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
            all: {
                map: function(doc) {
                        if (doc.type === "article") {
                            emit(doc._id, doc);
                        }
                     },
            },
            by_date: {
                map: function(doc) {
                    if (doc.type === "article") {
                        emit(doc.date, doc);
                    }
                },
            }
        },
    }, printResult);
    this.db.save("_design/feeds", {
        language: "javascript",
        views: {
            all: {
                map: function(doc) {
                        if (doc.type === "feed_data") {
                            emit(doc._id, doc);
                        }
                     },
            }
        },
    }, printResult);
    
};



