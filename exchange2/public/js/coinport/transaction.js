(function() {
  var app;

  app = angular.module('coinport.transaction', ['navbar', 'coinport.app']);

  app.controller('TxCtrl', function($scope, $http, $window) {
    $scope.tid = $window.location.pathname.replace("/transaction/", "");
    return $http.get('/api/transaction/' + $scope.tid, {
      params: {}
    }).success(function(data, status, headers, config) {
      console.log("transaction", data.data);
      $scope.transaction = data.data;
      $scope.takeOrder = $scope.transaction.tOrder;
      return $scope.makeOrder = $scope.transaction.mOrder;
    });
  });

}).call(this);

