  var app;

  app = angular.module('coinport.openness', ['ui.bootstrap', 'ngResource', 'navbar', 'ngRoute', 'coinport.app']);

  app.config(function($routeProvider) {
    return $routeProvider.when('/', {
      redirectTo: '/reserve'
    }).when('/reserve/en-US', {
      templateUrl: 'views/openness.en-US.html'
    }).when('/reserve/zh-CN', {
      templateUrl: 'views/openness.zh-CN.html'
    }).when('/opendata', {
      controller: 'DownCtrl',
      templateUrl: 'views/opendata.html'
    }).when('/reserve', {
      controller: 'ReserveCtrl',
      templateUrl: 'views/reserve.html'
    }).when('/opensource', {
      templateUrl: 'views/opensource.html'
    }).when('/connectivity', {
      controller: 'ConnectCtrl',
      templateUrl: 'views/connectivity.html'
    }).otherwise({
      redirectTo: '/'
    });
  });

  app.controller('DownCtrl', function($scope, $http) {
    $scope.messagesPage = 1;
    $scope.snapshotsPage = 1;
    $scope.loadSnapshots = function() {
      return $http.get('/api/open/data/snapshot', {
        params: {
          limit: 10,
          page: $scope.snapshotsPage
        }
      }).success(function(data, status, headers, config) {
        return $scope.snapshots = data.data;
      });
    };
    $scope.loadMessages = function() {
      return $http.get('/api/open/data/messages', {
        params: {
          limit: 10,
          page: $scope.messagesPage
        }
      }).success(function(data, status, headers, config) {
        return $scope.messages = data.data;
      });
    };
    $scope.loadSnapshots();
    return $scope.loadMessages();
  });

  app.controller('ReserveCtrl', function($scope, $http) {
    $scope.hotWallets = {};
    $scope.coldWallets = {};
    $scope.walletsBalance = {};
    $scope.addressUrl = COINPORT.addressUrl;
    $http.get('/api/account/-1').success(function(data, status, headers, config) {
      return $scope.accounts = data.data.accounts;
    });
    return $scope.getWallets = function(currency) {
      $http.get('/api/open/wallet/' + currency + '/hot').success(function(data, status, headers, config) {
        $scope.hotWallets[currency] = data.data;
        return data.data.forEach(function(w) {
          if (!$scope.walletsBalance[w.currency]) {
            $scope.walletsBalance[w.currency] = 0;
          }
          return $scope.walletsBalance[w.currency] += w.amount.value;
        });
      });
      return $http.get('/api/open/wallet/' + currency + '/cold').success(function(data, status, headers, config) {
        $scope.coldWallets[currency] = data.data;
        return data.data.forEach(function(w) {
          if (!$scope.walletsBalance[w.currency]) {
            $scope.walletsBalance[w.currency] = 0;
          }
          return $scope.walletsBalance[w.currency] += w.amount.value;
        });
      });
    };
  });

  app.controller('ConnectCtrl', function($scope, $http) {
    $scope.currencies = {};
    $scope.status = {};
    $scope.timestamp = new Date().getTime();
    $scope.blockUrl = COINPORT.blockUrl;
    $scope.xyz = "abc";
    $scope.getNetworkStatus = function(currency) {
      $scope.currencies[currency] = true;
      return $http.get('/api/open/network/' + currency).success(function(data, status, headers, config) {
        return $scope.status[currency] = data.data;
      });
    };
    $scope.check = function() {
      var currency, _i, _len, _ref, _results;
      $scope.xyz = "xyz";
      console.log($scope.xyz);
     // $scope.timestamp = new Date().getTime();
    };
    var xcheck= $scope.check
    setTimeout($scope.check);
  });


//# sourceMappingURL=open.js.map
