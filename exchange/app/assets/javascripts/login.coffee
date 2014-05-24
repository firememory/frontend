app = angular.module('coinport.login', ['ui.bootstrap', 'ngResource', 'navbar'])


class PasswordCtrl
	@$inject: ["$scope", "$http", "$window"]
	constructor: ($scope, $http, $window) ->
		$scope.pwdreset = {}
		$scope.requestPwdReset = () ->
			$window.location.href = '/account/requestpwdreset/' + $scope.pwdreset.email



class ResetPasswordCtrl
	@$inject: ["$scope", "$http", "$window"]
	constructor: ($scope, $http, $window) ->
		$scope.pwdreset = {}
		$scope.errorMessage = ''
		$scope.token = ''


		$scope.resetPassword = () ->
			hash = $.sha256b64($scope.pwdreset.password)
			$http.post('/account/dopwdreset', $.param({password: hash, token: _token}))
				.success (data, status, headers, config) ->
					if data.success
						console.log("data: ", data)
						$window.location.href = '/login?msg=login.resetPwdSucceeded'
					else
						$scope.errorMessage = data.message





#app.config ($httpProvider) ->
#	$httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded'

app.controller 'PasswordCtrl', PasswordCtrl
app.controller 'ResetPasswordCtrl', ResetPasswordCtrl
