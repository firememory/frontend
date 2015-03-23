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
    $scope.transactionCursor = 2000000000000000;
    $scope.transactionCount = 1;
    $scope.reloadTransactions = function() {
        $http.get('/api/user/' + $scope.targetUid + '/transaction', {params: {limit: $scope.transactionLimit, page: $scope.transactionPage}})
            .success(function(data, status, headers, config) {
                $scope.transactions = data.data.items;
                newCursor = $scope.transactionCursor;
                $scope.transactions.forEach(function(transaction){
                   if (newCursor > transaction.id)
                     newCursor = transaction.id;
                });
                if (newCursor < $scope.transactionCursor) {
                  $scope.transactionCount = $scope.transactionCount + $scope.transactions.length;
                  $scope.transactionCursor = newCursor
                }
        });
    };


    $scope.transferPage = 1;
    $scope.transferLimit = 10;
    $scope.transferCursor = 2000000000000;
    $scope.transferCount = 1;
    $scope.reloadTransfers = function() {
        $http.get('/api/ALL/transfer/' + $scope.targetUid, {params: {limit: $scope.transferLimit, page: $scope.transferPage}})
            .success(function(data, status, headers, config) {
                $scope.transfers = data.data.items;
                newCursor = $scope.transferCursor;
                $scope.transfers.forEach(function(transfer){
                   transfer.txlink =  COINPORT.txUrl[transfer.amount.currency]+transfer.txid;
                   if (newCursor > transfer.id)
                    newCursor = transfer.id;
                });
                if (newCursor < $scope.transferCursor) {
                  $scope.transferCount = $scope.transferCount + $scope.transfers.length
                  $scope.transferCursor = newCursor
                }
                // $scope.transferCount = data.data.count;
            });
    };

    $scope.orderPage = 1;
    $scope.orderLimit = 10;
    $scope.orderCursor = 2000000000000;
    $scope.orderCount = 1;
    $scope.reloadOrders = function() {
        $http.get('/api/user/' + $scope.targetUid + '/order/all' , {params: {limit: $scope.orderLimit, page: $scope.orderPage}})
            .success(function(data, status, headers, config) {
                $scope.orders = data.data.items;
                newCursor = $scope.orderCursor;
                $scope.orders.forEach(function(order){
                   if (newCursor > order.id)
                    newCursor = order.id;
                });
                if (newCursor < $scope.orderCursor) {
                  $scope.orderCount = $scope.orderCount + $scope.orders.length;
                  $scope.orderCursor = newCursor
                }
                // $scope.orderCount = data.data.count;
            });
    };


    $scope.reloadTransactions();
    $scope.reloadTransfers();
    $scope.reloadOrders();
});

