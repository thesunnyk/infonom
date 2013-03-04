var express = require('express');
var app = express();

var home = function(req, res){
    res.send('Hello World');
}

app.get('/', home);

app.use("/static", express.static(__dirname + "/static"));

app.listen(process.env.PORT);
console.log('Listening on port ' + process.env.PORT);
