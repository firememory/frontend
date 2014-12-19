(function() {
  var app;

  app = angular.module('coinport.transactions', ['ui.bootstrap', 'navbar', 'coinport.app']);

  app.controller('TransCtrl', function($scope, $http, $window) {
    $scope.market = $window.location.pathname.replace("/transactions/", "");
    $scope.page = 1;
    $scope.limit = 25;
    $scope.reload = function() {
      return $http.get('/api/' + $scope.market + '/transaction', {
        params: {
          limit: $scope.limit,
          page: $scope.page
        }
      }).success(function(data, status, headers, config) {
        return $scope.transactions = data.data;
      });
    };
    return $scope.reload();
  });

}).call(this);

