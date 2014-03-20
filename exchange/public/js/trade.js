var tradeApp = angular.module('coinport.trade', ['ui.bootstrap', 'ngResource', 'ngRoute', 'navbar', 'timer']);

function routeConfig($routeProvider) {
    $routeProvider.
    when('/', {
        controller: 'BidAskCtrl',
        templateUrl: 'views/bidask.html'
    }).
    when('/deposit/rmb', {
        controller: 'DepositRmbCtrl',
        templateUrl: 'views/deposit.html'
    }).
    when('/deposit/btc', {
        controller: 'DepositBtcCtrl',
        templateUrl: 'views/deposit-btc.html'
    }).
    when('/account', {
            controller: 'UserCtrl',
            templateUrl: 'views/account.html'
    }).

    when('/test', {
            controller: 'DepositBtcCtrl',
            templateUrl: 'views/test.html'
    }).
    otherwise({
        redirectTo: '/'
    });
}

tradeApp.config(routeConfig);

function BidAskCtrl($scope, $http) {
    $scope.orders = [];
    $scope.bid = {type: 'bid', price: 4000, amount: 0, total: 0};
    $scope.ask = {type: 'ask', price: 5000, amount: 0, total: 0};
    $scope.account = {RMB: 0, BTC: 0}
    $scope.bidOptions = {limitPrice: true, limitAmount: true, limitTotal: false};
    $scope.askOptions = {limitPrice: true, limitAmount: true, limitTotal: false};
    $scope.info = {fundingLocked: 0, fundingRemaining: 0, quantityLocked: 0, quantityRemaining: 0, income: 0}

    $scope.refresh = function() {
        $http.get('api/account')
            .success(function(data, status, headers, config) {
                console.log('got', data);
                $scope.account = data;
                updateAskTotal();
                updateBidTotal();
        });

        $http.get('api/depth')
          .success(function(data, status, headers, config) {
            $scope.depth = data;
            $scope.depth.asks.reverse();
        });

        $http.get('api/order')
        .success(function(data, status, headers, config) {
            $scope.orders = data
        });
    };

    $scope.refresh();

        $http.get('api/history')
          .success(function(data, status, headers, config) {
            $scope.history = [];
            data.forEach(function(row) {
              $scope.history.push([row[0]*1000, row[3], row[5], row[6], row[4], row[7]]);
            });

            var chart = $('.candle-chart').jqCandlestick($scope.history, {
              theme: 'light',
              yAxis: [{
                height: 8
              }, {
                height: 2
              }],
              info: {
                color: '#000', // color for info
                font: null, // font
                spacing: 10, // distance between values
                position: 'left', // 'left', 'right' or 'auto'
                wrap: 'no' // 'auto', 'yes' or 'no'
              },
              cross: {
                color: 'rgba(0, 0, 0, 0.6)', // color of cursor-cross
                strokeWidth: 1.0, // width cursor-cross lines
                text: {
                  //background: '#cccccc', // background color for text
                  font: null, // font for text
                  color: '#000' // color for text
                }
              },
              xAxis: {
                dataLeftOffset: Math.max(0, $scope.history.length - 31),
                dataRightOffset: $scope.history.length - 1
              },
              series: [{
                type: 'candlestick',
                names: ['开盘','最高', '最低', '收盘'],
                upStroke: '#009dc6',
                upColor: 'rgba(0, 150, 255, 0.3)',
                downStroke: '#b9231f',
                downColor: 'rgba(255, 0, 0, 0.3)'
              }, {
                type: 'column',
                name: '成交量',
                dataOffset: 5,
                yAxis: 1,
                stroke: '#8b4787',
                color: '#8b4787'
              }]
            });
          });

    var updateBidTotal = function() {
        if(!$scope.account || $scope.account.RMB == undefined || $scope.bid.price == undefined || $scope.bid.amount == undefined)
            return;
        var total = $scope.bid.price * $scope.bid.amount;
        if(total > $scope.account.RMB) {
            total = $scope.account.RMB;
//            updateBidAmount();
        }
        $scope.bid.total = total
        $scope.info.fundingLocked = total;
        $scope.info.fundingRemaining = $scope.account.RMB - total;
        console.log('update bid total', $scope.bid.price, $scope.bid.amount, $scope.bid.total);
    };

    var updateBidAmount = function() {
        $scope.bid.amount = Math.round($scope.bid.total / $scope.bid.price * 10000)/10000;
        console.log('update bid amount', $scope.bid.total, $scope.bid.price, $scope.bid.amount);
    };

    var updateAskTotal = function() {
        console.log('update ask total', $scope.account, $scope.ask.price, $scope.ask.amount);
        if(!$scope.account || $scope.account.RMB == undefined || $scope.ask.price == undefined || $scope.ask.amount == undefined)
            return;
        var total = $scope.ask.price * $scope.ask.amount;

        $scope.info.income = total
        $scope.info.quantityLocked = $scope.ask.amount;
        $scope.info.quantityRemaining = $scope.account.BTC - $scope.info.quantityLocked;
    };

    var updateAskAmount = function() {
        console.log('update ask amount', $scope.ask.total, $scope.ask.price);
        $scope.ask.amount = $scope.ask.total / $scope.ask.price;
    };

    $scope.addBidOrder = function() {
        console.log('add bid', $scope.bid);
        if($scope.bid.amount < 0)
            return;
        $scope.account.RMB = ($scope.account.RMB - $scope.bid.total).toFixed(2);
        $scope.bid.total = Math.min($scope.bid.total, $scope.account.RMB);
        $scope.orders.push({
            "date": new Date().getTime(),
            "tid": 0,
            "price": $scope.bid.price,
            "amount": $scope.bid.amount,
            "type": "buy",
            "status": -1
        });

        $http.post('trade/bid', $scope.bid)
          .success(function(data, status, headers, config) {
            console.log('bid order sent, response:', data);
            $scope.refresh();
        });
    };

    $scope.addAskOrder = function() {
        console.log('add ask', $scope.ask);
        if($scope.ask.amount < 0)
            return;
        $scope.orders.push({
            "date": new Date().getTime(),
            "tid": 0,
            "price": $scope.ask.price,
            "amount": $scope.ask.amount,
            "type": "sell",
            "status": -1
        });

        $http.post('trade/bid', $scope.ask)
          .success(function(data, status, headers, config) {
            console.log('bid order sent, response:', data);
            $scope.refresh();
        });
    };

    $scope.clickFunding = function(amount) {
        $scope.bid.total = amount;
        $scope.bidOptions.limitTotal = true;
        $scope.info.fundingLocked = amount;
        $scope.info.fundingRemaining = $scope.account.RMB - amount;
    }

    $scope.clickQuantity = function(quantity) {
        $scope.ask.amount = quantity;
        $scope.askOptions.limitAmount = true;
    }

    $scope.cancelOrder = function(tid) {
        console.log('cancel order', tid);
        for(var i = 0; i < $scope.orders.length; i++) {
            if($scope.orders[i].tid == tid) {
                $scope.orders.splice(i, 1);
                break;
            }
        }
    };

    // polling
    $scope.$on('timer-tick', function (event, args) {
        //console.log('polling', args);
        //$scope.refresh();
    });

    $scope.$watch('bid.amount', updateBidTotal);
    $scope.$watch('bid.price', updateBidTotal);
//    $scope.$watch('bid.total', updateBidAmount);
    $scope.$watch('ask.amount', updateAskTotal);
    $scope.$watch('ask.price', updateAskTotal);
//    $scope.$watch('ask.total', updateAskAmount);
}

tradeApp.controller('DepositRmbCtrl', ['$scope', '$http', function($scope, $http) {
    $scope.refresh = function() {
        $http.get('api/account')
              .success(function(data, status, headers, config) {
                $scope.balance = data['RMB'];
              });
    }

    $scope.refresh();
    $scope.depositData = {type: 'RMB'};
    $scope.deposit = function() {
        var amount = $scope.amount;
        console.log('deposit ' + $scope.depositData.amount);
        $http.post('account/deposit', $scope.depositData)
          .success(function(data, status, headers, config) {
            console.log(data);
            $scope.refresh();
          });
    };

    // polling
    $scope.$on('timer-tick', function (event, args) {
        console.log('polling', args);
        $scope.refresh();
    });
}]);

tradeApp.controller('DepositBtcCtrl', ['$scope', '$http', function($scope, $http) {
    $scope.refresh = function() {
        $http.get('api/account')
              .success(function(data, status, headers, config) {
                $scope.balance = data['BTC'];
              });
    }

    $scope.refresh();
    $scope.depositData = {type: 'BTC'};
    $scope.deposit = function() {
        var amount = $scope.amount;
        console.log('deposit ' + $scope.depositData.amount);
        $http.post('account/deposit', $scope.depositData)
          .success(function(data, status, headers, config) {
            console.log(data);
            $scope.refresh();
          });
    };

    // polling
    $scope.$on('timer-tick', function (event, args) {
        //console.log('polling', args);
        //$scope.refresh();
    });

}]);

tradeApp.controller('UserCtrl', function ($scope, $http) {
    $http.get('api/account')
        .success(function(data, status, headers, config) {
            console.log('got', data);
            $scope.accounts = data.accounts;
        });
    // mocked data
    var data = [];

    for (var i = 1; i < 30; i++) {
        var time = new Date(2014, 0, i);
        var btc = 10000 + 2000 * Math.random();
        var ltc = 3100;
        var pts = 1000;
        var cny = 13411 + 5000 * Math.floor(i / 20);
        var price = 4000 + 200 * Math.random();

        data.push({date: time, value1: cny, value2: btc, value3: ltc, value4: pts, value5: price});
    }

    // jQuery code
    $('#user-finance-chart-history').jqChart({
        title: { text: '资产净值(CNY)' },
        dataSource: data,
        axes: [
            {
                type: 'dateTime',
                location: 'bottom',
                minimum: new Date(2014, 0, 1),
                maximum: new Date(2014, 0, 29),
                interval: 1,
                intervalType: 'days'
            },
            {
                name: 'y1',
                location: 'left'
            }
        ],
        series: [
            {
                type: 'stackedArea',
                title: '人民币(CNY)',
                xValuesField: 'date',
                yValuesField: 'value1',
                axisY: 'y1'
            },
            {
                type: 'stackedArea',
                title: '比特币(BTC)',
                xValuesField: 'date',
                yValuesField: 'value2',
                axisY: 'y1'
            },
            {
                type: 'stackedArea',
                title: '莱特币(LTC)',
                xValuesField: 'date',
                yValuesField: 'value3',
                axisY: 'y1'
            },
            {
                type: 'stackedArea',
                title: '原型股(PTS)',
                xValuesField: 'date',
                yValuesField: 'value4',
                axisY: 'y1'
            }
        ]
    });

    $('#user-finance-chart-pie').jqChart({
        title: { text: '资产构成' },
        legend: {},
        shadows: {
            enabled: true
        },
        series: [
            {
                type: 'pie',
                fillStyles: ['#418CF0', '#FCB441', '#E0400A', '#056492', '#BFBFBF', '#1A3B69', '#FFE382'],
                data: [['人民币(CNY)', 15498.21], ['比特币(BTC)', 58745.04], ['莱特币(LTC)', 3031.12],
                    ['原型股(PTS)', 6064.73]],
                labels: {
                    stringFormat: '%.1f%%',
                    valueType: 'percentage',
                    font: '15px sans-serif',
                    fillStyle: 'white'
                },
                explodedRadius: 10,
                explodedSlices: [0],
                strokeStyle : '#cccccc',
                lineWidth : 1
            }
        ]
    });
});

tradeApp.filter('orderTypeText', function() {
    var filter = function(input) {
        if(input == 'buy')
            return '买入';
        if(input == 'sell')
            return '卖出';
        return '未知';
    }
    return filter;
});

tradeApp.filter('orderStatusClass', function() {
    var filter = function(input) {
        if(input == 2)
            return 'success';
        if(input == 0)
            return 'info';
        if(input == 1)
            return 'warning';
        if(input == 3)
            return 'danger';
        return '';
    }
    return filter;
});

tradeApp.filter('orderStatusText', function() {
    var filter = function(input) {
        if(input == 2)
            return '交易成功';
        if(input == 0)
            return '挂单中';
        if(input == 1)
            return '部分成交';
        if(input == 3)
            return '已撤销';
        if(input == -1)
            return '等待处理';
        return '';
    }
    return filter;
});

tradeApp.filter('currency', function() {
    var filter = function(input) {
        return input ? input.toFixed(2) : '-';
    }
    return filter;
});

tradeApp.filter('quantity', function() {
    var filter = function(input) {
        return input ? input.toFixed(3) : '-';
    }
    return filter;
});