var app = angular.module('coinport.user', ['ui.bootstrap', 'ngResource', 'navbar', 'coinport.app']);

app.controller('UserCtrl', function($scope, $http, $window) {
    $scope.dw = {};
    console.log('query account', $window.location);

    $scope.targetUid = $window.location.pathname.replace("/user/", "");

    $http.get('/api/account/' + $scope.targetUid)
        .success(function(data, status, headers, config) {
            $scope.accounts = data.data.accounts
        });

    $scope.transactionPage = 1;
    $scope.transactionLimit = 10;
    $scope.reloadTransactions = function() {
        $http.get('/api/user/' + $scope.targetUid + '/transaction', {params: {limit: $scope.transactionLimit, page: $scope.transactionPage}})
            .success(function(data, status, headers, config) {
            $scope.transactionCount = data.data.count;
            $scope.transactions = data.data.items;
        });
    };

    $scope.reloadTransactions();

    $scope.transferPage = 1;
    $scope.transactionLimit = 10;
    $scope.reloadTransfers = function() {
        $http.get('/api/ALL/transfer/' + $scope.targetUid, {params: {limit: $scope.transactionLimit, page: $scope.transferPage}})
            .success(function(data, status, headers, config) {
                $scope.transfers = data.data.items;
                $scope.transferCount = data.data.count;
            });
    };

    $scope.reloadTransfers();
});

