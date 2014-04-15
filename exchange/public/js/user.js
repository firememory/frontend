// Declare app level module which depends on filters, and services
var userApp = angular.module('coinport.user', ['ui.bootstrap', 'ngResource', 'navbar']);

userApp.controller('UserCtrl', function ($scope, $http, $location) {
    console.log('query account', $location.path())
    $scope.uid = $location.path().substr(1);
    $http.get('api/account/' + $scope.uid)
        .success(function(data, status, headers, config) {
            console.log('got', data);
            $scope.accounts = data.data.accounts;
        });
});