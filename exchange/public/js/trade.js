var tradeApp = angular.module('coinport.trade', ['ui.bootstrap', 'ngResource', 'ngRoute', 'coinport.app', 'navbar', 'timer']);

function routeConfig($routeProvider) {
    $routeProvider.
    when('/', {
        controller: 'BidAskCtrl',
        templateUrl: 'views/bidask.html'
    }).
    when('/deposit/rmb', {
        controller: 'DepositRmbCtrl',
        templateUrl: 'views/deposit-CNY.html'
    }).
    when('/deposit/btc', {
        controller: 'DepositBtcCtrl',
        templateUrl: 'views/deposit-BTC.html'
    }).
    when('/withdrawal/rmb', {
        controller: 'WithdrawalRmbCtrl',
        templateUrl: 'views/withdrawal-CNY.html'
    }).
    when('/withdrawal/btc', {
        controller: 'WithdrawalBtcCtrl',
        templateUrl: 'views/withdrawal-BTC.html'
    }).
    when('/asset', {
            controller: 'AssetCtrl',
            templateUrl: 'views/asset.html'
    }).
    when('/order', {
            controller: 'OrderDetailCtrl',
            templateUrl: 'views/order.html'
    }).
    when('/orders', {
            controller: 'UserOrderCtrl',
            templateUrl: 'views/orders.html'
    }).
    when('/transaction', {
        controller: 'UserTxCtrl',
        templateUrl: 'views/transactions.html'
    }).
    when('/test', {
            controller: 'DepositBtcCtrl',
            templateUrl: 'views/test.html'
    }).
    otherwise({
        redirectTo: '/'
    });
}
function httpConfig($httpProvider) {
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
}

tradeApp.config(routeConfig);
tradeApp.config(httpConfig);

function BidAskCtrl($scope, $http) {
    $scope.orders = [];
    $scope.transactions = [];
    $scope.bid = {price: 4000, amount: 0, total: 0};
    $scope.ask = {type: 'ask', price: 5000, amount: 0, total: 0};
    $scope.account = {CNY: {}, BTC: {}}
    $scope.bidOptions = {limitPrice: true, limitAmount: true, limitTotal: false, advanced: false};
    $scope.askOptions = {limitPrice: true, limitAmount: true, limitTotal: false, advanced: false};
    $scope.config = {
        bidButtonLabel: '买入 CNY-BTC',
        askButtonLabel: '卖出 BTC-CNY'};
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
        $http.get('api/order')
            .success(function(data, status, headers, config) {
                $scope.orders = data.data;
        });
    };

    $scope.updateTransactions = function() {
        $http.get('api/transaction', {params: {limit: 15, skip: 0}})
        .success(function(data, status, headers, config) {
            console.log('transactions', data);
            $scope.transactions = data.data;
            if ($scope.transactions.length > 0) {
                $scope.lastPrice = $scope.transactions[0].price;
            }
        });
    };

    $scope.updateDepth = function() {
        $http.get('api/depth')
            .success(function(data, status, headers, config) {
                $scope.depth = data.data;
                $scope.depth.asks.reverse();
        });
    };

    $scope.updateBestPrice = function() {
        $http.get('api/depth')
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

    $http.get('api/account/' + $scope.uid)
        .success(function(data, status, headers, config) {
            $scope.account = data.data.accounts;
    });

    $http.get('api/history', {params: {period: 5}})
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
        if(!$scope.account || $scope.account.CNY == undefined || $scope.bid.price == undefined || $scope.bid.amount == undefined)
            return;
        var total = $scope.bid.price * $scope.bid.amount;
        if(total > $scope.account.CNY) {
            total = $scope.account.CNY;
//            updateBidAmount();
        }
        $scope.bid.total = total
        $scope.info.fundingLocked = total;
        $scope.info.fundingRemaining = $scope.account.CNY - total;
        console.log('update bid total', $scope.bid.price, $scope.bid.amount, $scope.bid.total);
    };

    var updateBidAmount = function() {
        $scope.bid.amount = Math.round($scope.bid.total / $scope.bid.price * 10000)/10000;
        console.log('update bid amount', $scope.bid.total, $scope.bid.price, $scope.bid.amount);
    };

    var updateAskTotal = function() {
        console.log('update ask total', $scope.account, $scope.ask.price, $scope.ask.amount);
        if(!$scope.account || $scope.account.CNY == undefined || $scope.ask.price == undefined || $scope.ask.amount == undefined)
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
        if ($scope.bid.total > $scope.account.CNY) {
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

        $http.post('trade/bid', $.param(payload))
          .success(function(data, status, headers, config) {
            console.log('bid order sent, response:', data);
            $scope.info.bidButtonLabel = $scope.config.bidButtonLabel;
            if (data.success) {
                var order = data.data;
                $scope.account.CNY -= order.total;
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
        if ($scope.ask.amount > $scope.account.BTC) {
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

        $http.post('trade/bid', $.param(payload))
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
        $scope.info.fundingRemaining = $scope.account.CNY - amount;
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
        $http.get('trade/BTCCNY/order/cancel/' + id)
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

tradeApp.controller('DepositRmbCtrl', ['$scope', '$http', function($scope, $http) {
    $http.get('/api/account/' + $scope.uid)
      .success(function(data, status, headers, config) {
        $scope.balance = data.data.accounts['CNY'];
    });

    $http.get('/api/CNY/transfer/' + $scope.uid, {params: {'type': 0}})
      .success(function(data, status, headers, config) {
        $scope.deposits = data.data;
    });

    $scope.depositData = {currency: 'CNY'};
    $scope.deposit = function() {
        var amount = $scope.amount;
        console.log('deposit ' + $scope.depositData.amount);
        $http.post('/account/deposit', $.param($scope.depositData))
          .success(function(data, status, headers, config) {
            var deposit = data.data.transfer;
            alert('充值成功，本次充值' + deposit.amount/100 + '元');
          });
    };
}]);

tradeApp.controller('WithdrawalRmbCtrl', ['$scope', '$http', function($scope, $http) {
    $http.get('/api/account/' + $scope.uid)
        .success(function(data, status, headers, config) {
            $scope.balance = data.data.accounts['CNY'];
        });

    $http.get('/api/CNY/transfer/' + $scope.uid, {params: {'type': 1}})
        .success(function(data, status, headers, config) {
            $scope.withdrawals = data.data;
        });

    $scope.withdrawalData = {currency: 'CNY'};
    $scope.withdrawal = function() {
        var amount = $scope.amount;
        console.log('withdrawal ' , $scope.withdrawalData);
        $http.post('/account/withdrawal', $.param($scope.withdrawalData))
            .success(function(data, status, headers, config) {
                if (data.success) {
                    var withdrawal = data.data.transfer;
                    alert('提现成功，本次提现' + withdrawal.amount/100 + '元');
                } else {
                    alert(data.message);
                }
            });
    };
}]);

tradeApp.controller('DepositBtcCtrl', ['$scope', '$http', function($scope, $http) {
    $http.get('/api/account/' + $scope.uid)
          .success(function(data, status, headers, config) {
            $scope.balance = data.data.accounts['BTC'];
    });

    $http.get('/api/BTC/transfer/' + $scope.uid, {params: {'type': 0}})
      .success(function(data, status, headers, config) {
        $scope.deposits = data.data;
    });

    $scope.depositData = {currency: 'BTC'};
    $scope.deposit = function() {
        var amount = $scope.amount;
        console.log('deposit ' + $scope.depositData.amount);
        $http.post('/account/deposit', $.param($scope.depositData))
          .success(function(data, status, headers, config) {
            var deposit = data.data.transfer;
            alert('充值成功，本次充值' + deposit.amount/1000 + 'BTC');
          });
    };
}]);

tradeApp.controller('WithdrawalBtcCtrl', ['$scope', '$http', function($scope, $http) {
    $http.get('/api/account/' + $scope.uid)
        .success(function(data, status, headers, config) {
            console.log(data.data.accounts['BTC'])
            $scope.balance = data.data.accounts['BTC'];
        });

    $http.get('/api/BTC/transfer/' + $scope.uid, {params: {'type': 1}})
        .success(function(data, status, headers, config) {
            $scope.withdrawals = data.data;
        });

    $scope.withdrawalData = {currency: 'BTC'};
    $scope.withdrawal = function() {
        console.log('withdrawal ' + $scope.withdrawalData.amount);
        $http.post('/account/withdrawal', $.param($scope.withdrawalData))
            .success(function(data, status, headers, config) {
                if (data.success) {
                    var withdrawal = data.data.transfer;
                    alert('提现成功，本次提现' + withdrawal.amount/1000 + 'BTC');
                } else {
                    alert(data.message);
                }
            });
    };
}]);

tradeApp.controller('AssetCtrl', function ($scope, $http) {
    $http.get('api/asset/' + $scope.uid)
        .success(function(data, status, headers, config) {
            $scope.assets = data.data;
            var map = $scope.assets[$scope.assets.length-1].amountMap;
            $scope.pieData = [];
            for(asset in map) {
                $scope.pieData.push([asset, map[asset]]);
            }
            console.log($scope.pieData)
            $scope.updateAsset();
        });

    $scope.updateAsset = function() {
        $http.get('api/account/' + $scope.uid)
            .success(function(data, status, headers, config) {
                $scope.accounts = data.data.accounts;
                var map = $scope.assets[$scope.assets.length-1].amountMap;
                for (currency in $scope.accounts) {
                    var account = $scope.accounts[currency];
                    account.total = account.available.value + account.locked.value + account.pendingWithdrawal.value;
                    account.asset = map[currency];
                }
                console.log($scope.accounts)
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
                    data: $scope.pieData,
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

        var assetData = [];
            $scope.assets.forEach(function(row) {
                var asset = row;
                console.log("asset", asset);
                var time = new Date(asset.timestamp);
                var btc = asset.amountMap["Btc"];
                var cny = asset.amountMap["Cny"];

                assetData.push({date: time, value1: cny, value2: btc});
            });


        // jQuery code
        $('#user-finance-chart-history').jqChart({
            title: { text: '资产走势' },
            dataSource: assetData,
            axes: [
                {
                    type: 'dateTime',
                    location: 'bottom',
                    minimum: new Date($scope.assets[0].timestamp),
                    maximum: new Date($scope.assets[$scope.assets.length-1].timestamp),
                    interval: 1,
                    intervalType: 'minutes'
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
                }
            ]
        });
    };
});

tradeApp.controller('UserTxCtrl', ['$scope', '$http', function($scope, $http) {
    $http.get('/api/user/' + $scope.uid + '/transaction/BTCCNY', {params: {}})
          .success(function(data, status, headers, config) {
                $scope.transactions = data.data;
          });
}]);

tradeApp.controller('UserOrderCtrl', ['$scope', '$http', '$location', function($scope, $http, $location) {
    $http.get('/api/order')
        .success(function(data, status, headers, config) {
            $scope.orders = data.data;
        });

    $scope.showDetail = function(order) {
        $scope.$parent.order = order;
        $location.path('/order');
    };

    $scope.cancelOrder = function(id) {
        $http.get('/trade/BTCCNY/order/cancel/' + id)
            .success(function(data, status, headers, config) {

            });
    };
}]);

tradeApp.controller('OrderDetailCtrl', ['$scope', '$http', function($scope, $http) {
    var order = $scope.order;
    $http.get('/api/order/' + order.id + '/transaction')
        .success(function(data, status, headers, config) {
            $scope.transactions = data.data;
        });
}]);