var app = angular.module('coinport.transactions', ['ui.bootstrap', 'navbar', 'coinport.app']);

app.controller('TransCtrl', function($scope, $http, $window) {
  $scope.market = $window.location.pathname.replace("/transactions/", "");
  $scope.page = 1;
  $scope.limit = 25;
  $scope.transactionCursor = 2000000000000000;
  $scope.count = 1;
  $scope.reload = function() {
    $http.get('/api/' + $scope.market + '/transaction', {params: {limit: $scope.limit, page: $scope.page}})
      .success(function(data, status, headers, config) {
        $scope.transactions = data.data;
        newCursor = $scope.transactionCursor;
        $scope.transactions.items.forEach(function(transaction){
         if (newCursor > transaction.id)
           newCursor = transaction.id;
        });
        if (newCursor < $scope.transactionCursor) {
          $scope.count = $scope.count + $scope.transactions.items.length;
          $scope.transactionCursor = newCursor
        }
      });
  };

  $scope.reload();
});

