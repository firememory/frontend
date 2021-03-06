app = angular.module('coinport.openness', ['ui.bootstrap', 'ngResource', 'navbar', 'ngRoute', 'coinport.app'])

app.config ($routeProvider) ->
    $routeProvider.
        when('/', {
            redirectTo: '/reserve'
        }).
        when('/reserve/en-US', {
            templateUrl: 'views/openness.en-US.html'
        }).
        when('/reserve/zh-CN', {
            templateUrl: 'views/openness.zh-CN.html'
        }).
        when('/opendata', {
            controller: 'DownCtrl',
            templateUrl: 'views/opendata.html'
        }).
        when('/reserve', {
            controller: 'ReserveCtrl',
            templateUrl: 'views/reserve.html'
        }).
        when('/opensource', {
            templateUrl: 'views/opensource.html'
        }).
        when('/connectivity', {
            controller: 'ConnectCtrl',
            templateUrl: 'views/connectivity.html'
        }).
        when('/opentransfer/:coin', {
            controller: 'TransferCtrl',
            templateUrl: 'views/opentransfer.html'
        }).
        otherwise({
            redirectTo: '/'
        })

app.controller 'DownCtrl', ($scope, $http) ->
    $scope.messagesPage = 1
    $scope.snapshotsPage = 1

    $scope.loadSnapshots = () ->
        $http.get('/api/open/data/snapshot', {params: {limit: 10, page: $scope.snapshotsPage}})
        .success (data, status, headers, config) -> $scope.snapshots = data.data

    $scope.loadMessages = () ->
        $http.get('/api/open/data/messages', {params: {limit: 10, page: $scope.messagesPage}})
        .success (data, status, headers, config) -> $scope.messages = data.data

    $scope.loadSnapshots()
    $scope.loadMessages()

app.controller 'TransferCtrl', ($scope, $http) ->
    $scope.currency = $routeParams.currency.toUpperCase();
    console.log('$scope.currency', $scope.currency)


app.controller 'ReserveCtrl', ($scope, $http) ->
    $scope.hotWallets = {}
    $scope.coldWallets = {}
    $scope.walletsBalance = {}
    $scope.addressUrl = COINPORT.addressUrl

    $http.get('/api/account/-1')
        .success (data, status, headers, config) -> $scope.accounts = data.data.accounts
    console.log('reserve', $scope.account)

    $scope.getWallets = (currency) ->
        $http.get('/api/open/wallet/' + currency + '/hot')
            .success (data, status, headers, config) ->
                $scope.hotWallets[currency] = data.data
                data.data.forEach (w)->
                    $scope.walletsBalance[w.currency] = 0 if !$scope.walletsBalance[w.currency]
                    $scope.walletsBalance[w.currency] += w.amount.value

        $http.get('/api/open/wallet/' + currency + '/cold')
            .success (data, status, headers, config) ->
                $scope.coldWallets[currency] = data.data;
                data.data.forEach (w) ->
                    $scope.walletsBalance[w.currency] = 0 if (!$scope.walletsBalance[w.currency])
                    $scope.walletsBalance[w.currency] += w.amount.value

app.controller 'ConnectCtrl', ($scope, $http) ->
    $scope.currencies = ['BTC', 'LTC', 'DOG', 'PTS']
    $scope.status = {}

    $scope.getNetworkStatus = (currency) ->
         $http.get '/api/open/network/' + currency
            .success (data, status, headers, config) -> $scope.status[currency] = data.data

    $scope.check = ->
        $scope.timestamp = new Date().getTime()
        $scope.getNetworkStatus(currency) for currency in $scope.currencies

    $scope.check()
    setInterval($scope.check, 5000)