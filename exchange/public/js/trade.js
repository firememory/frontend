var tradeApp = angular.module('coinport.trade', ['ui.bootstrap', 'ngResource', 'ngRoute', 'ngAnimate', 'coinport.app', 'navbar', 'timer']);

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
    $scope.historyPeriod = 1; // 1 - minute K
    $scope.historyUpdateTime = 5000; // polling period in milliseconds
    $scope.subject = $scope.market.substr(0, 3);
    $scope.currency = $scope.market.substr(3);
    $scope.orders = [];
    $scope.bid = {price: 0, amount: 0, total: 0};
    $scope.ask = {price: 0, amount: 0, total: 0};
    $scope.account = {};
    $scope.showMessage = {bid: false, ask: false};
    $scope.bidOptions = {limitPrice: true, limitAmount: true, limitTotal: true, advanced: false};
    $scope.askOptions = {limitPrice: true, limitAmount: true, limitTotal: true, advanced: false};
    $scope.config = {
        bidButtonLabel: Messages.buy + ' (' + $scope.subject + '-' + $scope.currency + ')',
        askButtonLabel: Messages.sell + ' (' + $scope.currency + '-' + $scope.subject + ')'};
    $scope.info = {
        fundingLocked: 0,
        fundingRemaining: 0,
        quantityLocked: 0,
        quantityRemaining: 0,
        income: 0,
        message: {},
        bidButtonLabel: $scope.config.bidButtonLabel,
        askButtonLabel: $scope.config.askButtonLabel};

    $scope.orderStatus = -1;

    $scope.alert = function(operation, message) {
        console.log(operation, message)
        $scope.showMessage[operation] = true;
        $scope.info.message[operation] = message;
        setTimeout(function() {
            $scope.showMessage[operation] = false;
        }, 3000);
    };

    $scope.loadOrders = function() {
        var params = {limit: 10};
        if ($scope.orderStatus >= 0)
            params.status = $scope.orderStatus;
        $http.get('/api/' + $scope.market + '/order', {params: params})
            .success(function(data, status, headers, config) {
                $scope.orders = data.data.items;
                $scope.count = data.data.count;
        });
    };

    $scope.changeOrderStatus = function(status) {
        $scope.orderStatus = status;
        $scope.loadOrders();
    };

    $scope.updateTransactions = function() {
        $http.get('/api/' + $scope.market + '/transaction', {params: {limit: 15, skip: 0}})
        .success(function(data, status, headers, config) {
            if(!$scope.transactions) {
                $scope.transactions = data.data;
            } else {
                for(var i = 0; i < data.data.items.length; i ++) {
                    if ($scope.transactions.items[0].id == data.data.items[i].id)
                        break;
                }
                for(var j = i - 1; j >= 0; j--) {
                    $scope.transactions.items.unshift(data.data.items[j]);
                    if ($scope.transactions.length > data.data.items.length)
                        $scope.transactions.items.pop();
                }
            }

            if ($scope.transactions.items.length > 0) {
                $scope.lastPrice = $scope.transactions.items[0].price.value;
            }
        });
    };

    $scope.updateDepth = function() {
        $http.get('/api/' + $scope.market + '/depth')
            .success(function(response, status, headers, config) {
                response.data.asks.reverse();
                $scope.depth = response.data;
        });
    };

    $scope.updateBestPrice = function() {
        $http.get('/api/' + $scope.market + '/depth')
            .success(function(data, status, headers, config) {
                if (data.data.bids.length > 0 )
                    $scope.ask.price = data.data.bids[0].price || 0;
                if (data.data.asks.length > 0 )
                    $scope.bid.price = data.data.asks[0].price || 0;
        });
    };

    $scope.refresh = function() {
        $scope.updateDepth();
        $scope.updateTransactions();
    };

    $scope.loadOrders();
    $scope.updateDepth();
    $scope.updateTransactions();
    $scope.updateBestPrice();

    $http.get('/api/account/' + $scope.uid)
        .success(function(data, status, headers, config) {
            $scope.account = data.data.accounts;
    });

    $scope.updateHistory = function() {
         $http.get('/api/' + $scope.market + '/history', {params: {period: 5}})
              .success(function(response, status, headers, config) {
                    var data = response.data;
              });
    };

    var splitHistoryData = function(data) {
        // split the data set into ohlc and volume
        var ohlc = [];
        var volume = [];
        $scope.history.forEach(function(data){
            ohlc.push([
                data[0], // the date
                data[1], // open
                data[2], // high
                data[3], // low
                data[4] // close
            ]);

            volume.push([
                data[0], // the date
                data[5] // the volume
            ]);
        });
        return {ohlc: ohlc, volume: volume};
    };

    var getMA = function(data, n) {
        var ma = [];
        if (!data || n > data.length)
            return ma;
        var sum = 0;
        for (var i = 0; i < n; i++) {
            sum += data[i][4];
            ma.push([data[i][0], sum / (i + 1)]);
        }

        for (var i = n; i < data.length; i++) {
            var row = data[i];
            var time = row[0];
            sum = sum - data[i - n][4] + row[4];
            ma.push([time, sum / n]);
        }
        return ma;
    };

    $http.get('/api/' + $scope.market + '/history', {params: {period: $scope.historyPeriod}})
      .success(function(response, status, headers, config) {
        $scope.history = response.data.candles;
        $scope.ma7 = getMA($scope.history, 7);
        $scope.ma30 = getMA($scope.history, 30);
        $scope.lastHistory = $scope.history[$scope.history.length - 1];

        // set the allowed units for data grouping
        var groupingUnits = [[
            'minute', // unit name
            [1, 3, 5, 15, 30] // allowed multiples
        ], [
            'hour',
            [1]
        ]];

        // create the chart
        Highcharts.setOptions({
            global: {
                useUTC: false
            }
        });
        $('.candle-chart').highcharts('StockChart', {
            chart : {
                events : {
                    load : function() {
                        var candleSeries = this.series[0];
                        var volumeSeries = this.series[1];
                        var ma7Series = this.series[2];
                        var ma30Series = this.series[3];
                        // polling new data
                        setInterval(function() {
                            $http.get('/api/' + $scope.market + '/history', {params: {period: $scope.historyPeriod, from: $scope.lastHistory[0]}})
                              .success(function(response, status, headers, config) {
                                    var data = response.data.candles;
                                    if (!data || data.length == 0)
                                        return;
                                    // merge candle data
                                    var last = $scope.history.pop();
                                    data.forEach(function(item){
                                        if (item[1] == 0){
                                            item[1] = last[1];
                                            item[2] = last[2];
                                            item[3] = last[3];
                                            item[4] = last[4];
                                        }
                                    });
                                    $scope.history = $scope.history.concat(data);
                                    $scope.lastHistory = data[data.length - 1];
                                    // update MA data
                                    $scope.ma7 = getMA($scope.history, 7);
                                    $scope.ma30 = getMA($scope.history, 30);

                                    // set data
                                    candleSeries.setData(splitHistoryData($scope.history).ohlc, true, true, false);
                                    volumeSeries.setData(splitHistoryData($scope.history).volume, true, true, false);
                                    ma7Series.setData($scope.ma7, true, true, false);
                                    ma30Series.setData($scope.ma30, true, true, false);
                            });
                        }, $scope.historyUpdateTime);
                    }
                }
            },
            rangeSelector : {
                enabled: true,
                buttons : [
                {
                    type : 'day',
                    count : 1,
                    text : '1D'
                },{
                    type : 'hour',
                    count : 1,
                    text : '1h'
                },{
                    type : 'all',
                    count : 1,
                    text : 'All'
                }],
                selected : 1,
                inputEnabled : false
            },
            navigator: {
                enabled: true,
                height: 20,
                margin: 5
            },
            scrollbar: {
                enabled: false,
            },
            yAxis: [{
                height: '75%'
            },{
                top: '80%',
                height: '20%'
            }],
            series: [{
                type: 'candlestick',
                name: $scope.subject + '-' + $scope.currency,
                data: splitHistoryData($scope.history).ohlc,
                dataGrouping: {
                    units: groupingUnits
                }
            },{
                type: 'column',
                name: 'Volume',
                data: splitHistoryData($scope.history).volume,
                yAxis: 1,
                dataGrouping: {
                    units: groupingUnits
                }
            },{
                type: 'spline',
                name: 'MA7',
                data: $scope.ma7,
                color: '#660033',
                lineWidth: 1,
                yAxis: 0,
                threshold: null,
                tooltip: {
                    valueDecimals: 2
                },
                dataGrouping: {
                    units: groupingUnits
                }
            },{
                type: 'spline',
                name: 'MA30',
                data: $scope.ma30,
                color: '#666633',
                lineWidth: 1,
                yAxis: 0,
                threshold: null,
                tooltip: {
                    valueDecimals: 2
                },
                dataGrouping: {
                    units: groupingUnits
                }
            }]
        });
    });

    var updateBidTotal = function() {
        if($scope.bid.price == undefined || $scope.bid.amount == undefined)
            return;
        var total = +($scope.bid.price * $scope.bid.amount).toFixed(COINPORT.getAmountFixed($scope.currency));
        $scope.bid.total = total;
        console.log('update bid total', $scope.bid.price, $scope.bid.amount, $scope.bid.total);
    };

    var updateBidAmount = function() {
        if (!$scope.bid.price)
            $scope.bid.amount = 0;
        else
            $scope.bid.amount = +($scope.bid.total / $scope.bid.price).toFixed(COINPORT.getAmountFixed($scope.subject));
    };

    var updateAskTotal = function() {
        if(!$scope.account || $scope.account[$scope.currency] == undefined || $scope.ask.price == undefined || $scope.ask.amount == undefined)
            return;
        var total = +($scope.ask.price * $scope.ask.amount).toFixed(COINPORT.getAmountFixed($scope.currency));
        console.log('update ask total', $scope.account, $scope.ask.price, $scope.ask.amount);
        $scope.ask.total = total;
    };

    var updateAskAmount = function() {
        if (!$scope.ask.price)
            $scope.ask.amount = 0;
        else
            $scope.ask.amount = +($scope.ask.total / $scope.ask.price).toFixed(COINPORT.getAmountFixed($scope.subject));
    };

    var toggleBidAdvanced = function(newValue, oldValue) {
        if (!newValue && oldValue) {
            $scope.bidOptions.limitAmount = true;
            $scope.bidOptions.limitPrice = true;
            $scope.bidOptions.limitTotal = true;
//            addBidWatches();
        } else if (newValue && !oldValue) {
//            removeBidWatches();
        }
    }

    var toggleAskAdvanced = function(newValue, oldValue) {
        if (!newValue && oldValue) {
            $scope.askOptions.limitAmount = true;
            $scope.askOptions.limitPrice = true;
            $scope.askOptions.limitTotal = true;
//            addAskWatches();
        } else if (newValue && !oldValue) {
//            removeAskWatches();
        }
    }

    $scope.addBidOrder = function() {
        if($scope.bid.amount < 0) {
            $scope.alert('bid', Messages.trade.lowerZero);
            return;
        }
        if ($scope.bid.total > $scope.account[$scope.currency]) {
            $scope.alert('bid', Messages.trade.noEnough);
            return;
        }
        if ($scope.bidOptions.limitAmount && $scope.bid.amount <= 0) {
            $scope.alert('bid', Messages.trade.inputAmount);
            return;
        }
        if ($scope.bidOptions.limitPrice && $scope.bid.price <= 0) {
            $scope.alert('bid', Messages.trade.inputPrice);
            return;
        }

        $scope.info.bidButtonLabel = Messages.trade.submit;
        var payload = {type: 'bid'};
        if (!$scope.bidOptions.advanced || $scope.bidOptions.limitPrice)
            payload.price = $scope.bid.price;
        if (!$scope.bidOptions.advanced || $scope.bidOptions.limitAmount)
            payload.amount = $scope.bid.amount;
        if ($scope.bidOptions.advanced && $scope.bidOptions.limitTotal)
            payload.total = $scope.bid.total;

        $http.post('/trade/' + $scope.market + '/bid', $.param(payload))
          .success(function(data, status, headers, config) {
            console.log('bid order sent, request:', payload, ' response:', data);
            $scope.info.bidButtonLabel = $scope.config.bidButtonLabel;
            if (data.success) {
                var order = data.data;
                $scope.account[$scope.currency].available.value -= order.total;
                $scope.orders.push(order);
                $scope.alert('bid', 'order submitted');
                setTimeout($scope.loadOrders, 1000);
            } else {
                // handle errors
                $scope.alert('bid', 'order submission failed');
            }
            // clear amount
            $scope.bid.amount = 0;
            $scope.bid.total = 0;
        }).error(function() {
            $scope.alert('bid', 'internal error occurs');
            $scope.info.bidButtonLabel = $scope.config.bidButtonLabel;
        });
    };

    $scope.addAskOrder = function() {
        if($scope.ask.amount < 0) {
            $scope.alert('ask', Messages.trade.lowerZero);
            return;
        }
        if ($scope.ask.amount > $scope.account[$scope.subject].available.value) {
            $scope.alert('ask', Messages.trade.noEnough);
            return;
        }
        if ($scope.askOptions.limitAmount && $scope.ask.amount <= 0) {
            $scope.alert('ask', Messages.trade.inputAmount);
            return;
        }
        if ($scope.askOptions.limitPrice && $scope.ask.price <= 0) {
            $scope.alert('ask', Messages.trade.inputPrice);
            return;
        }

        $scope.info.askButtonLabel = Messages.trade.submit;
        var payload = {type: 'ask'};
        if (!$scope.askOptions.advanced || $scope.askOptions.limitPrice)
            payload.price = $scope.ask.price;
        if (!$scope.askOptions.advanced || $scope.askOptions.limitAmount)
            payload.amount = $scope.ask.amount;
        if ($scope.askOptions.advanced && $scope.askOptions.limitTotal)
            payload.total = $scope.ask.total;

        $http.post('/trade/' + $scope.market + '/ask', $.param(payload))
          .success(function(data, status, headers, config) {
            console.log('ask order sent, request:', payload, ' response:', data);
            $scope.info.askButtonLabel = $scope.config.askButtonLabel;
            if (data.success) {
                var order = data.data;
                $scope.orders.push(order);
                $scope.account[$scope.subject].available.value -= order.amount;
                $scope.alert('ask', 'order submitted');
                setTimeout($scope.loadOrders, 1000);
            } else {
                $scope.alert('ask', 'order submission failed');
            }

            // clear amount
            $scope.ask.amount = 0;
            $scope.ask.total = 0;
        }).error(function() {
            $scope.alert('ask', 'internal error occurs');
            $scope.info.askButtonLabel = $scope.config.askButtonLabel;
        });
    };

    $scope.clickFunding = function(amount) {
        if (!amount)
            return;
        $scope.bid.total = +amount;
        $scope.bidOptions.limitTotal = true;
        updateBidAmount();
    }

    $scope.clickQuantity = function(quantity) {
        $scope.ask.amount = +quantity;
        $scope.askOptions.limitAmount = true;
        updateAskTotal();
    }

    $scope.clickDepthBids = function(index) {
        var data = $scope.depth.bids
        if (index < 0 || index >= data.length) return;

        var amount = 0;
        for(var i = 0; i <= index; i++) {
            amount += data[i].amount;
        }
        var price = data[index].price;
        $scope.ask.price = price;
        $scope.ask.amount = amount;
        updateAskTotal();
    }

    $scope.clickDepthAsks = function(index) {
            var data = $scope.depth.asks
            if (index < 0 || index >= data.length) return;

            var amount = 0;
            for(var i = data.length - 1; i >= index; i--) {
                amount += data[i].amount;
            }
            var price = data[index].price;
            $scope.bid.price = price;
            $scope.bid.amount = amount;
            updateBidTotal();
    }

    $scope.cancelOrder = function(id) {
        $http.get('/trade/' + $scope.market + '/order/cancel/' + id)
            .success(function(data, status, headers, config) {
                if (data.success) {
                    setTimeout($scope.loadOrders, 1000);
                }
            });
    };

    // polling
    $scope.$on('timer-tick', function (event, args) {
        $scope.refresh();
    });

    // watch
    var watchBidPrice = function(newValue) {
        var fixed = COINPORT.getPriceFixed($scope.market);
        if (newValue == null)
            $scope.bid.price = 0;
        else if (newValue < 0)
            $scope.bid.price = -newValue;
        else
            $scope.bid.price = +newValue.toFixed(fixed);
        updateBidTotal();
    };
    var watchBidAmount = function(newValue) {
        if (newValue == null)
            $scope.bid.amount = 0;
        else if (newValue < 0)
            $scope.bid.amount = -newValue;
        else
            $scope.bid.amount = +newValue.toFixed(COINPORT.getAmountFixed($scope.subject));
        updateBidTotal();
    };
    var watchAskPrice = function(newValue) {
        var fixed = COINPORT.getPriceFixed($scope.market);
        if (newValue == null)
            $scope.ask.price = 0;
        else if (newValue < 0)
            $scope.ask.price = -newValue;
        else
            $scope.ask.price = +newValue.toFixed(fixed);
        updateAskTotal();
    };
    var watchAskAmount = function(newValue) {
        if (newValue == null)
            $scope.ask.amount = 0;
        else if (newValue < 0)
            $scope.ask.amount = -newValue;
        else
            $scope.ask.amount = +newValue.toFixed(COINPORT.getAmountFixed($scope.subject));
        updateAskTotal();
    };

    var bidWatches = [];
    var askWatches = [];

//    var addBidWatches = function() {
//        bidWatches.push($scope.$watch('bid.amount', watchBidAmount));
//        bidWatches.push($scope.$watch('bid.price', watchBidPrice));
//        bidWatches.push($scope.$watch('bid.total', updateBidAmount));
//    };
//
//    var addAskWatches = function() {
//        askWatches.push($scope.$watch('ask.amount', watchAskAmount));
//        askWatches.push($scope.$watch('ask.price', watchAskPrice));
//        askWatches.push($scope.$watch('ask.total', updateAskAmount));
//    };
//
//    var removeBidWatches = function() {
//        bidWatches.forEach(function(fn) {fn.apply();});
//    };
//
//    var removeAskWatches = function() {
//        askWatches.forEach(function(fn) {fn.apply();});
//    };

    $scope.$watch('bidOptions.advanced', toggleBidAdvanced);
    $scope.$watch('askOptions.advanced', toggleAskAdvanced);

    $('#bid_price').keyup(updateBidTotal);
    $('#bid_amount').keyup(updateBidTotal);
    $('#bid_total').keyup(updateBidAmount);

    $('#ask_price').keyup(updateAskTotal);
    $('#ask_amount').keyup(updateAskTotal);
    $('#ask_total').keyup(updateAskAmount);
}