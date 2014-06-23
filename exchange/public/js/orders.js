var app = angular.module('coinport.orders', ['ui.bootstrap', 'ngResource', 'navbar', 'coinport.app']);

app.controller('OrdersCtrl', function ($scope, $http, $window) {
    $scope.market = $window.location.pathname.replace("/orders/", "");
    $scope.marketdisplay = $scope.market.substr(0,3)+'/'+$scope.market.substr(3,3);

    $scope.loadOrders = function() {
        $http.get('/api/' + $scope.market + '/order', {params: {limit: $scope.limit, page: $scope.page}})
            .success(function(data, status, headers, config) {
                $scope.orders = data.data.items;
                $scope.count = data.data.count;
        });
    };

    $scope.loadOrders();
});