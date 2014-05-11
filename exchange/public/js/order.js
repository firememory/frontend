var orderApp = angular.module('coinport.order', ['ui.bootstrap', 'ngResource', 'navbar', 'coinport.app']);

orderApp.controller('OrderCtrl', function ($scope, $http, $window) {
    $scope.oid = $window.location.pathname.replace("/order/", "");
    $http.get('/api/order/' + $scope.oid, {params: {}})
      .success(function(data, status, headers, config) {
      $scope.order = data.data.items[0];
    });

    $http.get('/api/BTCCNY/order/' + $scope.oid + '/transaction', {params: {}})
      .success(function(data, status, headers, config) {
        $scope.transactions = data.data;
    });
});