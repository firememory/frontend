var app = angular.module('coinport.open', ['ui.bootstrap', 'ngResource', 'navbar', 'ngRoute', 'coinport.app']);

function routeConfig($routeProvider) {
    $routeProvider.
    when('/', {
        redirectTo: '/about/en-US'
    }).
    when('/about/en-US', {
        templateUrl: 'views/transparency.en-US.html'
    }).
    when('/about/zh-CN', {
        templateUrl: 'views/transparency.zh-CN.html'
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
    otherwise({
        redirectTo: '/'
    });
}

app.config(routeConfig);

app.controller('DownCtrl', function ($scope, $http) {
    $scope.messagesPage = 1;
    $scope.snapshotsPage = 1;

    $scope.loadSnapshots = function() {
        $http.get('/api/open/data/snapshot', {params: {limit: 10, page: $scope.snapshotsPage}})
        .success(function(data, status, headers, config) {
            $scope.snapshots = data.data;
        });
    }

    $scope.loadMessages = function() {
        $http.get('/api/open/data/messages', {params: {limit: 10, page: $scope.messagesPage}})
        .success(function(data, status, headers, config) {
            $scope.messages = data.data;
        });
    }

    $scope.loadSnapshots();
    $scope.loadMessages();
});

app.controller('ReserveCtrl', function ($scope, $http) {
    $scope.hotWallets = {};
    $scope.coldWallets = {};
    $scope.walletsBalance = {};

    $http.get('/api/account/-1')
        .success(function(data, status, headers, config) {
            $scope.accounts = data.data.accounts;
    });

    $scope.getWallets = function(currency) {
        $http.get('/api/open/wallet/' + currency + '/hot')
            .success(function(data, status, headers, config) {
                $scope.hotWallets[currency] = data.data;
                data.data.forEach(function(w){
                    if (!$scope.walletsBalance[w.currency])
                        $scope.walletsBalance[w.currency] = 0;
                    $scope.walletsBalance[w.currency] += w.amount.value;
                });
        });
        $http.get('/api/open/wallet/' + currency + '/cold')
            .success(function(data, status, headers, config) {
                $scope.coldWallets[currency] = data.data;
                data.data.forEach(function(w){
                    if (!$scope.walletsBalance[w.currency])
                        $scope.walletsBalance[w.currency] = 0;
                    $scope.walletsBalance[w.currency] += w.amount.value;
                });
        });
    };
});

app.controller('ConnectCtrl', function ($scope, $http) {
    $scope.currencies = {};
    $scope.status = {};
    $scope.timestamp = new Date().getTime();

    $scope.getNetworkStatus = function(currency) {
        $scope.currencies[currency] = true;
        $http.get('/api/open/network/' + currency)
            .success(function(data, status, headers, config) {
                $scope.status[currency] = data.data;
        });
    };

    $scope.check = function() {
        $scope.timestamp = new Date().getTime();
        for (var currency in $scope.currencies) {
            $scope.getNetworkStatus(currency);
        };
    };

    setInterval($scope.check, 5000);
});

