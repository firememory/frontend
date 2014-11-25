var app = angular.module('coinport.home', ['ui.bootstrap', 'ngResource', 'coinport.app', 'navbar', 'timer']);

app.controller('TickerCtrl', function ($scope, $http, $modal) {
  var updateSparkline = function(market) {
      var fromTime = new Date().getTime() - 24 * 3600 * 1000;
      var sparkConfig = {
        height: 15,
        fillColor: '#fff',
        lineColor: '#444',
        disableTooltips: true
      };
      $http.get('/api/' + market + '/history', {params: {period: 6, from: fromTime}})
        .success(function(response, status, headers, config) {
            console.log(response.data)
            var sparkData = [];
            response.data.candles.forEach(function(row) {
                sparkData.push(row[1]);
            });
            $('.sparkline-' + market).sparkline(sparkData, sparkConfig);
      });
    };
    var refresh = function() {
      $http.get('/api/ticker')
        .success(function(response, status, headers, config) {
          $scope.tickers = response.data;
//          $scope.tickers.forEach(function(ticker){
//            updateSparkline(ticker.market);
//          });
        });
    };

    refresh();
    // polling
    $scope.$on('timer-tick', function (event, args) {
        console.log('polling');
        refresh();
    });
});

app.controller('ReserveCtrl', function ($scope, $http, $modal) {
    $scope.hotWallets = {};
    $scope.coldWallets = {};
    $scope.walletsBalance = {};

    $http.get('/api/account/-1000')
        .success(function(data, status, headers, config) {
            $scope.accounts = data.data.accounts
        });

    $scope.getWallets = function(currency) {
        $http.get('/api/open/reserve/' + currency)
            .success(function(data, status, headers, config) {
                $scope.walletsBalance[currency] = data.data.total;
        });
    };
});