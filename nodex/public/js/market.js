// Declare app level module which depends on filters, and services
var marketApp = angular.module('coinport.market', ['ui.bootstrap', 'ngResource', 'navbar']);

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

            var chart = $('.candle-chart').jqCandlestick({
            data: $scope.history,
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
                dataLeftOffset: Math.max(0, $scope.history.length - 61),
                dataRightOffset: $scope.history.length - 1
              },
              series: [{
                type: 'candlestick',
                names: ['开盘','最高', '最低', '收盘'],
                upStroke: '#0C0',
                downStroke: '#C00',
                downColor: 'rgba(255, 0, 0, 0.7)'
              }, {
                type: 'column',
                name: '成交量',
                dataOffset: 5,
                yAxis: 1,
                stroke: '#00C',
                color: 'rgba(0, 0, 255, 0.7)'
              }]
            });
        });
    $http.get('api/depth')
        .success(function(data, status, headers, config) {
            $scope.depth = data;
            $scope.depth.asks.reverse();
        });
    $http.get('api/trade')
        .success(function(data, status, headers, config) {
            $scope.trades = data.reverse();
        });
});