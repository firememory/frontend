var marketApp = angular.module('coinport.market', ['ui.bootstrap', 'timer', 'coinport.app', 'navbar']);

marketApp.controller('MarketCtrl', function ($scope, $http) {
    $scope.history = [];
    $scope.lastUpdate = new Date().getTime();
    $scope.candleParam = {period: 4};
    $scope.candleChart = null;
    $scope.periods = [
        {period: 13, title: '1周'},
        {period: 12, title: '3日'},
        {period: 11, title: '1日'},
        {class: 'subsep'},
        {period: 10, title: '12小时'},
        {period: 9, title: '6小时'},
        {period: 8, title: '4小时'},
        {period: 7, title: '2小时'},
        {period: 6, title: '1小时'},
        {class: 'subsep'},
        {period: 5, title: '30分钟'},
        {period: 4, title: '15分钟', class: 'period selected'},
        {period: 3, title: '5分钟'},
        {period: 2, title: '3分钟'},
        {period: 1, title: '1分钟'}];
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
        $http.get('api/price')
            .success(function(data, status, headers, config) {
                $scope.price = data.ticker;
            });
        $http.get('api/history', {params: $scope.candleParam})
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
                            upStroke: '#0C0',
                            downStroke: '#C00',
                            downColor: 'rgba(255, 0, 0, 0.5)'
                        }, {
                            type: 'volume',
                            name: 'VOLUME',
                            yAxis: 1,
                            dataOffset: 5,
                            stroke: '#00C',
                            color: 'rgba(0, 0, 255, 0.6)',
                            upStroke: '#0C0',
                            downStroke: '#C00',
                            upColor: 'rgba(0, 255, 0, 0.5)',
                            downColor: 'rgba(255, 0, 0, 0.5)'
                        }]
                    });
                }
                console.log($scope.history);
                $scope.candleChart.setData($scope.history);
            });

        $http.get('api/depth', {params: {depth: 10}})
            .success(function(data, status, headers, config) {
                $scope.depth = data.data;
                $scope.depth.asks.reverse();
            });

        $http.get('api/transaction', {params: {limit: 40, skip: 0}})
            .success(function(data, status, headers, config) {
                $scope.transactions = data.data;
                if ($scope.transactions.length > 0) {
                    $scope.lastTransaction = $scope.transactions[0];
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

marketApp.filter('txTypeClass', function() {
    return function(input) {
        return input ? 'red' : 'green';
    }
});

marketApp.filter('txTypeIcon', function() {
    return function(input) {
        return input ?  'fa-angle-double-right' : 'fa-angle-double-left';
    }
});

marketApp.filter('txTypeText', function() {
    return function(input) {
        return input ?  '卖出' : '买入';
    }
});

marketApp.filter('UID', function() {
    return function(input) {
        var uid = parseInt(input).toString(35).toUpperCase().replace('-','Z');
        var shortUid = uid.substring(0,2) + '***' + uid.substring(uid.length-3,uid.length);
        return shortUid;
    }
});