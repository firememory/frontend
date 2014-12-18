(function() {
  var app;

  app = angular.module('coinport.order', ['navbar', 'coinport.app']);

  app.controller('OrderCtrl', function($scope, $http, $window) {
    $scope.oid = $window.location.pathname.replace("/order/", "");
    $http.get('/api/order/' + $scope.oid, {
      params: {}
    }).success(function(data, status, headers, config) {
      return $scope.order = data.data.items[0];
    });
    return $http.get('/api/order/' + $scope.oid + '/transaction', {
      params: {}
    }).success(function(data, status, headers, config) {
      return $scope.transactions = data.data;
    });
  });

}).call(this);
