app = angular.module('coinport.transactions', ['ui.bootstrap', 'ngResource', 'navbar', 'coinport.app'])

app.controller 'TransCtrl', ($scope, $http, $window) ->
		$scope.market = $window.location.pathname.replace("/transactions/", "")
		$scope.page = 1
		$scope.limit = 25

		$scope.reload = () ->
			$http.get('/api/' + $scope.market + '/transaction', {params: {limit: $scope.limit, page: $scope.page}})
				.success (data, status, headers, config) -> $scope.transactions = data.data

		$scope.reload()
