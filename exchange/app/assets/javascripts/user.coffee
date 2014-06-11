## Declare app level module which depends on filters, and services

app = angular.module('coinport.user', ['ui.bootstrap', 'ngResource', 'navbar', 'coinport.app'])

app.controller 'UserCtrl', ($scope, $http, $window) ->
    $scope.dw = {}
    console.log('query account', $window.location)


    $scope.targetUid = $window.location.pathname.replace("/user/", "")

    $http.get('/api/account/' + $scope.targetUid)
        .success (data, status, headers, config) -> $scope.accounts = data.data.accounts

    $scope.transactionPage = 1
    $scope.reloadTransactions = $http.get('/api/user/' + $scope.targetUid + '/transaction', {params: {limit: 15, page: $scope.transactionPage}})
        .success (data, status, headers, config) ->
          $scope.transactions = data.data.items
          $scope.transactionCount = data.data.count

    $scope.transferPage = 1
    $scope.reloadTransfers = $http.get('/api/ALL/transfer/' + $scope.targetUid, {params: {limit: 15, page: $scope.transferPage}})
        .success (data, status, headers, config) ->
          $scope.transfers = data.data.items
          $scope.transferCount = data.data.count

