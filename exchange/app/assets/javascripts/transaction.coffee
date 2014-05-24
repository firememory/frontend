## Declare app level module which depends on filters, and services

app = angular.module('coinport.transaction', ['ui.bootstrap', 'ngResource', 'navbar', 'coinport.app'])

app.controller 'TxCtrl', ($scope, $http, $window) ->
    $scope.tid = $window.location.pathname.replace("/transaction/", "")

    $http.get('/api/BTCCNY/transaction/' + $scope.tid, {params: {}})
      .success (data, status, headers, config) ->
            console.log("transaction", data.data)
            $scope.transaction = data.data.items[0]
            $scope.takeOrder =  $scope.transaction.tOrder
            $scope.makeOrder =  $scope.transaction.mOrder
