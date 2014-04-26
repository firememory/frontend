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

    $http.get('/api/user/' + $scope.targetUid + '/transaction/BTCCNY', {params: {}})
      .success(function(data, status, headers, config) {
        $scope.transactions = data.data;
    });

    $http.get('/api/CNY/transfer/' + $scope.targetUid, {params: {status: 1}})
      .success(function(data, status, headers, config) {
        $scope.dw.CNY = data.data;
    });

    $http.get('/api/BTC/transfer/' + $scope.targetUid, {params: {status: 1}})
      .success(function(data, status, headers, config) {
        $scope.dw.BTC = data.data;
    });
});