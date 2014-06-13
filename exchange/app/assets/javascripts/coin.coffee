## Declare app level module which depends on filters, and services
app = angular.module('coinport.currency', ['ui.bootstrap', 'ngResource', 'navbar', 'ngRoute', 'coinport.app'])

app.config ($routeProvider) ->
    $routeProvider.
        when('/', {
            redirectTo: '/cointransferuser'
        }).
        when('/cointransferuser', {
            controller: 'UserCtrl'
            templateUrl: '/views/cointransferuser.html'
        }).
        when('/cointransferaddr', {
            controller: 'AddrCtrl',
            templateUrl: '/views/cointransferaddr.html'
        }).
        when('/coinwallet', {
            controller: 'WalletCtrl',
            templateUrl: '/views/coinwallet.html'
        }).
        otherwise({
          redirectTo: '/cointransferuser'
        })

app.controller 'UserCtrl', ($scope, $http, $window) ->
    $scope.coin = $window.location.pathname.replace("/opentransfer/", "")

    $scope.transferPage = 1
    $scope.reloadTransfers = $http.get('/api/' + $scope.coin + '/transfer/-1', {params: {limit: 15, page: $scope.transferPage}})
        .success (data, status, headers, config) ->
            $scope.transfers = data.data.items
            $scope.transferCount = data.data.count



app.controller 'AddrCtrl', ($scope, $http, $window) ->
    $scope.coin = $window.location.pathname.replace("/opentransfer/", "")

    $scope.transferPage = 1
    $scope.reloadTransfers = $http.get('/api/' + $scope.coin + '/transfer/-1', {params: {limit: 15, page: $scope.transferPage}})
        .success (data, status, headers, config) ->
            $scope.transfers = data.data.items
            $scope.transferCount = data.data.count


app.controller 'WalletCtrl', ($scope, $http, $window) ->
    $scope.coin = $window.location.pathname.replace("/opentransfer/", "")

    $scope.hotWallets = []
    $scope.coldWallets = []
#    $scope.walletsBalance =
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
