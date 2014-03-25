var marketApp = angular.module('coinport.market', ['timer']);

marketApp.controller('MarketCtrl', function ($scope, $http) {
    $scope.history = [];
    $scope.candleParam = {period: 4};
    $scope.candleChart = null;
    $scope.refresh = function() {
        $http.get('api/price')
            .success(function(data, status, headers, config) {
                $scope.price = data.ticker;
            });
        $http.get('api/history', {params: $scope.candleParam})
            .success(function(data, status, headers, config) {
                $scope.history = data;
                if ($scope.candleChart == null) {
                    $scope.candleChart = $('#wrapper').jqCandlestick( $scope.history, {
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
                $scope.depth = data;
                $scope.depth.asks.reverse();
            });

        $http.get('api/transaction', {params: {limit: 40, skip: 0}})
            .success(function(data, status, headers, config) {
                $scope.transactions = data
            });
    };

    $scope.refresh();

    $scope.$on('timer-tick', function (event, args) {
        console.log('polling', args);
        $scope.refresh();
    });
});