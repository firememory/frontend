var app = angular.module('coinport.transactions', ['ui.bootstrap', 'ngResource', 'navbar', 'coinport.app']);

app.controller('TransCtrl', function ($scope, $http, $window) {
    $scope.market = $window.location.pathname.replace("/transactions/", "");
    $scope.page = 1;
    $scope.limit = 15;

    $scope.reload = function() {
        $http.get('/api/' + $scope.market + '/transaction', {params: {limit: $scope.limit, page: $scope.page}})
        .success(function(data, status, headers, config) {
            $scope.transactions = data.data;
        });
    };

    $scope.reload();
});