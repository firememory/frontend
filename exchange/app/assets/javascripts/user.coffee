## Declare app level module which depends on filters, and services

app = angular.module('coinport.user', ['ui.bootstrap', 'ngResource', 'navbar', 'coinport.app'])

app.controller 'UserCtrl', ($scope, $http, $window) ->
    $scope.dw = {}
    console.log('query account', $window.location)

    $scope.targetUid = $window.location.pathname.replace("/user/", "")

    $http.get('/api/account/' + $scope.targetUid)
        .success (data, status, headers, config) -> $scope.accounts = data.data.accounts

    $http.get('/api/user/' + $scope.targetUid + '/transaction', {params: {}})
        .success (data, status, headers, config) -> $scope.transactions = data.data.items

    $http.get('/api/ALL/transfer/' + $scope.targetUid, {params: {}})
        .success (data, status, headers, config) -> $scope.transfers = data.data.items
