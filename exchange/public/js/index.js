var app = angular.module('coinport.index', ['ui.bootstrap', 'ngResource', 'coinport.app', 'navbar', 'timer']);

app.controller('IndexCtrl', function ($scope, $http, $modal) {
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
            response.data.forEach(function(row) {
                sparkData.push(row[1]);
            });
            $('.sparkline-' + market).sparkline(sparkData, sparkConfig);
//        $scope.history = response.data;

//            var data = response;
//      		// split the data set into ohlc and volume
//      		var ohlc = [],
//      			volume = [],
//      			dataLength = data.length;
//
//      		for (i = 0; i < dataLength; i++) {
//      			ohlc.push([
//      				data[i][0] * 1000, // the date
//      				data[i][1], // open
//      				data[i][3], // high
//      				data[i][4], // low
//      				data[i][2] // close
//      			]);
//
//      			volume.push([
//      				data[i][0] * 1000, // the date
//      				data[i][5] // the volume
//      			])
//      		}
//
//      		// set the allowed units for data grouping
//      		var groupingUnits = [[
//      			'hour',                         // unit name
//      			[1]                             // allowed multiples
//      		], [
//      			'hour',
//      			[1, 2, 3, 4, 6]
//      		]];
//
//      		// create the chart
//      		$('#candle-chart').highcharts('StockChart', {
//
//      		    rangeSelector: {
//                    inputEnabled: false,
//      		        selected: 1
//      		    },
//
//      		    yAxis: [{
//      		        title: {
//                        text: '价格'
//      		        },
//      		        height: '60%',
//      		        lineWidth: 2
//      		    }, {
//      		        title: {
//                        text: '交易量'
//      		        },
//      		        top: '65%',
//      		        height: '35%',
//      		        offset: 0,
//      		        lineWidth: 2
//      		    }],
//      		    series: [{
//      		        type: 'candlestick',
//      		        name: 'BTC',
//      		        data: ohlc,
//      		        dataGrouping: {
//      					units: groupingUnits
//      		        }
//      		    }, {
//      		        type: 'column',
//      		        name: 'Volume',
//      		        data: volume,
//      		        yAxis: 1,
//      		        dataGrouping: {
//      					units: groupingUnits
//      		        }
//      		    }]
//      		});
      });
    };
    var refresh = function() {
      $http.get('/api/ticker')
        .success(function(data, status, headers, config) {
          $scope.tickers = data.data;
          $scope.tickers.forEach(function(ticker){
            updateSparkline(ticker.market);
          });
        });

      $http.get('/api/depth', {params: {depth: 10}})
          .success(function(data, status, headers, config) {
              $scope.depth = data.data;
      });
    };

  refresh();
    // polling
    $scope.$on('timer-tick', function (event, args) {
        console.log('polling');
        refresh();
    });

    $scope.openLoginWindow = function (activeTab) {
          $scope.activeTab = activeTab;
          var modalInstance = $modal.open({
            templateUrl: 'register.html',
            controller: function ($scope, $http, $modalInstance) {
              $scope.login = {};
              $scope.register = {};

              $scope.activeTab = $scope.$parent.activeTab;

              $scope.isRegisterActive = ($scope.activeTab == 1);

              $scope.doLogin = function () {
                $http.post('user/login', $scope.login)
                  .success(function(data, status, headers, config) {
                    if (data.success) {
                      $scope.$parent.username = $scope.login.username;
                      $scope.$parent.isLogin = true;
                      $modalInstance.close();
                    } else {
                      $scope.$parent.loginErrorMessage = data.message;
                      $scope.$parent.showLoginError = true;
                    }
                  });
              };

              $scope.doRegister = function () {
                $http.post('user/register', $scope.register)
                  .success(function(data, status, headers, config) {
                    console.log(data);
                    if (data.success) {
                      $scope.$parent.username = $scope.register.username;
                      $scope.$parent.isLogin = true;
                      $modalInstance.close();
                    } else {
                      $scope.$parent.registerErrorMessage = data.message;
                      console.log(data);
                      $scope.$parent.showRegisterError = true;
                    }
                  });
              };

              $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
              };
            },
            scope: $scope
          });
        };
});
