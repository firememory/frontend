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
    otherwise({
        redirectTo: '/'
    });
}

tradeApp.config(routeConfig);

function BidAskCtrl($scope, $http) {
    $scope.orders = [];
    $scope.bid = {type: 'bid', price: 4419, amount: 0, total: 0};
    $scope.ask = {type: 'ask', price: 4419, amount: 0, total: 0};
    $scope.account = {RMB: 'loading', BTC: 'loading'}

    $scope.refresh = function() {
        $http.get('api/account')
            .success(function(data, status, headers, config) {
                console.log('got', data);
                $scope.account = data;
        });

        $http.get('api/depth')
          .success(function(data, status, headers, config) {
            $scope.depth = data;
            $scope.depth.asks.reverse();
        });

        $http.get('api/order')
        .success(function(data, status, headers, config) {
            if (data.orders) {
                    $scope.orders = data.orders;
                }
        });
    };

    $scope.refresh();

        $http.get('api/history')
          .success(function(data, status, headers, config) {
            $scope.history = [];
            data.forEach(function(row) {
              $scope.history.push([row[0]*1000, row[3], row[5], row[6], row[4], row[7]]);
            });

            var chart = $('.candle-chart').jqCandlestick({
              data: $scope.history,
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
        if(!$scope.account || !$scope.account.RMB || !$scope.bid.price || !$scope.bid.amount)
            return;
        var total = $scope.bid.price * $scope.bid.amount;
        if(total > $scope.account.RMB) {
            total = $scope.account.RMB;
            updateBidAmount();
        }
        $scope.bid.total = total;
        console.log('update bid total', $scope.bid.price, $scope.bid.amount, $scope.bid.total);
    };

    var updateBidAmount = function() {
        $scope.bid.amount = Math.round($scope.bid.total / $scope.bid.price * 10000)/10000;
        console.log('update bid amount', $scope.bid.total, $scope.bid.price, $scope.bid.amount);
    };

    var updateAskTotal = function() {
        console.log('update ask total', $scope.ask.price, $scope.ask.amount);
        if(!$scope.account || !$scope.account.RMB || !$scope.ask.price || !$scope.ask.amount)
            return;
        var total = $scope.ask.price * $scope.ask.amount;
    };

    var updateAskAmount = function() {
        console.log('update ask amount', $scope.ask.total, $scope.ask.price);
        $scope.ask.amount = $scope.ask.total / $scope.ask.price;
    };

    $scope.addBidOrder = function() {
        console.log('add bid', $scope.bid);
        if($scope.bid.amount <= 0)
            return;
        $scope.account.RMB = ($scope.account.RMB - $scope.bid.total).toFixed(2);
        $scope.bid.total = Math.min($scope.bid.total, $scope.account.RMB);
        $scope.orders.push({
            "date": new Date().getTime(),
            "tid": new Date().getTime(),
            "price": $scope.bid.price,
            "amount": $scope.bid.amount,
            "type": "buy",
            "status": 10
        });

        $http.post('trade/bid', $scope.bid)
          .success(function(data, status, headers, config) {
            console.log('bid order sent');
        });
    };

    $scope.addAskOrder = function() {
        console.log('add ask', $scope.ask);
        if($scope.ask.amount <= 0)
            return;
        $scope.orders.push({
            "date": new Date().getTime(),
            "tid": new Date().getTime(),
            "price": $scope.ask.price,
            "amount": $scope.ask.amount,
            "type": "sell",
            "status": 10
        });

        $http.post('trade/bid', $scope.ask)
          .success(function(data, status, headers, config) {
            console.log('bid order sent');
        });
    };

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

//    $scope.$watch('bid.amount', updateBidTotal);
//    $scope.$watch('bid.price', updateBidTotal);
//    $scope.$watch('bid.total', updateBidAmount);
//    $scope.$watch('ask.amount', updateAskTotal);
//    $scope.$watch('ask.price', updateAskTotal);
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
        if(input == 10)
            return '等待处理';
        return '';
    }
    return filter;
});