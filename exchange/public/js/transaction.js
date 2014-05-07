// Declare app level module which depends on filters, and services
var txApp = angular.module('coinport.transaction', ['ui.bootstrap', 'ngResource', 'navbar', 'coinport.app']);

txApp.controller('TxCtrl', function ($scope, $http, $window) {
    $scope.tid = $window.location.pathname.replace("/transaction/", "");
    $http.get('/api/BTCCNY/transaction/' + $scope.tid, {params: {}})
      .success(function(data, status, headers, config) {
        $scope.transaction = data.data[0];
        $scope.takeOrder =  $scope.transaction.tOrder;
        $scope.makeOrder =  $scope.transaction.mOrder;
    });
});