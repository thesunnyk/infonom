define(['sax'], function(sax) {

    function WordParser() {
        this.parse = parse;
        this.clearItems = clearItems;
        this.putItem = putItem;
        this.getItems = getItems;

        function onerror(e) {
            console.log(e);
        }

        var self = this;
        function ontext(text) {
            var items = text.toLowerCase().split(" ");
            for (item in items) {
                var word = items[item];
                var nWord = word.replace(/\W*/g, "");
                if (nWord.length > 3) {
                    self.putItem(nWord, 1);
                }
            }
        }

        this.parser = sax.parser(false, {trim: true, normalize: true});
        this.parser.onerror = onerror;
        this.parser.ontext = ontext;
    }

    function parse(data) {
        this.clearItems();

        this.parser.write(data).close();

        return this.getItems();
    }

    function clearItems() {
        this.items = {};
    }

    function putItem(item, value) {
        this.items[item] = value;
    }

    function getItems() {
        return this.items;
    }

    return { WordParser: WordParser };
});
