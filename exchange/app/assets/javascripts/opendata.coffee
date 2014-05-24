app = angular.module('coinport.open', ['ui.bootstrap', 'ngResource', 'navbar', 'ngRoute', 'coinport.app'])

app.config ($routeProvider) ->
	$routeProvider.
		when('/', {
			redirectTo: '/about/en-US'
		}).
		when('/about/en-US', {
			templateUrl: 'views/transparency.en-US.html'
		}).
		when('/about/zh-CN', {
			templateUrl: 'views/transparency.zh-CN.html'
		}).
		when('/opendata', {
			controller: 'DownCtrl',
			templateUrl: 'views/opendata.html'
		}).
		when('/reserve', {
			controller: 'ReserveCtrl',
			templateUrl: 'views/reserve.html'
		}).
		when('/opensource', {
			templateUrl: 'views/opensource.html'
		}).
		when('/connectivity', {
			controller: 'ConnectCtrl',
			templateUrl: 'views/connectivity.html'
		}).
		otherwise({
			redirectTo: '/'
		})

app.controller 'DownCtrl', ($scope, $http) ->
	$scope.messagesPage = 1
	$scope.snapshotsPage = 1

	$scope.loadSnapshots = () ->
		$http.get('/api/open/data/snapshot', {params: {limit: 10, page: $scope.snapshotsPage}})
		.success (data, status, headers, config) -> $scope.snapshots = data.data

	$scope.loadMessages = () ->
		$http.get('/api/open/data/messages', {params: {limit: 10, page: $scope.messagesPage}})
		.success (data, status, headers, config) -> $scope.messages = data.data

	$scope.loadSnapshots()
	$scope.loadMessages()

app.controller 'ReserveCtrl', ($scope, $http) ->
	$http.get('/api/account/-1')
		.success (data, status, headers, config) -> $scope.accounts = data.data.accounts

app.controller 'ConnectCtrl', ($scope, $http) ->
	$scope.currencies = ['BTC', 'LTC', 'DOG', 'PTS']
	$scope.status = {}
	$scope.check = () ->
		$scope.timestamp = new Date().getTime()
		$scope.currencies.forEach (currency) ->
			$http.get('/api/open/network/' + currency)
				.success (data, status, headers, config) ->
					$scope.status[currency] = data.data
					console.log($scope.status)
	$scope.check()
	setInterval($scope.check, 5000)

