## Declare app level module which depends on filters, and services
app = angular.module('coinport.currency', ['ui.bootstrap', 'ngResource', 'navbar', 'coinport.app'])

app.controller 'CoinCtrl', ($scope, $http, $window) ->
    $scope.coin = $window.location.pathname.replace("/coin/", "")

    $scope.transferPage = 1
    $scope.reloadTransfers = $http.get('/api/' + $scope.coin + '/transfer/-1', {params: {limit: 15, page: $scope.transferPage}})
        .success (data, status, headers, config) ->
            $scope.transfers = data.data.items
            $scope.transferCount = data.data.count


    $http.get('/api/account/-1')
    .success (data, status, headers, config) -> $scope.accounts = data.data.accounts

    $scope.addressUrl = COINPORT.addressUrl[$scope.coin]

    $http.get('/api/open/reserve/' + $scope.coin )
        .success (data, status, headers, config) ->
            $scope.reserveAmount = data.data.amount
            console.log('reserveAmount', $scope.reserveAmount.display)
