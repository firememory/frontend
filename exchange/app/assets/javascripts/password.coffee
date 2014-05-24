app = angular.module('coinport.password', ['ui.bootstrap', 'ngResource', 'navbar'])

app.config ($httpProvider) ->
	$httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded'

app.controller 'PasswordCtrl', ($scope, $http, $window) ->
	$scope.pwdreset = {}
	$scope.errorMessage = ''

	showMessage = (message) -> $scope.errorMessage = message

	$scope.requestPwdReset = () ->
		$window.location.href = '/account/requestpwdreset/' + $scope.pwdreset.email



app.controller 'ResetPasswordCtrl', ($scope, $http, $window) ->
	$scope.pwdreset = {}
	$scope.errorMessage = ''
	$scope.token = ''

	$scope.resetPassword =  () ->
	$http.post('/account/dopwdreset', $.param(
		password: $.sha256b64($scope.pwdreset.password)
		token: _token)
	).success (data, status, headers, config) ->
		if data.success
			console.log("data: ", data)
			$window.location.href = '/login?msg=login.resetPwdSucceeded'
		else
			showMessage(data.message);
