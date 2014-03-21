var marketApp = angular.module('coinport.market', []);

marketApp.controller('MarketCtrl', function ($scope, $http) {
    $http.get('api/price')
        .success(function(data, status, headers, config) {
            $scope.price = data.ticker;
        });
    $http.get('api/history')
        .success(function(data, status, headers, config) {
            $scope.history = [];
            data.forEach(function(row) {
                $scope.history.push([row[0]*1000, row[3], row[5], row[6], row[4], row[7]]);
            });

            var chart = $('#wrapper').jqCandlestick( $scope.history, {
                theme: 'dark',
                yAxis: [{
                    height: 8
                }, {
                    height: 2
                }],
                xAxis: {
                    dataLeftOffset: data.length - 90,
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
        });
    $http.get('api/depth')
        .success(function(data, status, headers, config) {
            $scope.depth = data;
        });
    $http.get('api/trade')
        .success(function(data, status, headers, config) {
            $scope.trades = data.reverse();
        });
});