var marketApp = angular.module('coinport.market', ['ui.bootstrap', 'timer', 'ngRoute', 'coinport.app', 'navbar']);

marketApp.controller('MarketCtrl', function ($scope, $http, $location) {
    if ($location.path() == '/')
        $location.path('/btccny');
    $scope.market = $location.path().replace('/', '').toUpperCase();
    $scope.subject = $scope.market.substr(0, 3);
    $scope.currency = $scope.market.substr(3);
    $scope.history = [];
    $scope.lastUpdate = new Date().getTime();
    $scope.candleParam = {period: 4};
    $scope.candleChart = null;
    $scope.periods = [
        {period: 13, title: Messages.timeDemension.w1},
        {period: 12, title: Messages.timeDemension.d3},
        {period: 11, title: Messages.timeDemension.d1},
        {class: 'subsep'},
        {period: 10, title: Messages.timeDemension.h12},
        {period: 9, title: Messages.timeDemension.h6},
        {period: 8, title: Messages.timeDemension.h4},
        {period: 7, title: Messages.timeDemension.h2},
        {period: 6, title: Messages.timeDemension.h1},
        {class: 'subsep'},
        {period: 5, title: Messages.timeDemension.m30},
        {period: 4, title: Messages.timeDemension.m15, class: 'period selected'},
        {period: 3, title: Messages.timeDemension.m5},
        {period: 2, title: Messages.timeDemension.m3},
        {period: 1, title: Messages.timeDemension.m1}];
    $scope.setPeriod = function(period) {
        if ($scope.candleParam.period == period)
            return;
        $scope.periods.forEach(function(item) {
            if (item.period == period)
                item.class = 'period selected';
            else if (item.class == 'period selected')
                item.class = 'period';
        });
        $scope.candleParam.period = period;
        $scope.$broadcast('timer-stop');
    };
    $scope.refresh = function() {
        $http.get('/api/price')
            .success(function(data, status, headers, config) {
                $scope.price = data.ticker;
            });
        $http.get('/api/' + $scope.market + '/history', {params: $scope.candleParam})
            .success(function(data, status, headers, config) {
                $scope.history = data.data;
                if ($scope.candleChart == null) {
                    $scope.candleChart = $('#wrapper').jqCandlestick($scope.history, {
                        theme: 'dark',
                        yAxis: [{
                            height: 8
                        }, {
                            height: 2
                        }],
                        xAxis: {
                            dataLeftOffset: 0,
                            minDataLength: 90
                        },
                        yAxisDefaults: {
                          color: '#333',
                          labels: {
                            color: '#AAA'
                          }
                        },
                        info: {
                          color: '#AAA'
                        },
                        series: [{
                            type: 'candlestick',
                            name: 'OHLC',
                            upStroke: 'rgb(15, 157, 88)',      // green
                            downStroke: 'rgb(219, 68, 55)',    // red
                            downColor: 'rgba(219, 68, 55, 0.5)' // red with 0.5 alpha
                        }, {
                            type: 'volume',
                            name: 'VOLUME',
                            yAxis: 1,
                            dataOffset: 5,
                            stroke: 'rgb(66, 133, 244)', // blue
                            color: 'rgba(66, 133, 244, 0.6)',  // blue with 0.6 alpha
                            upStroke: 'rgb(15, 157, 88)',
                            downStroke: 'rgb(219, 68, 55)',
                            upColor: 'rgba(15, 157, 88, 0.5)',
                            downColor: 'rgba(219, 68, 55, 0.5)' // red with 0.5 alpha
                        }]
                    });
                }
                console.log($scope.history);
                $scope.candleChart.setData($scope.history);
            });

        $http.get('/api/' + $scope.market + '/depth', {params: {depth: 10}})
            .success(function(data, status, headers, config) {
                $scope.depth = data.data;
                $scope.depth.asks.reverse();
            });

        $http.get('/api/' + $scope.market + '/transaction', {params: {limit: 40, skip: 0}})
            .success(function(data, status, headers, config) {
                $scope.transactions = data.data;
                if ($scope.transactions.items.length > 0) {
                    $scope.lastTransaction = $scope.transactions.items[0];
                }
            });
    };

    $scope.refresh();

    $scope.$on('timer-stopped', function (event, data){
        console.log('polling', data);
        $scope.refresh();
        $scope.lastUpdate = new Date().getTime();
        $scope.$broadcast('timer-start');
    });
});

marketApp.filter('txTypeIcon', function() {
    return function(input) {
        return input ?  'fa-angle-double-right' : 'fa-angle-double-left';
    }
});

marketApp.filter('txTypeText', function() {
    return function(input) {
        return input ?  Messages.sell : Messages.buy;
    }
});

marketApp.filter('UID', function() {
    return function(input) {
        var uid = parseInt(input).toString(35).toUpperCase().replace('-','Z');
        var shortUid = uid.substring(0,2) + '***' + uid.substring(uid.length-3,uid.length);
        return shortUid;
    }
});
