// Declare app level module which depends on filters, and services
var userApp = angular.module('coinport.user', ['ui.bootstrap', 'ngResource', 'navbar', 'coinport.app']);
userApp.controller('UserCtrl', function ($scope, $http, $window) {
    $scope.dw = {};
    console.log('query account', $window.location)
    $scope.targetUid = $window.location.pathname.replace("/user/", "");

    $http.get('/api/account/' + $scope.targetUid)
    .success(function(data, status, headers, config) {
        $scope.accounts = data.data.accounts;
    });

    $http.get('/api/user/' + $scope.targetUid + '/transaction/BTCRMB', {params: {}})
      .success(function(data, status, headers, config) {
        $scope.transactions = data.data;
    });

    $http.get('/api/RMB/dw/' + $scope.targetUid, {params: {status: 1}})
      .success(function(data, status, headers, config) {
        $scope.dw.CNY = data.data;
        console.log(data.data)
    });

    $http.get('/api/BTC/dw/' + $scope.targetUid, {params: {status: 1}})
      .success(function(data, status, headers, config) {
        $scope.dw.BTC = data.data;
        console.log(data.data)
    });
});