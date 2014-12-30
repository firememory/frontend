var marketApp = angular.module('coinport.market', ['ui.bootstrap', 'timer', 'ngRoute', 'coinport.app']);

marketApp.controller('MarketCtrl', function ($scope, $http, $location, $window) {
    var resize = function() {
        // var navbar = document.getElementById('navbar');
        var main = document.getElementById('main');
        var header = document.getElementById('header_outer');
        var footer = document.getElementById('footer_outer');
        var _ref;
        var height = (_ref = window.innerHeight) != null ? _ref : document.documentElement.clientHeight;
        main.style.height = height - header.clientHeight - footer.clientHeight - 83 + 'px';
        main.style.display = 'block';
    };

    var setParams = function() {
        var search = $window.location.search;
        var matched = search.match(/[?&]m=([a-zA-Z]+-[a-zA-Z]+)/);
        if (matched && matched.length > 1) {
            $scope.market = matched[1].toUpperCase();
        } else {
            $scope.market = 'BTC-CNY';
        }
        var res = $scope.market.split('-');
        $scope.subject = res[0];
        $scope.currency = res[1];
    };

    resize();

    $(window).resize(function(){
        resize();
        $scope.candleChart.resize();
    });

    setParams();

    var changeUrl = function(event, newUrl) {
        if ($location.path() == '') {
            if (event != null)
                event.preventDefault();
            return;
        }
        setParams();
        $scope.lastTransaction = {};
        $scope.refresh();
    };

    $scope.history = [];
    $scope.lastUpdate = new Date().getTime();
    $scope.candleParam = {period: 6};
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
        {period: 6, title: Messages.timeDemension.h1, class: 'period selected'},
        {class: 'subsep'},
        {period: 5, title: Messages.timeDemension.m30},
        {period: 4, title: Messages.timeDemension.m15},
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
        $http.get('/api/' + $scope.market + '/history', {params: $scope.candleParam})
            .success(function(data, status, headers, config) {
                $scope.history = data.data.candles;

                if ($scope.candleChart == null) {
                    $scope.candleChart = $('#wrapper').jqCandlestick($scope.history, {
                        theme: 'dark',
                        plot: {
                            spacing: 10,
                            padding: {
                                top: 0,
                                left: 15,
                                bottom: 0,
                                right: 15
                            }
                        },
                        yAxis: [{
                            height: 8
                        }, {
                            height: 2
                        }],
                        yAxisDefaults: {
                          color: '#1A1A1A',
                          labels: {
                            color: '#999;'
                          }
                        },
                        info: {
                          color: '#F4B400;'
                        },
                        series: [{
                            type: 'maline',
                            name: 'MA7',
                            span: 7,
                            dataOffset: 4,
                            color: 'rgba(134,172,245,0.7)' //blue
                        }, {
                            type: 'maline',
                            name: 'MA30',
                            span: 30,
                            dataOffset: 4,
                            color: 'rgba(255, 255, 102, 0.6)' // dark yellow
                        }, {
                            type: 'candlestick',
                            names: ['Open', 'High', 'Low', 'Close'],
                            upStroke: 'rgb(25,178,28)',      // green
                            downStroke: 'rgb(251,2,25)',    // red
                            downColor: 'rgba(251,2,25,0.7)' // red with 0.7 alpha
                        }, {
                            type: 'volume',
                            name: 'Volume',
                            yAxis: 1,
                            dataOffset: 5,
                            stroke: 'rgb(134,172,245)', // blue
                            color: 'rgba(134,172,245,0.7)',  // blue with 0.7 alpha
                            upStroke: 'rgba(25,178,28)', //green
                            downStroke: 'rgb(251,2,25)',  //red
                            upColor: 'rgba(25,178,28,0.7)',  //green with 0.7 alpha
                            downColor: 'rgba(251,2,25,0.7)' // red with 0.7 alpha
                        }]
                    });
                }
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
        $scope.refresh();
        $scope.lastUpdate = new Date().getTime();
        $scope.$broadcast('timer-start');
    });

    $scope.$on('$locationChangeStart', changeUrl);

    changeUrl();
});

marketApp.filter('txTypeIcon', function() {
    return function(input) {
        return input ?  'fa-arrow-right' : 'fa-arrow-left';
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
