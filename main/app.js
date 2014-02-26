var express = require('express');
var routes = require('./route');
var config = require('./config');
var MongoStore = require('connect-mongo')(express);
var app = express();

// config
app.configure(function(){
  app.set('views', __dirname + '/view');
  app.set('view engine', 'ejs');
  app.use(express.logger('dev'));
  app.use(express.bodyParser());
  app.use(express.methodOverride());
  app.use(express.cookieParser());
  app.use(express.session({
    secret: config.cookieSecret,
    store: new MongoStore({
      db: config.mongoDb
    })
  }));
  app.use(express.static(__dirname + '/public'));
});

// routes mapping
routes(app);

// start server
app.listen(3000);
console.log('frontend server listening on port 3000');