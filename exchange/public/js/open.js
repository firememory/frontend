var openApp = angular.module('coinport.open', ['ui.bootstrap', 'ngResource', 'navbar']);

openApp.controller('OpenCtrl', function ($scope, $http) {
    $http.get('/api/account/-1')
    .success(function(data, status, headers, config) {
        $scope.accounts = data.data.accounts;
    });

    $http.get('/api/open/data/dw')
    .success(function(data, status, headers, config) {
        $scope.dw = data.data;
    });

    $http.get('/api/open/data/mu')
    .success(function(data, status, headers, config) {
        $scope.mu = data.data;
    });
});