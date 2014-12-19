var app = angular.module('coinport.orders', ['ui.bootstrap', 'navbar', 'coinport.app']);

app.controller('OrdersCtrl', function ($scope, $http, $window) {
    $scope.market = $window.location.pathname.replace("/orders/", "");
    $scope.limit = 25;
    $scope.subject = $scope.market.split("-")[0];
    $scope.currency = $scope.market.split("-")[1];

    $scope.loadOrders = function() {
        $http.get('/api/' + $scope.market + '/orders', {params: {limit: $scope.limit, page: $scope.page}})
            .success(function(data, status, headers, config) {
                $scope.orders = data.data;
        });
    };

    $scope.loadOrders();
});