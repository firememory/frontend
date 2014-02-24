var https = require('https');

var redirect = function(req, res, url) {
    // fetch data from url
    https.get(url, function(response) {
        console.log('request from ' + url, 'statusCode: ' + response.statusCode);
        var data = "";
        response.on('data', function (chunk) {
            data += chunk;
        }).on('end', function () {
            res.send(200, data);
        });

    }).on('error', function(e) {
            console.error('Error:', e);
    });
};

exports.api = function(req, res) {
    res.send({ key: 'value' });
};

exports.price = function(req, res) {
    // fetch price data by OKCoin API
    redirect(req, res, 'https://www.okcoin.com/api/ticker.do');
};

exports.history = function(req, res) {
  redirect(req, res, 'https://www.okcoin.com/kline/period.do?step=900&mode=simple');
};

exports.depth = function(req, res) {
    // fetch depth data by OKCoin API
    redirect(req, res, 'https://www.okcoin.com/api/depth.do');
};

exports.trade = function(req, res) {
    // fetch trade data by OKCoin API
    redirect(req, res, 'https://www.okcoin.com/api/trades.do');
};

exports.order = function(req, res) {
    // TODO (chunming): query orders from DB
    res.send({"orders": [
        {"amount":"11.15","date":1391279930,"price":"4938.68","tid":4538119,"type":"sell","status":10},
        {"amount":"11.0","date":1391279930,"price":"4938.68","tid":4538118,"type":"sell","status":12},
        {"amount":"12","date":1391279930,"price":"4938.68","tid":4538117,"type":"buy","status":11},
        {"amount":"13.1","date":1391279930,"price":"4938.68","tid":4538116,"type":"sell","status":3},
        {"amount":"11.75","date":1391279930,"price":"4938.68","tid":4538115,"type":"buy","status":0}
    ]});
};

exports.account = function(req, res) {
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