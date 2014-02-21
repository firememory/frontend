var tradeApp = angular.module('coinport.trade', ['ui.bootstrap', 'ngResource']);

tradeApp.controller('TradeCtrl', function ($scope, $http) {
    $scope.bid = {price: 4419, amount: 0, total: 0};
    $scope.ask = {price: 4419, amount: 0, total: 0};

    $http.get('api/account')
        .success(function(data, status, headers, config) {
            $scope.account = data;
        });

    $http.get('api/order')
        .success(function(data, status, headers, config) {
            $scope.orders = data.orders;
        });

    var updateBidTotal = function() {
        if(!$scope.account || !$scope.account.balance || !$scope.bid.price || !$scope.bid.amount)
            return;
        var total = $scope.bid.price * $scope.bid.amount;
        if(total > $scope.account.balance) {
            total = $scope.account.balance;
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
        if(!$scope.account || !$scope.account.balance || !$scope.ask.price || !$scope.ask.amount)
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
        $scope.account.balance = ($scope.account.balance - $scope.bid.total).toFixed(2);
        $scope.bid.total = Math.min($scope.bid.total, $scope.account.balance);
        $scope.orders.push({
            "date": new Date().getTime(),
            "tid": new Date().getTime(),
            "price": $scope.bid.price,
            "amount": $scope.bid.amount,
            "type": "buy",
            "status": 10
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

    $scope.$watch('bid.amount', updateBidTotal);
    $scope.$watch('bid.price', updateBidTotal);
    $scope.$watch('bid.total', updateBidAmount);
    $scope.$watch('ask.amount', updateAskTotal);
    $scope.$watch('ask.price', updateAskTotal);
    $scope.$watch('ask.total', updateAskAmount);
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
        if(input == 0)
            return 'success';
        if(input == 11)
            return 'info';
        if(input == 12)
            return 'warning';
        if(input == 3)
            return 'danger';
        return '';
    }
    return filter;
});

tradeApp.filter('orderStatusText', function() {
    var filter = function(input) {
        if(input == 0)
            return '交易成功';
        if(input == 11)
            return '挂单中';
        if(input == 12)
            return '部分成交';
        if(input == 3)
            return '已撤销';
        if(input == 10)
            return '等待处理';
        return '';
    }
    return filter;
});

// mock market data
var data = [];

var time = new Date('2014-1-1 12:00').valueOf();

var h = Math.floor(Math.random() * 1000) + 4000;
var l = h - Math.floor(Math.random() * 500);
var o = h - Math.floor(Math.random() * (h - l));
var c = h - Math.floor(Math.random() * (h - l));
var v = 0;

for (var i = 0; i < 60; i++) {
    data.push([time, o, h, l, c, v]);
    o = c;
    h = o + Math.floor(Math.random() * 200);
    l = o - Math.floor(Math.random() * 200);
    c = h - Math.floor(Math.random() * (h - l));
    v = Math.floor(Math.random() * 1000);
    time += 30 * 60000; // Add 30 minutes
}

$(function() {
    $('.candle-chart').jqCandlestick({
        data: data,
        theme: 'light',
        yAxis: [{
            height: 7
        }, {
            height: 3
        }],
        series: [{
            type: 'candlestick',
            names: ['开盘','最高', '最低', '收盘'],
            upStroke: '#0C0',
            downStroke: '#C00',
            downColor: 'rgba(255, 0, 0, 0.4)'
        }, {
            type: 'column',
            name: '成交量',
            yAxis: 1,
            dataOffset: 5,
            stroke: '#00C',
            color: 'rgba(0, 0, 255, 0.5)'
        }]
    });
});