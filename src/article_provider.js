define(['link_parser', 'words_parser', 'underscore'],
        function(linkParserFactory, wordParserFactory, underscore) {

    /**
    * Creates an article provider.
    * @param dbConn the database connection.
    * @return an Article Provider.
    */
    function ArticleProvider(dbConn) {
        this.dbConn = dbConn;
        this.byDate = byDate;
        this.bookmarked = bookmarked;
        this.byLinkScore = byLinkScore;
        this.byWordScore = byWordScore;
        this.save = save;
        this.update = update;
        this.calculateLinkSoup = calculateLinkSoup;
        this.calculateWordSoup = calculateWordSoup;
        this.calculateScore = calculateScore;
        this.getGlobalWordSoup = getGlobalWordSoup;
        this.getGlobalLinkSoup = getGlobalLinkSoup;
        this.linkParser = new linkParserFactory.LinkParser();
        this.wordParser = new wordParserFactory.WordParser();
        this.makeArticleData = makeArticleData;
        this.makeArticle = makeArticle;
    }

    function makeArticle(item) {
        return {
            type: 'article',
            title: item.title,
            guid: item.guid,
            description: item.description,
            author: item.author,
            summary: item.summary,
            link: item.link,
            date: item.date,
            image: item.image,
        };
    }

    function makeArticleData(item) {
        return {
            type: 'article_data',
            guid: item.guid,
            bookmarked: item.bookmarked,
            starred: item.starred,
            fromfeedurl: item.fromfeedurl,
            linkSoup: item.linkSoup,
            wordSoup: item.wordSoup,
            linkScore: item.linkScore,
            wordScore: item.wordScore
        };
    }

    function extractArticle(article, articleData) {
        return {
            guid: article.guid,
            title: article.title,
            description: article.description,
            author: article.author,
            summary: article.summary,
            link: article.link,
            date: article.date,
            image: article.image,
            fromfeedurl: articleData.fromfeedurl,
            bookmarked: articleData.bookmarked,
            starred: articleData.starred,
            linkScore: articleData.linkScore,
            wordScore: articleData.wordScore
        };
    }

    function callbackWith(callback, func) {
        return function cwFunc(err, res) {
            if (err) {
                callback(err);
            } else {
                callback(null, func(res));
            }
        }
    }

    function callbackOf(callback, func) {
        return function coFunc(err, res) {
            if (err) {
                callback(err);
            } else {
                func(res);
            }
        }
    }

    /**
    * Saves the given article to the database.
    * There are a couple of article chunks that form the main data structure. The
    * first is the article itself, along with a word and link soup. This is
    * immutable data. Secondly, there is the article metadata, which includes
    * bookmarked status, starred status, word and link scores, and the time they
    * were last updated.
    * @param item the article to save.
    */
    function save(item) {
        var self = this;
        function dbCallback(wordSoup, linkSoup, res) {
            if (res.length === 0) {
                item.linkSoup = self.calculateLinkSoup(item.description),
                item.wordSoup = self.calculateWordSoup(item.description),
                item.linkScore = self.calculateScore(item.linkSoup, linkSoup);
                item.wordScore = self.calculateScore(item.wordSoup, wordSoup);
                self.dbConn.db.save([self.makeArticleData(item), self.makeArticle(item)]);
            }
        }
        function linkSoupCallback(wordSoup, linkSoup) {
            self.dbConn.db.view('article_data/by_guid', {key: item.guid}, callbackOf(console.log,
                        underscore.partial(dbCallback, wordSoup, linkSoup)));
        }
        function wordSoupCallback(wordSoup) {
            self.getGlobalLinkSoup(callbackOf(console.log, underscore.partial(linkSoupCallback, wordSoup)));
        }
        this.getGlobalWordSoup(callbackOf(console.log, wordSoupCallback));
    }

    function update(item) {
        var self = this;
        function saveArticleData(res) {
            if (res.length > 0) {
                var oldVal = res[0].value;
                var newVal = underscore.defaults(item, oldVal);
                self.dbConn.db.save(oldVal._id, oldVal._rev, self.makeArticleData(item));
            }
        }
        this.dbConn.db.view('article_data/by_guid', {key: item.guid}, callbackOf(console.log, saveArticleData));
    }

    /**
    * Gets the global word soup. This can be used to calculate points for
    * articles.
    */
    function getGlobalWordSoup(callback) {
        this.dbConn.db.view('wordsoup/all', {group: true}, callbackWith(callback, fixGlobalSoup));
    }

    /**
    * Gets the global link soup. This can be used to calculate points for links.
    */
    function getGlobalLinkSoup(callback) {
        this.dbConn.db.view('linksoup/all', {group: true}, callbackWith(callback, fixGlobalSoup));
    }

    /**
    * Calculates the word soup for the given item.
    * @param item the article to calculate word soup for.
    */
    function calculateWordSoup(item) {
        return this.wordParser.parse(item);
    }

    /**
    * Calculates the link soup for the given item.
    * @param item the article to calculate word soup for.
    */
    function calculateLinkSoup(item) {
        return this.linkParser.parse(item);
    }

    /**
    * Takes key-value pairs from a couchdb mapreduce and remaps them to a set of
    * key-value pairs as searchable by javascript.
    */
    function fixGlobalSoup(globalSoup) {
        var fixed = {};
        for (index in globalSoup) {
            var item = globalSoup[index];
            fixed[item.key] = item.value;
        }
        return fixed;
    }

    /**
    * Calculate the score for the item.
    * @param item the article to calculate soup for.
    */
    function calculateScore(soup, globalSoup) {
        var score = 0;
        for (index in soup) {
            if (globalSoup[index]) {
                score += globalSoup[index];
            }
        }
        return score;
    }

    /**
    * Gets all articles, sorted by date.
    * @param offset the offset of the articles.
    * @param count the number of articles to retrieve.
    * @param callback the callback function to return articles to.
    */
    function byDate(offset, count, callback) {
        var self = this;
        function extractArticles(articles, articleDatas) {
            var docs = [];
            articles.forEach(function (article) {
                var articleData = underscore.findWhere(articleDatas, {key: article.guid}).value;
                docs.push(extractArticle(article, articleData));
            });
            return docs;
        }
        function recvByDate(articles) {
            var guids = underscore.pluck(underscore.pluck(articles, 'value'), 'guid');
            self.dbConn.db.view('article_data/by_guid', {keys: guids}, callbackWith(callback,
                        underscore.partial(extractArticles, articles)));
        }
        this.dbConn.db.view('articles/by_date', {skip: offset, limit: count, descending: true},
                callbackOf(callback, recvByDate));
    }

    /**
    * Gets all bookmarked articles.
    * @param offset the offset of the articles.
    * @param count the number of articles to retrieve.
    * @param callback the callback function to return articles to.
    */
    function bookmarked(offset, count, callback) {
        var self = this;
        function extractArticleData(articleDatas, articles) {
            var docs = [];
            articleDatas.forEach(function (articleData) {
                var article = underscore.findWhere(articles, {key: articleData.guid}).value;
                docs.push(extractArticle(article, articleData));
            });
            return docs;
        }
        function recvBookmarked(articleDatas) {
            var guids = underscore.pluck(underscore.pluck(articleDatas, 'value'), 'guid');
            self.dbConn.db.view('articles/by_guid', {keys: guids}, callbackWith(callback,
                        underscore.partial(extractArticleData, articleDatas)));
        }
        this.dbConn.db.view('article_data/bookmarked', {skip: offset, limit: count, descending: true},
                callbackOf(callback, recvBookmarked));
    }

    /**
    * Gets all bookmarked articles.
    * @param offset the offset of the articles.
    * @param count the number of articles to retrieve.
    * @param callback the callback function to return articles to.
    */
    function byLinkScore(offset, count, callback) {
        var self = this;
        function extractArticleData(articleDatas, articles) {
            var docs = [];
            articleDatas.forEach(function (articleData) {
                var article = underscore.findWhere(articles, {key: articleData.guid}).value;
                docs.push(extractArticle(article, articleData));
            });
            return docs;
        }
        function recvBookmarked(articleDatas) {
            var guids = underscore.pluck(underscore.pluck(articleDatas, 'value'), 'guid');
            self.dbConn.db.view('articles/by_guid', {keys: guids}, callbackWith(callback,
                        underscore.partial(extractArticleData, articleDatas)));
        }
        this.dbConn.db.view('article_data/by_link_score', {skip: offset, limit: count, descending: true},
                callbackOf(callback, recvBookmarked));
    }

    /**
    * Gets all bookmarked articles.
    * @param offset the offset of the articles.
    * @param count the number of articles to retrieve.
    * @param callback the callback function to return articles to.
    */
    function byWordScore(offset, count, callback) {
        var self = this;
        function extractArticleData(articleDatas, articles) {
            var docs = [];
            articleDatas.forEach(function (articleData) {
                var article = underscore.findWhere(articles, {key: articleData.guid}).value;
                docs.push(extractArticle(article, articleData));
            });
            return docs;
        }
        function recvBookmarked(articleDatas) {
            var guids = underscore.pluck(underscore.pluck(articleDatas, 'value'), 'guid');
            self.dbConn.db.view('articles/by_guid', {keys: guids}, callbackWith(callback,
                        underscore.partial(extractArticleData, articleDatas)));
        }
        this.dbConn.db.view('article_data/by_word_score', {skip: offset, limit: count, descending: true},
                callbackOf(callback, recvBookmarked));
    }

    return { ArticleProvider: ArticleProvider };
});
