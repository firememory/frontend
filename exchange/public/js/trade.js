var tradeApp = angular.module('coinport.trade', ['ui.bootstrap', 'ngResource', 'ngRoute', 'ngAnimate', 'coinport.app', 'navbar', 'timer']);

function httpConfig($httpProvider) {
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
}

tradeApp.config(httpConfig);

function BidAskCtrl($scope, $http, $routeParams, $window) {

    if (!$window.location.hash) $window.location.hash = '#/' + COINPORT.defaultMarket;

    $scope.market = $window.location.hash.replace('#/', '').toUpperCase();
    $scope.historyPeriod = 5; // 1 - minute K
    $scope.historyUpdateTime = 1000 * 60; // polling period in milliseconds
    $scope.subject = $scope.market.split("-")[0];
    $scope.currency = $scope.market.split("-")[1];
    $scope.orders = [];
    $scope.recentOrders = [];
    $scope.bid = {price: 0, amount: 0, total: 0, limit: 0};
    $scope.ask = {price: 0, amount: 0, total: 0, limit: 0};
    $scope.account = {};
    $scope.showMessage = {bid: false, ask: false};
    $scope.bidOptions = {limitPrice: true, limitAmount: true, limitTotal: true, limitLImit: true};
    $scope.askOptions = {limitPrice: true, limitAmount: true, limitTotal: true, limitLImit: true};
    $scope.config = {
        bidButtonLabel: Messages.buy,
        askButtonLabel: Messages.sell};
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

    var updateNav = function() {
        $('li').removeClass('active');
        $('#nav-' + $scope.market).addClass('active');
    };

    updateNav();

    $scope.changeMarket = function(market) {
        $scope.market = market;
        $scope.subject = $scope.market.split("-")[0];
        $scope.currency = $scope.market.split("-")[1];

        updateNav();
        reload();
        $scope.updateHistory();
    };

    $scope.alert = function(operation, message) {
        $scope.showMessage[operation] = true;
        $scope.info.message[operation] = message;
        setTimeout(function() {
            $scope.showMessage[operation] = false;
        }, 3000);
    };

    $scope.toLogin = function() {
        $window.location = '/login';
    };

    $scope.loadOrders = function() {
        var params = {limit: 10};
        if ($scope.orderStatus >= 0)
            params.status = $scope.orderStatus;
        $http.get('/api/user/' + $scope.uid + '/order/' + $scope.market, {params: params})
            .success(function(data, status, headers, config) {
                $scope.orders = data.data.items;
                $scope.count = data.data.count;
        });
    };

    $scope.loadRecentOrders = function() {
        var params = {limit: 10, skip: 0};
        $http.get('/api/' + $scope.market + '/orders', {params: params})
            .success(function(data, status, headers, config) {
                $scope.recentOrders = data.data.items;
//                $scope.count = data.data.count;
            });
    };

    $scope.changeOrderStatus = function(status) {
        $scope.orderStatus = status;
        $scope.loadOrders();
    };

    $scope.updateTransactions = function() {
        $http.get('/api/' + $scope.market + '/transaction', {params: {limit: 15, skip: 0}})
        .success(function(data, status, headers, config) {
            $scope.transactions = data.data;
//            if(!$scope.transactions) {
//                $scope.transactions = data.data;
//            } else {
//                for(var i = 0; i < data.data.items.length; i ++) {
//                    if ($scope.transactions.items[0].id == data.data.items[i].id)
//                        break;
//                }
//                for(var j = i - 1; j >= 0; j--) {
//                    $scope.transactions.items.unshift(data.data.items[j]);
//                    if ($scope.transactions.length > data.data.items.length)
//                        $scope.transactions.items.pop();
//                }
//            }

            if ($scope.transactions.items.length > 0) {
                $scope.lastPrice = $scope.transactions.items[0].price;
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
                    $scope.ask.price = data.data.bids[0].price.display || 0;
                if (data.data.asks.length > 0 )
                    $scope.bid.price = data.data.asks[0].price.display || 0;
        });
    };

    $scope.updateAccount = function() {
        if ($scope.uid == 0)
            return;
        $http.get('/api/account/' + $scope.uid)
            .success(function(data, status, headers, config) {
                $scope.account = data.data.accounts;
        });
    };

    var reload = function() {
        $scope.loadOrders();
        $scope.loadRecentOrders();
        $scope.updateDepth();
        $scope.updateTransactions();
        $scope.updateBestPrice();
        $scope.updateAccount();
    };

    var refresh = function() {
        $scope.updateDepth();
        $scope.updateTransactions();
        $scope.loadRecentOrders();
        setTimeout(refresh, 3000);
    };

    reload();
    setTimeout(refresh, 3000);

//    $scope.updateHistory = function() {
//         $http.get('/api/' + $scope.market + '/history', {params: {period: 5}})
//              .success(function(response, status, headers, config) {
//                    var data = response.data;
//              });
//    };

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

    $scope.updateHistory = function() {
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
                            var load = function() {
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

                                setTimeout(load, $scope.historyUpdateTime);
                            };

                            load();
                        }
                    }
                },
                rangeSelector : {
                    enabled: true,
                    buttons : [
                    {
                        type : 'day',
                        count : 1,
                        text : 'Day'
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
    };

    $scope.updateHistory();

    var updateBidTotal = function() {
        if($scope.bid.price == undefined || $scope.bid.amount == undefined)
            return;
        var total = +(+$scope.bid.price * +$scope.bid.amount).toFixed(6);
//        var available = 0;
//        if ($scope.account[$scope.currency])
//            available = +$scope.account[$scope.currency].available.display;
//        $scope.bid.total = Math.min(total, available);
        $scope.bid.total = total;
    };

    var updateBidAmount = function() {
        if (!$scope.bid.price)
            $scope.bid.amount = 0;
        else
            $scope.bid.amount = COINPORT.floor(+$scope.bid.total / +$scope.bid.price, COINPORT.getAmountFixed($scope.subject));
    };

    var updateAskTotal = function() {
        if($scope.ask.price == undefined || $scope.ask.amount == undefined)
            return;
        var total = +(+$scope.ask.price * +$scope.ask.amount).toFixed(6);
        $scope.ask.total = total;
    };

    var updateAskAmount = function() {
        if (!$scope.ask.price)
            $scope.ask.amount = 0;
        else
            $scope.ask.amount = COINPORT.floor(+$scope.bid.total / +$scope.bid.price, COINPORT.getAmountFixed($scope.subject));
    };

    var toggleBidAdvanced = function(newValue, oldValue) {
        if (!newValue && oldValue) {
            $scope.bidOptions.limitAmount = true;
            $scope.bidOptions.limitPrice = true;
            $scope.bidOptions.limitTotal = true;
            $scope.bidOptions.limitLimit = true;
        }
    }

    var toggleAskAdvanced = function(newValue, oldValue) {
        if (!newValue && oldValue) {
            $scope.askOptions.limitAmount = true;
            $scope.askOptions.limitPrice = true;
            $scope.askOptions.limitTotal = true;
            $scope.bidOptions.limitLimit = true;
        }
    }

    $scope.addBidOrder = function() {
        if($scope.bid.amount <= 0) {
            $scope.alert('bid', Messages.trade.inputAmount);
            return;
        }
        if (!$scope.account[$scope.currency] || $scope.bid.total > ($scope.account[$scope.currency].available.value || 0)) {
            $scope.alert('bid', Messages.trade.noEnough);
            return;
        }
        if (+$scope.bid.price <= 0) {
            $scope.alert('bid', Messages.trade.inputPrice);
            return;
        }
        if (+$scope.bid.total <= 0) {
            $scope.alert('bid', Messages.trade.inputTotal);
            return;
        }

        $scope.info.bidButtonLabel = Messages.trade.submit;
        var payload = {type: 'bid'};
        if (+$scope.bid.price > 0)
            payload.price = +$scope.bid.price;
        if (+$scope.bid.amount > 0)
            payload.amount = +$scope.bid.amount;
        // TODO: market order is not supported
//        if (+$scope.bid.limit > 0)
//            payload.total = +$scope.bid.limit;

        $http.post('/trade/' + $scope.market + '/bid', $.param(payload))
          .success(function(data, status, headers, config) {
            $scope.info.bidButtonLabel = $scope.config.bidButtonLabel;
            if (data.success) {
                var order = data.data;
                $scope.orders.push(order);
                $scope.alert('bid', Messages.trade.submitted);
                setTimeout($scope.loadOrders, 1000);
                setTimeout($scope.updateAccount, 1000);
                // clear amount
                $scope.bid.amount = 0;
                $scope.bid.total = 0;
            } else {
                // handle errors
                $scope.alert('bid', Messages.trade.error);
            }
        }).error(function(data, status, headers, config) {
            if (status == 401) {
                $scope.alert('bid', 'please LOGIN first');
            } else {
                $scope.alert('bid', Messages.trade.error);
            }
            $scope.info.bidButtonLabel = $scope.config.bidButtonLabel;
        });
    };

    $scope.addAskOrder = function() {
        if($scope.ask.amount <= 0) {
            $scope.alert('ask', Messages.trade.inputAmount);
            return;
        }
        if (!$scope.account[$scope.subject] || $scope.ask.amount > ($scope.account[$scope.subject].available.value || 0)) {
            $scope.alert('ask', Messages.trade.noEnough);
            return;
        }
        if ($scope.ask.price <= 0) {
            $scope.alert('ask', Messages.trade.inputPrice);
            return;
        }

        $scope.info.askButtonLabel = Messages.trade.submit;
        var payload = {type: 'ask'};
        if (+$scope.ask.price > 0)
            payload.price = +$scope.ask.price;
        if (+$scope.ask.amount > 0)
            payload.amount = +$scope.ask.amount;
        // TODO: market order is not supported
//        if (+$scope.ask.limit > 0)
//            payload.total = +$scope.ask.limit;

        $http.post('/trade/' + $scope.market + '/ask', $.param(payload))
          .success(function(data, status, headers, config) {
            $scope.info.askButtonLabel = $scope.config.askButtonLabel;
            if (data.success) {
                var order = data.data;
                $scope.orders.push(order);
                $scope.alert('ask', Messages.trade.submitted);
                setTimeout($scope.loadOrders, 1000);
                setTimeout($scope.updateAccount, 1000);
                // clear amount
                $scope.ask.amount = 0;
                $scope.ask.total = 0;
            } else {
                $scope.alert('ask', Messages.trade.error);
            }
        }).error(function(data, status, headers, config) {
            if (status == 401) {
                $scope.alert('ask', 'please LOGIN first');
            } else {
                $scope.alert('ask', Messages.trade.error);
            }
            $scope.info.askButtonLabel = $scope.config.askButtonLabel;
        });
    };

    $scope.clickFunding = function(amount) {
        if (!amount)
            return;
        $scope.bid.total = +amount;
        updateBidAmount();
    }

    $scope.clickQuantity = function(quantity) {
        $scope.ask.amount = +quantity;
    }

    $scope.clickDepthBids = function(index) {
        var data = $scope.depth.bids
        if (index < 0 || index >= data.length) return;

        var amount = 0;
        for(var i = 0; i <= index; i++) {
            amount += data[i].amount.value;
        }
        var price = data[index].price;
        $scope.ask.price = price.display;
        $scope.ask.amount = Math.min(amount, +$scope.account[$scope.subject].available.display);
        updateAskTotal();
    }

    $scope.clickDepthAsks = function(index) {
            var data = $scope.depth.asks
            if (index < 0 || index >= data.length) return;

            var amount = 0;
            for(var i = data.length - 1; i >= index; i--) {
                amount += data[i].amount.value;
            }
            var price = data[index].price;
            $scope.bid.price = price.display;
            $scope.bid.amount = amount;
            updateBidTotal();
    }

    $scope.cancelOrder = function(order) {
        order.status = 100;
        $http.get('/trade/' + $scope.market + '/order/cancel/' + order.id)
            .success(function(data, status, headers, config) {
                if (data.success) {
                    setTimeout($scope.loadOrders, 1000);
                    setTimeout($scope.updateAccount, 1000);
                }
            });
    };

    var watchBidPrice = function(newValue, oldValue) {
        if (!COINPORT.numberRegExp.test(newValue)) {
            $scope.bid.price = oldValue;
            return;
        }

        var fixed = COINPORT.priceFixed[$scope.market.toLowerCase()];
        var value = COINPORT.floor(newValue, fixed);

        $scope.bid.price = value;

        updateBidTotal();
    };

    var watchAskPrice = function(newValue, oldValue) {
        if (!COINPORT.numberRegExp.test(newValue)) {
            $scope.ask.price = oldValue;
            return;
        }

        var fixed = COINPORT.priceFixed[$scope.market.toLowerCase()];
        var value = COINPORT.floor(newValue, fixed);

        $scope.ask.price = value;

        updateAskTotal();
    };

    var watchBidAmount = function(newValue, oldValue) {
        if (!COINPORT.numberRegExp.test(newValue)) {
            $scope.bid.amount = oldValue;
            return;
        }

        var fixed = COINPORT.amountFixed[$scope.subject.toLowerCase()];
        var value = COINPORT.floor(newValue, fixed);

        $scope.bid.amount = value;

        updateBidTotal();
    };

    var watchAskAmount = function(newValue, oldValue) {
        console.log(oldValue, newValue)
        if (!COINPORT.numberRegExp.test(newValue)) {
            $scope.ask.amount = oldValue;
            return;
        }

        var fixed = COINPORT.amountFixed[$scope.subject.toLowerCase()];
        var value = COINPORT.floor(newValue, fixed);

        $scope.ask.amount = value;

        updateAskTotal();
    };

    $scope.$watch('bid.price', watchBidPrice);
    $scope.$watch('bid.amount', watchBidAmount);
    $scope.$watch('ask.price', watchAskPrice);
    $scope.$watch('ask.amount', watchAskAmount);

    $('#bid_total').keyup(updateBidAmount);
    $('#ask_total').keyup(updateAskAmount);
};

// prevent app from memory leak, kind of hack
setTimeout(function() {
    window.location.reload();
}, 1000 * 60 * 10);