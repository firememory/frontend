var marketApp = angular.module('coinport.market', []);

marketApp.controller('MarketCtrl', function ($scope, $http) {
    $http.get('api/price')
        .success(function(data, status, headers, config) {
            $scope.price = data.ticker;
        });
    $http.get('api/history', {params: {span: 1}})
        .success(function(data, status, headers, config) {
            $scope.history = data[0];

            var chart = $('#wrapper').jqCandlestick( $scope.history, {
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
        });
    $http.get('api/depth', {params: {depth: 10}})
        .success(function(data, status, headers, config) {
            $scope.depth = data;
            $scope.depth.asks.reverse();
        });
});