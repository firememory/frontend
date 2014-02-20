// Declare app level module which depends on filters, and services
var marketApp = angular.module('coinport.market', ['ui.bootstrap', 'ngResource']);

marketApp.controller('MarketCtrl', function ($scope, $http) {
    $http.get('data/price.json')
        .success(function(data, status, headers, config) {
            $scope.price = data.ticker;
        });
    $http.get('data/depth.json')
        .success(function(data, status, headers, config) {
            $scope.depth = data;
            $scope.depth.asks.reverse();
        });
    $http.get('data/trade.json')
        .success(function(data, status, headers, config) {
            $scope.trades = data.reverse();
        });
});

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
    chart = $('.candle-chart').jqCandlestick({
        data: data,
        theme: 'light',
        yAxis: [{
            height: 7
        }, {
            height: 3
        }],
        info: {
            color: '#000', // color for info
            font: null, // font
            spacing: 10, // distance between values
            position: 'left', // 'left', 'right' or 'auto'
            wrap: 'no' // 'auto', 'yes' or 'no'
        },
        cross: {
            color: 'rgba(0, 0, 0, 0.9)', // color of cursor-cross
            strokeWidth: 1.0, // width cursor-cross lines
            text: {
                background: '#cccccc', // background color for text
                font: null, // font for text
                color: '#000' // color for text
            }
        },
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