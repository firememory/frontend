var app = angular.module('coinport.transactions', ['ui.bootstrap', 'ngResource', 'navbar', 'coinport.app']);

app.controller('TransCtrl', function ($scope, $http, $window) {
    $scope.market = $window.location.pathname.replace("/transactions/", "");
    $scope.page = 1;

    $scope.loadTransactions = function() {
        $http.get('/api/' + $scope.market + '/transaction', {params: {limit: 15, page: $scope.page}})
        .success(function(data, status, headers, config) {
            console.log('transactions', data);
            $scope.transactions = data.data;
        });
    };

    $scope.loadTransactions();
});