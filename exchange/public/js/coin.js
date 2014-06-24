var app = angular.module('coinport.coin', ['ui.bootstrap', 'ngResource', 'navbar', 'coinport.app']);

app.controller('CoinTransferCtrl', function ($scope, $http, $window) {
    $scope.coin = $window.location.pathname.replace("/coin/", "");
    $scope.addressUrl = COINPORT.addressUrl[$scope.coin];

    $scope.page = 1;
    $scope.limit = 10;
    $scope.reloadTransfers = function () {
        $http.get('/api/' + $scope.coin + '/transfer/-1', {params: {limit: $scope.limit, page: $scope.page}})
            .success(function (data, status, headers, config) {
                $scope.transfers = data.data.items;
                $scope.transfers.forEach(function(item){
                    item.txlink =  COINPORT.txUrl[item.amount.currency]+item.txid;
                });
                $scope.count = data.data.count;
            });
    };

    $scope.reloadTransfers();
});

app.controller('CoinWalletCtrl', function ($scope, $http, $window) {
    $scope.coin = $window.location.pathname.replace("/coin/", "");
    $http.get('/api/account/-1000')
        .success(function (data, status, headers, config) {
        $scope.accounts = data.data.accounts;
        });

    $http.get('/api/open/reserve/' + $scope.coin )
        .success(function(data, status, headers, config) {
            $scope.reserveAmount = data.data.available;
            console.log('reserveAmount', $scope.reserveAmount.display);
        });

    $scope.hotWallets = [];
    $scope.coldWallets = [];

    $http.get('/api/open/wallet/' + $scope.coin + '/hot')
        .success(function(data, status, headers, config) {
            $scope.hotWallets = data.data;
        });

    $http.get('/api/open/wallet/' + $scope.coin + '/cold')
        .success(function(data, status, headers, config) {
            $scope.coldWallets = data.data;
        });
});
