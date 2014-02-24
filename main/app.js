var express = require('express');
var routes = require('./route');
var app = express();

// config
app.configure(function(){
    app.set('views', __dirname + '/view');
    app.set('view engine', 'ejs');
    app.use(express.logger('dev'));
    app.use(express.bodyParser());
    app.use(express.methodOverride());
    app.use(app.router);
    app.use(express.static(__dirname + '/public'));
});

// routes mapping
app.get('/api/price', routes.price);
app.get('/api/history', routes.history);
app.get('/api/depth', routes.depth);
app.get('/api/order', routes.order);
app.get('/api/trade', routes.trade);
app.get('/api/account', routes.account);

// start server
app.listen(3000);
console.log('frontend server listening on port 3000');