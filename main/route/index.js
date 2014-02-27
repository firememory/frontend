module.exports = function (app) {
  var https = require('https');

  var redirect = function (req, res, url) {
    // fetch data from url
    https.get(url,function (response) {
      console.log('request from ' + url, 'statusCode: ' + response.statusCode);
      var data = "";
      response.on('data',function (chunk) {
        data += chunk;
      }).on('end', function () {
          res.send(200, data);
        });

    }).on('error', function (e) {
        console.error('Error:', e);
      });
  };

  var price = function (req, res) {
    // fetch price data by OKCoin API
    redirect(req, res, 'https://www.okcoin.com/api/ticker.do');
  };

  var history = function (req, res) {
    redirect(req, res, 'https://www.okcoin.com/kline/period.do?step=3600');
  };

  var depth = function (req, res) {
    // fetch depth data by OKCoin API
    redirect(req, res, 'https://www.okcoin.com/api/depth.do');
  };

  var trade = function (req, res) {
    // fetch trade data by OKCoin API
    redirect(req, res, 'https://www.okcoin.com/api/trades.do');
  };

  var order = function (req, res) {
    // TODO (chunming): query orders from DB
    res.send({"orders": [
      {"amount": "11.15", "date": 1391279930, "price": "4938.68", "tid": 4538119, "type": "sell", "status": 10},
      {"amount": "11.0", "date": 1391279930, "price": "4938.68", "tid": 4538118, "type": "sell", "status": 12},
      {"amount": "12", "date": 1391279930, "price": "4938.68", "tid": 4538117, "type": "buy", "status": 11},
      {"amount": "13.1", "date": 1391279930, "price": "4938.68", "tid": 4538116, "type": "sell", "status": 3},
      {"amount": "11.75", "date": 1391279930, "price": "4938.68", "tid": 4538115, "type": "buy", "status": 0}
    ]});
  };

  var account = function (req, res) {
    // TODO (chunming): query account data from DB
    res.send({
      "balance": 22300,
      "coins": [
        {
          "type": "btc",
          "name": "比特币",
          "balance": 12
        },
        {
          "type": "ltc",
          "name": "莱特币",
          "balance": 55.8
        },
        {
          "type": "pts",
          "name": "原型股",
          "balance": 130.77
        }
      ]
    });
  };

  var User = require('../model/user.js');
  var crypto = require('crypto');

  var login = function (req, res) {
    var md5 = crypto.createHash('md5');
    var password = md5.update(req.body.password).digest('base64');

    User.get(req.body.username, function (err, user) {
      if (!user) {
        var result = {success: false, message: '用户不存在'};
        return res.send(result);
      }
      if (user.password != password) {
        var result = {success: false, message: '密码错误'};
        return res.send(result);
      }
      req.session.user = user;
      var result = {success: true, message: '登录成功'};
      return res.send(result);
    });
  };

  var register = function (req, res) {
    // 检验用户两次输入的口令是否一致
    if (req.body['confirmPassword'] != req.body['password']) {
      var result = {success: false, message: '两次输入的口令不一致'};
      return res.send(result);
    }

    // 生成口令的散列值
    var md5 = crypto.createHash('md5');
    var password = md5.update(req.body.password).digest('base64');
    var newUser = new User({
      name: req.body.username,
      password: password
    });

    // 检查用户名是否已经存在
    User.get(newUser.name, function (err, user) {
      if (user)
        err = '用户已存在';
      if (err) {
        var result = {success: false, message: err};
        return res.send(result);
      }
      // 如果不存在则新增用户
      newUser.save(function (err) {
        if (err) {
          var result = {success: false, message: err};
          return res.send(result);
        }
        req.session.user = newUser;
        var result = {success: true, message: '注册成功'};
        return res.send(result);
      });
    });
  };

  var logout = function (req, res) {
    req.session.user = null;
    res.redirect('/');
  };

  // routes mapping
  app.get('/', function(req, res) { res.render('index', {'pageIndex': 0, 'user': req.session.user})});
  app.get('/trade', function(req, res) { res.render('trade', {'pageIndex': 1, 'user': req.session.user})});
  app.get('/market', function(req, res) { res.render('market', {'pageIndex': 2, 'user': req.session.user})});
  app.get('/user', function(req, res) { res.render('user', {'pageIndex': 3, 'user': req.session.user})});
  app.get('/api/price', price);
  app.get('/api/history', history);
  app.get('/api/depth', depth);
  app.get('/api/order', order);
  app.get('/api/trade', trade);
  app.get('/api/account', account);
  app.post('/login', login);
  app.get('/logout', logout);
  app.post('/register', register);
}