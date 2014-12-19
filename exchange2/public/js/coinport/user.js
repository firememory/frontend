var app = angular.module('coinport.user', ['ui.bootstrap', 'navbar', 'coinport.app']);

app.controller('UserCtrl', function($scope, $http, $window) {
    $scope.dw = {};
    console.log('query account', $window.location);
//    $scope.txUrl = COINPORT.txUrl[$scope.coin];

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
                $scope.transactions = data.data.items;
                $scope.transactionCount = data.data.count;
        });
    };


    $scope.transferPage = 1;
    $scope.transactionLimit = 10;
    $scope.reloadTransfers = function() {
        $http.get('/api/ALL/transfer/' + $scope.targetUid, {params: {limit: $scope.transactionLimit, page: $scope.transferPage}})
            .success(function(data, status, headers, config) {
                $scope.transfers = data.data.items;
                $scope.transfers.forEach(function(transfer){
                   transfer.txlink =  COINPORT.txUrl[transfer.amount.currency]+transfer.txid;
                });
                $scope.transferCount = data.data.count;
            });
    };

    $scope.orderPage = 1;
    $scope.orderLimit = 10;
    $scope.reloadOrders = function() {
        $http.get('/api/user/' + $scope.targetUid + '/order/all' , {params: {limit: $scope.orderLimit, page: $scope.orderPage}})
            .success(function(data, status, headers, config) {
                $scope.orders = data.data.items;
                $scope.orderCount = data.data.count;
            });
    };


    $scope.reloadTransactions();
    $scope.reloadTransfers();
    $scope.reloadOrders();
});

