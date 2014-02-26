var config = require('../config');
var Db = require('mongodb').Db;
var Server = require('mongodb').Server;

module.exports = new Db(config.mongoDb, new Server(config.mongoHost, config.mongoPort, {}));