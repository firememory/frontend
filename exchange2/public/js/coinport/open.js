var app = angular.module('coinport.openness', ['ui.bootstrap', 'navbar', 'ngRoute', 'coinport.app']);

function routeConfig($routeProvider) {
    $routeProvider.
    when('/', {
        redirectTo: '/market'
    }).
    when('/market', {
        controller: 'ReserveCtrl',
        templateUrl: 'views/openmarket.html'
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
    otherwise({
        redirectTo: '/'
    });
}

app.config(routeConfig);

app.controller('DownCtrl', function ($scope, $http) {
    $scope.messagesPage = 1;
    $scope.messagesLimit = 30;
    $scope.loadSnapshots = function() {
        $http.get('/api/open/data/snapshot', {params: {limit: $scope.messagesLimit, page: $scope.snapshotsPage}})
        .success(function(data, status, headers, config) {
            $scope.snapshots = data.data;
        });
    };

    $scope.snapshotsPage = 1;
    $scope.snapshotsLimit = 30;
    $scope.loadMessages = function() {
        $http.get('/api/open/data/messages', {params: {limit: $scope.snapshotsLimit, page: $scope.messagesPage}})
        .success(function(data, status, headers, config) {
            $scope.messages = data.data;
        });
    };

    $scope.loadSnapshots();
    $scope.loadMessages();
});

app.controller('MarketCtrl', function ($scope, $http) {
    var refresh = function() {
      $http.get('/api/ticker')
        .success(function(response, status, headers, config) {
          $scope.tickers = response.data;
          //console.log(response)
        });
    };

    refresh();
});

app.controller('ReserveCtrl', function ($scope, $http) {
    $scope.hotWallets = {};
    $scope.coldWallets = {};
    $scope.walletsBalance = {};
    $scope.addressUrl = COINPORT.addressUrl;
    $scope.currencies = {};
    $scope.status = {};
    $scope.blockUrl = COINPORT.blockUrl;

    $http.get('/api/account/-1000')
        .success(function(data, status, headers, config) {
            $scope.accounts = data.data.accounts;
    });

    $scope.getWallets = function(currency) {
        $http.get('/api/open/reserve/' + currency)
            .success(function(data, status, headers, config) {
                $scope.walletsBalance[currency] = data.data.total;
        });
    };

    $scope.check = function() {
        for(currency in $scope.currencies) {
            $scope.getNetworkStatus(currency);
        }
        console.log($scope.status);
    };

    $scope.getNetworkStatus = function(currency) {
        $scope.currencies[currency] = true;
        $http.get('/api/open/network/' + currency)
            .success (function(data, status, headers, config) {
                $scope.status[currency] = data.data;
        });
    };

    $scope.check();

    setInterval($scope.check, 5000);
});