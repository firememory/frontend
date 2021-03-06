// Declare app level module which depends on filters, and services
var marketApp = angular.module('coinport.user', ['ui.bootstrap', 'ngResource']);

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

// mocked data
var data = [];

for (var i = 1; i < 30; i++) {
    var time = new Date(2014, 0, i);
    var btc = 10000 + 2000 * Math.random();
    var ltc = 3100;
    var pts = 1000;
    var cny = 13411 + 5000 * Math.floor(i / 20);
    var price = 4000 + 200 * Math.random();

    data.push({date: time, value1: cny, value2: btc, value3: ltc, value4: pts, value5: price});
}

// jQuery code
$(document).ready(function () {
    $('#user-finance-chart-history').jqChart({
        title: { text: '资产净值(CNY)' },
        dataSource: data,
        axes: [
            {
                type: 'dateTime',
                location: 'bottom',
                minimum: new Date(2014, 0, 1),
                maximum: new Date(2014, 0, 29),
                interval: 1,
                intervalType: 'days'
            },
            {
                name: 'y1',
                location: 'left'
            }
        ],
        series: [
            {
                type: 'stackedColumn',
                title: '人民币(CNY)',
                xValuesField: 'date',
                yValuesField: 'value1',
                axisY: 'y1'
            },
            {
                type: 'stackedColumn',
                title: '比特币(BTC)',
                xValuesField: 'date',
                yValuesField: 'value2',
                axisY: 'y1'
            },
            {
                type: 'stackedColumn',
                title: '莱特币(LTC)',
                xValuesField: 'date',
                yValuesField: 'value3',
                axisY: 'y1'
            },
            {
                type: 'stackedColumn',
                title: '原型股(PTS)',
                xValuesField: 'date',
                yValuesField: 'value4',
                axisY: 'y1'
            }
        ]
    });

    $('#user-finance-chart-pie').jqChart({
        title: { text: '资产构成' },
        legend: {},
        shadows: {
            enabled: true
        },
        series: [
            {
                type: 'pie',
                fillStyles: ['#418CF0', '#FCB441', '#E0400A', '#056492', '#BFBFBF', '#1A3B69', '#FFE382'],
                data: [['人民币(CNY)', 15498.21], ['比特币(BTC)', 58745.04], ['莱特币(LTC)', 3031.12],
                    ['原型股(PTS)', 6064.73]],
                labels: {
                    stringFormat: '%.1f%%',
                    valueType: 'percentage',
                    font: '15px sans-serif',
                    fillStyle: 'white'
                },
                explodedRadius: 10,
                explodedSlices: [0],
                strokeStyle : '#cccccc',
                lineWidth : 1
            }
        ]
    });
});