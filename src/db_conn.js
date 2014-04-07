define(['cradle', 'q', 'underscore'], function(dbMan, q, underscore) {

    /**
    * Creates a database connection. Currently assumes that there is no password
    * to get into the db.
    * @param host the host for the database connection.
    * @param port the port for the database connection.
    */
    function DBConn(host, port) {
        var self = this;
        this.connection= new (dbMan.Connection)(host, port, {
            cache: true,
            raw: false
        });
        this._db = this.connection.database('test');
        this._db.exists(function(err, exists) {
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
        this.saveId = saveId;
        this.save = save;
        this.update = update;
        this.view = view;
    }

    /**
    * Prints the result of a calculation. Convenience function
    * @param err the error for the computation.
    * @param res the result of the computation.
    */
    function printResult(err, res) {
        if (err) {
            console.log("Error: ");
            console.log(err);
        } else {
            console.log("Success!");
        }
    }

    function deferCallback(deferred) {
        return function (err, res) {
            if (err) {
                deferred.reject(new Error(err));
            } else {
                deferred.resolve(res);
            }
        }
    }

    function save(item) {
        var deferred = q.defer();
        // Save array, save IDs.
        this._db.save(item, deferCallback(deferred));
        return deferred.promise;
    }

    function saveId(id, rev, val) {
        var deferred = q.defer();
        this._db.save(id, rev, val, deferCallback(deferred));
        return deferred.promise;
    }

    function throwError(msg) {
        return function () {
            throw new Error(msg);
        }
    }

    function update(view, key, item) {
        var self = this;
        function saveData(res) {
            if (res.length > 0) {
                var oldVal = res[0].value;
                var newVal = underscore.defaults(item, oldVal);
                var x = self.saveId(oldVal._id, oldVal._rev, newVal);
                return x;
            }
            return Q.fcall(throwError("No existing row."));
        }

        return this.view(view, {key: key}).then(saveData);
    }

    function view(view, params) {
        var deferred = q.defer();
        this._db.view(view, params, deferCallback(deferred));
        return deferred.promise;
    }


    /**
    * Installs the database and all views. This is useful for upgrades and initial
    * startup with no database details.
    */
    function installDb() {
        console.log("installing database");
        this._db.save("_design/article_data", {
            language: "javascript",
            views: {
                all: {
                    map: function(doc) {
                            if (doc.type === "article_data") {
                                emit(doc._id, doc);
                            }
                        },
                },
                by_guid: {
                    map: function(doc) {
                        if (doc.type === "article_data") {
                            emit(doc.guid, doc);
                        }
                    }
                },
                bookmarked: {
                    map: function(doc) {
                        if (doc.type === "article_data"
                            && doc.bookmarked) {
                            emit(doc.date, doc);
                        }
                    }
                },
                by_link_score: {
                    map: function(doc) {
                        if (doc.type === "article_data") {
                            emit(doc.linkScore, doc);
                        }
                    }
                },
                by_word_score: {
                    map: function(doc) {
                        if (doc.type === "article_data") {
                            emit(doc.wordScore, doc);
                        }
                    }
                }
            },
        }, printResult);
        this._db.save("_design/articles", {
            language: "javascript",
            views: {
                by_guid: {
                    map: function(doc) {
                        if (doc.type === "article") {
                            emit(doc.guid, doc);
                        }
                    }
                },
                by_date: {
                    map: function(doc) {
                        if (doc.type === "article") {
                            emit(doc.date, doc);
                        }
                    },
                }
            }
        }, printResult);
        this._db.save("_design/feeds", {
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
        this._db.save("_design/linksoup", {
            language: "javascript",
            views: {
                all: {
                    map: function (doc) {
                        if (doc.type === "article_data") {
                            var linkSoup = doc.linkSoup;
                            for (item in linkSoup) {
                                var count = linkSoup[item];
                                emit(item, count);
                            }
                        }
                    },
                    reduce: function (key, values, rereduce) {
                        return sum(values);
                    }
                }
            }
        }, printResult);
        this._db.save("_design/wordsoup", {
            language: "javascript",
            views: {
                all: {
                    map: function (doc) {
                        if (doc.type === "article_data" && doc.starred) {
                            var wordSoup = doc.wordSoup;
                            for (item in wordSoup) {
                                var count = wordSoup[item];
                                emit(item, count);
                            }
                        }
                    },
                    reduce: function (key, values, rereduce) {
                        return sum(values);
                    }
                }
            }
        }, printResult);
    };

    return { DBConn: DBConn };
});


