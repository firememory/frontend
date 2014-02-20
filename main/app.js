var express = require('express');
var app = express();

// log requests
app.use(express.logger('dev'));

app.use(express.static(__dirname + '/public'));

app.use(app.router);

app.listen(3000);
console.log('listening on port 3000');