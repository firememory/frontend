var app = angular.module('coinport.index', ['ui.bootstrap', 'ngResource', 'coinport.app', 'navbar', 'timer']);

app.controller('IndexCtrl', function ($scope, $http, $modal) {
  var refresh = function() {
      $http.get('api/ticker')
        .success(function(data, status, headers, config) {
          $scope.tickers = data.data;
        });

      $http.get('api/depth', {params: {depth: 10}})
          .success(function(data, status, headers, config) {
              $scope.depth = data.data;
      });
  };

  refresh();
  $http.get('api/history', {params: {period: 5}})
    .success(function(data, status, headers, config) {
      $scope.history = data.data;
      var chart = $('.candle-chart').jqCandlestick(
      $scope.history, {
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
          upColor: 'rgba(0, 255, 0, 0.4)',
          downStroke: '#C00',
          downColor: 'rgba(255, 0, 0, 0.5)'
        }, {
          type: 'volume',
          name: '成交量',
          dataOffset: 5,
          yAxis: 1,
          stroke: '#00C',
          upStroke: '#0C0',
          upColor: 'rgba(0, 255, 0, 0.4)',
          downStroke: '#C00',
          downColor: 'rgba(255, 0, 0, 0.5)'
        }]
      });
    });

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