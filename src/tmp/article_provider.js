define(['link_parser', 'words_parser', 'underscore', 'q'],
        function(linkParserFactory, wordParserFactory, underscore, q) {

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
        function dbCallback(data) {
            var wordSoup = data[0];
            var linkSoup = data[1];
            var res = data[2];
            if (res.length === 0) {
                item.linkSoup = self.calculateLinkSoup(item.description),
                item.wordSoup = self.calculateWordSoup(item.description),
                item.linkScore = self.calculateScore(item.linkSoup, linkSoup);
                item.wordScore = self.calculateScore(item.wordSoup, wordSoup);
                return self.dbConn.save([self.makeArticleData(item), self.makeArticle(item)]);
            }
        }
        function linkSoupCallback() {
            return self.dbConn.view('article_data/by_guid', {key: item.guid});
        }
        return q.all([this.getGlobalWordSoup(), this.getGlobalLinkSoup(), linkSoupCallback()]).then(dbCallback);
    }

    function update(item) {
        return this.dbConn.update('article_data/by_guid', item.guid, this.makeArticleData(item));
    }

    /**
    * Gets the global word soup. This can be used to calculate points for
    * articles.
    */
    function getGlobalWordSoup() {
        return this.dbConn.view('wordsoup/all', {group: true}).then(fixGlobalSoup);
    }

    /**
    * Gets the global link soup. This can be used to calculate points for links.
    */
    function getGlobalLinkSoup() {
        return this.dbConn.view('linksoup/all', {group: true}).then(fixGlobalSoup);
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
    */
    function byDate(offset, count) {
        var self = this;
        function recvByDate(articles) {
            function extractArticles(articleDatas) {
                var docs = [];
                articles.forEach(function (article) {
                    var articleData = underscore.findWhere(articleDatas, {key: article.guid}).value;
                    docs.push(extractArticle(article, articleData));
                });
                return docs;
            }
            var guids = underscore.pluck(underscore.pluck(articles, 'value'), 'guid');
            return self.dbConn.view('article_data/by_guid', {keys: guids}).then(extractArticles);
        }
        return this.dbConn.view('articles/by_date', {skip: offset, limit: count, descending: true})
            .then(recvByDate);
    }

    /**
    * Gets all bookmarked articles.
    * @param offset the offset of the articles.
    * @param count the number of articles to retrieve.
    */
    function bookmarked(offset, count) {
        var self = this;
        function recvBookmarked(articleDatas) {
            function extractArticleData(articles) {
                var docs = [];
                articleDatas.forEach(function (articleData) {
                    var article = underscore.findWhere(articles, {key: articleData.guid}).value;
                    docs.push(extractArticle(article, articleData));
                });
                return docs;
            }
            var guids = underscore.pluck(underscore.pluck(articleDatas, 'value'), 'guid');
            return self.dbConn.view('articles/by_guid', {keys: guids}).then(extractArticleData);
        }
        return this.dbConn.view('article_data/bookmarked', {skip: offset, limit: count, descending: true})
            .then(recvBookmarked);
    }

    /**
    * Gets all bookmarked articles.
    * @param offset the offset of the articles.
    * @param count the number of articles to retrieve.
    */
    function byLinkScore(offset, count) {
        var self = this;
        function recvBookmarked(articleDatas) {
            function extractArticleData(articles) {
                var docs = [];
                articleDatas.forEach(function (articleData) {
                    var article = underscore.findWhere(articles, {key: articleData.guid}).value;
                    docs.push(extractArticle(article, articleData));
                });
                return docs;
            }
            var guids = underscore.pluck(underscore.pluck(articleDatas, 'value'), 'guid');
            return self.dbConn.view('articles/by_guid', {keys: guids}).then(extractArticleData);
        }
        return this.dbConn.view('article_data/by_link_score', {skip: offset, limit: count, descending: true})
            .then(recvBookmarked);
    }

    /**
    * Gets all bookmarked articles.
    * @param offset the offset of the articles.
    * @param count the number of articles to retrieve.
    */
    function byWordScore(offset, count) {
        var self = this;
        function recvBookmarked(articleDatas) {
            function extractArticleData(articles) {
                var docs = [];
                articleDatas.forEach(function (articleData) {
                    var article = underscore.findWhere(articles, {key: articleData.guid}).value;
                    docs.push(extractArticle(article, articleData));
                });
                return docs;
            }
            var guids = underscore.pluck(underscore.pluck(articleDatas, 'value'), 'guid');
            return self.dbConn.view('articles/by_guid', {keys: guids}).then(extractArticleData);
        }
        return this.dbConn.view('article_data/by_word_score', {skip: offset, limit: count, descending: true})
            .then(recvBookmarked);
    }

    return { ArticleProvider: ArticleProvider };
});
