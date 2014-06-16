## Declare app level module which depends on filters, and services
app = angular.module('coinport.currency', ['ui.bootstrap', 'ngResource', 'navbar', 'coinport.app'])

app.controller 'CoinCtrl', ($scope, $http, $window) ->
    $scope.coin = $window.location.pathname.replace("/coin/", "")

    $scope.transferPage = 1
    $scope.reloadTransfers = $http.get('/api/' + $scope.coin + '/transfer/-1', {params: {limit: 15, page: $scope.transferPage}})
        .success (data, status, headers, config) ->
            $scope.transfers = data.data.items
            $scope.transferCount = data.data.count

    $scope.hotWallets = []
    $scope.coldWallets = []
    $scope.addressUrl = COINPORT.addressUrl[$scope.coin]

    $http.get('/api/open/wallet/' + $scope.coin + '/hot')
        .success (data, status, headers, config) ->
            $scope.hotWallets = data.data
            console.log('hotWallets', $scope.hotWallets)
            data.data.forEach (w)->
            $scope.walletsBalance += w.amount.value


    $http.get('/api/open/wallet/' + $scope.coin + '/cold')
        .success (data, status, headers, config) ->
            $scope.coldWallets = data.data;
            console.log('hotWallets', $scope.coldWallets)
            data.data.forEach (w) ->
            $scope.walletsBalance += w.amount.value
