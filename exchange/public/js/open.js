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
    $http.get('/api/account/-1')
        .success(function(data, status, headers, config) {
            $scope.accounts = data.data.accounts;
    });
});

app.controller('ConnectCtrl', function ($scope, $http) {
    $scope.currencies = ['BTC', 'LTC', 'DOG', 'PTS'];
    $scope.status = {};
    $scope.check = function() {
        $scope.timestamp = new Date().getTime();
        $scope.currencies.forEach(function(currency) {
            $http.get('/api/open/network/' + currency)
                .success(function(data, status, headers, config) {
                    $scope.status[currency] = data.data;
                    console.log($scope.status);
            });
        });
    };
    $scope.check();
    setInterval($scope.check, 5000);
});

app.filter('statusClass', function() {
    return function(input) {
        if (input < 30 * 60 * 1000)
            return 'success';
        else if (input < 60 * 60 * 1000)
            return 'warning';
        else
            return 'danger';
    }
});

app.filter('statusText', function() {
    return function(input) {
        if (input < 30 * 60 * 1000)
            return Messages.connectivity.status.normal;
        else if (input < 60 * 60 * 1000)
            return Messages.connectivity.status.delayed;
        else
            return Messages.connectivity.status.blocked;
    }
});

