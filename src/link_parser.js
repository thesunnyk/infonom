exports.LinkParser = LinkParser;

var sax = require('sax');

function LinkParser() {
    this.parse = parse;
    this.clearItems = clearItems;
    this.putItem = putItem;
    this.getItems = getItems;

    function onerror(e) {
        console.log(e);
    }

    var self = this;
    function onopentag(node) {
        if (node.name == 'A') {
            var item = node.attributes.HREF;
            if (item.indexOf('http') == 0) {
                self.putItem(item, 1);
            }
        }
    }

    this.parser = sax.parser(false);
    this.parser.onerror = onerror;
    this.parser.onopentag = onopentag;
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
