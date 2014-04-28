var tradeApp = angular.module('coinport.trade', ['ui.bootstrap', 'ngResource', 'ngRoute', 'coinport.app', 'navbar', 'timer']);

function routeConfig($routeProvider) {
    $routeProvider.
    when('/:market', {
        controller: 'BidAskCtrl',
        templateUrl: 'views/bidask.html'
    }).
    otherwise({
        redirectTo: '/btccny'
    });
}
function httpConfig($httpProvider) {
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
}

tradeApp.config(routeConfig);
tradeApp.config(httpConfig);

function BidAskCtrl($scope, $http, $routeParams) {
    $scope.market = $routeParams.market.toUpperCase();
    $scope.subject = $scope.market.substr(0, 3);
    $scope.currency = $scope.market.substr(3);
    $scope.orders = [];
    $scope.transactions = [];
    $scope.bid = {price: 0, amount: 0, total: 0};
    $scope.ask = {type: 'ask', price: 0, amount: 0, total: 0};
    $scope.account = {};
    $scope.bidOptions = {limitPrice: true, limitAmount: true, limitTotal: false, advanced: false};
    $scope.askOptions = {limitPrice: true, limitAmount: true, limitTotal: false, advanced: false};
    $scope.config = {
        bidButtonLabel: '买入 ' + $scope.subject + '-' + $scope.currency,
        askButtonLabel: '卖出 ' + $scope.currency + '-' + $scope.subject};
    $scope.info = {
        fundingLocked: 0,
        fundingRemaining: 0,
        quantityLocked: 0,
        quantityRemaining: 0,
        income: 0,
        bidMessage: '',
        askMessage: '',
        bidButtonLabel: $scope.config.bidButtonLabel,
        askButtonLabel: $scope.config.askButtonLabel};

    $scope.updateOrders = function() {
        $http.get('/api/order')
            .success(function(data, status, headers, config) {
                $scope.orders = data.data;
        });
    };

    $scope.updateTransactions = function() {
        $http.get('/api/' + $scope.market + '/transaction', {params: {limit: 15, skip: 0}})
        .success(function(data, status, headers, config) {
            console.log('transactions', data);
            $scope.transactions = data.data;
            if ($scope.transactions.length > 0) {
                $scope.lastPrice = $scope.transactions[0].price;
            }
        });
    };

    $scope.updateDepth = function() {
        $http.get('/api/' + $scope.market + '/depth')
            .success(function(data, status, headers, config) {
                $scope.depth = data.data;
                $scope.depth.asks.reverse();
        });
    };

    $scope.updateBestPrice = function() {
        $http.get('/api/' + $scope.market + '/depth')
            .success(function(data, status, headers, config) {
                $scope.ask.price = data.data.bids[0].price;
                $scope.bid.price = data.data.asks[0].price;
        });
    };

    $scope.refresh = function() {
        $scope.updateDepth();
        $scope.updateTransactions();
    };

    $scope.updateOrders();
    $scope.updateDepth();
    $scope.updateTransactions();
    $scope.updateBestPrice();

    $http.get('/api/account/' + $scope.uid)
        .success(function(data, status, headers, config) {
            $scope.account = data.data.accounts;
    });

    $http.get('/api/' + $scope.market + '/history', {params: {period: 5}})
      .success(function(data, status, headers, config) {
        $scope.history = data.data
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
            dataLeftOffset: Math.max(0, $scope.history.length - 60),
            minDataLength: 30,
            dataRightOffset: $scope.history.length - 1
          },
          series: [{
            type: 'candlestick',
            names: ['开盘','最高', '最低', '收盘'],
            upStroke: '#006633',
            upColor: '#ffffff',
            downStroke: '#CC3333',
            downColor: '#ffffff'
          }, {
            type: 'volume',
            name: '成交量',
            dataOffset: 5,
            yAxis: 1,
            stroke: '#8b4787',
            color: '#8b4787',
            upStroke: '#006633',
            upColor: '#99CC99',
            downStroke: '#CC3333',
            downColor: '#CC9999'
          }]
        });
      });

    var updateBidTotal = function() {
        if(!$scope.account || $scope.account[$scope.currency] == undefined || $scope.bid.price == undefined || $scope.bid.amount == undefined)
            return;
        var total = $scope.bid.price * $scope.bid.amount;
        if(total > $scope.account[$scope.currency]) {
            total = $scope.account[$scope.currency];
//            updateBidAmount();
        }
        $scope.bid.total = total
        $scope.info.fundingLocked = total;
        $scope.info.fundingRemaining = $scope.account[$scope.currency] - total;
        console.log('update bid total', $scope.bid.price, $scope.bid.amount, $scope.bid.total);
    };

    var updateBidAmount = function() {
        $scope.bid.amount = Math.round($scope.bid.total / $scope.bid.price * 10000)/10000;
        console.log('update bid amount', $scope.bid.total, $scope.bid.price, $scope.bid.amount);
    };

    var updateAskTotal = function() {
        console.log('update ask total', $scope.account, $scope.ask.price, $scope.ask.amount);
        if(!$scope.account || $scope.account[$scope.currency] == undefined || $scope.ask.price == undefined || $scope.ask.amount == undefined)
            return;
        var total = $scope.ask.price * $scope.ask.amount;

        $scope.info.income = total
        $scope.info.quantityLocked = $scope.ask.amount;
        $scope.info.quantityRemaining = $scope.account[$scope.subject] - $scope.info.quantityLocked;
    };

    var updateAskAmount = function() {
        console.log('update ask amount', $scope.ask.total, $scope.ask.price);
        $scope.ask.amount = $scope.ask.total / $scope.ask.price;
    };

    var cancelOrder = function(id) {
        for(var i = 0; i < $scope.orders.length; i++) {
            var order = $scope.orders[i];
            if (order.id == id) {
                order.status = 3; // set status to 3-Cancelled
                break;
            }
        }
    }

    $scope.addBidOrder = function() {
        if($scope.bid.amount < 0) {
            $scope.info.bidMessage = '数量不能小于0';
            return;
        }
        if ($scope.bid.total > $scope.account[$scope.currency]) {
            $scope.info.bidMessage = '余额不足';
            return;
        }
        if ($scope.bidOptions.limitAmount && $scope.bid.amount <= 0) {
            $scope.info.bidMessage = '请输入数量';
            return;
        }
        if ($scope.bidOptions.limitPrice && $scope.bid.price <= 0) {
            $scope.info.bidMessage = '请输入价格';
            return;
        }

        $scope.info.bidButtonLabel = '提交订单中...';
        var payload = {type: 'bid'};
        if ($scope.bidOptions.limitPrice)
            payload.price = $scope.bid.price;
        if ($scope.bidOptions.limitAmount)
            payload.amount = $scope.bid.amount;
        if ($scope.bidOptions.limitTotal)
            payload.total = $scope.bid.total;

        $http.post('/trade/' + $scope.market + '/bid', $.param(payload))
          .success(function(data, status, headers, config) {
            console.log('bid order sent, response:', data);
            $scope.info.bidButtonLabel = $scope.config.bidButtonLabel;
            if (data.success) {
                var order = data.data;
                $scope.account[$scope.currency] -= order.total;
                $scope.orders.push(order);
            } else {
                // handle errors
            }
            $scope.info.bidMessage = data.message;
        });
    };

    $scope.addAskOrder = function() {
        if($scope.ask.amount < 0) {
            $scope.info.askMessage = '数量不能小于0';
            return;
        }
        if ($scope.ask.amount > $scope.account[$scope.subject]) {
            $scope.info.askMessage = '余额不足';
            return;
        }
        if ($scope.askOptions.limitAmount && $scope.ask.amount <= 0) {
            $scope.info.askMessage = '请输入数量';
            return;
        }
        if ($scope.askOptions.limitPrice && $scope.ask.price <= 0) {
            $scope.info.askMessage = '请输入价格';
            return;
        }

        $scope.info.askButtonLabel = '提交订单中...';
        var payload = {type: 'ask'};
        if ($scope.askOptions.limitPrice)
            payload.price = $scope.ask.price;
        if ($scope.askOptions.limitAmount)
            payload.amount = $scope.ask.amount;
        if ($scope.askOptions.limitTotal)
            payload.total = $scope.ask.total;

        $http.post('/trade/' + $scope.market + '/ask', $.param(payload))
          .success(function(data, status, headers, config) {
            console.log('bid order sent, response:', data);
            $scope.info.askButtonLabel = $scope.config.askButtonLabel;
            if (data.success) {
                var order = data.data;
                $scope.orders.push(order);
            } else {
                // handle errors
            }
            $scope.info.askMessage = data.message;
        });
    };

    $scope.clickFunding = function(amount) {
        $scope.bid.total = amount;
        $scope.bidOptions.limitTotal = true;
        $scope.info.fundingLocked = amount;
        $scope.info.fundingRemaining = $scope.account[$scope.currency] - amount;
    }

    $scope.clickQuantity = function(quantity) {
        $scope.ask.amount = quantity;
        $scope.askOptions.limitAmount = true;
    }

    $scope.clickDepthBids = function(index) {
        var data = $scope.depth.bids
        if (index < 0 || index >= data.length) return;

        var amount = 0;
        for(var i = 0; i <= index; i++) {
            amount += data[i].amount;
        }
        var price = data[index].price;

        var target = $scope.ask

        target.price = price;
        target.amount = amount;
    }

    $scope.clickDepthAsks = function(index) {
            var data = $scope.depth.asks
            if (index < 0 || index >= data.length) return;

            var amount = 0;
            for(var i = data.length - 1; i >= index; i--) {
                amount += data[i].amount;
            }
            var price = data[index].price;

            var target = $scope.bid

            target.price = price;
            target.amount = amount;
    }

    $scope.cancelOrder = function(id) {
        $http.get('/trade/' + $scope.market + '/order/cancel/' + id)
            .success(function(data, status, headers, config) {
                if (data.success) {
                    var order = data.data;
                    cancelOrder(order.id);
                }
            });
    };

    // polling
    $scope.$on('timer-tick', function (event, args) {
        $scope.refresh();
    });

    $scope.$watch('bid.amount', updateBidTotal);
    $scope.$watch('bid.price', updateBidTotal);
//    $scope.$watch('bid.total', updateBidAmount);
    $scope.$watch('ask.amount', updateAskTotal);
    $scope.$watch('ask.price', updateAskTotal);
//    $scope.$watch('ask.total', updateAskAmount);
}