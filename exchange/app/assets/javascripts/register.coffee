app = angular.module('coinport.register', ['ui.bootstrap', 'ngResource', 'navbar'])

app.config ($httpProvider) ->
	$httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded'

app.controller 'RegisterCtrl', ($scope, $http, $window) ->
	$scope.register = {}
	$scope.message = ""
	$scope.captcha = {}

	showMessage = (message) ->
		$scope.message = message

	$scope.newCaptcha = () ->
		$http.get('/captcha', $scope.captcha)
	 	 .success (data, status, headers, config) -> $scope.captcha = data.data

	$scope.newCaptcha()

	$scope.cancel = () ->
		$scope.register = {}

	$scope.doRegister = () ->
		pwdSha256 = $.sha256b64($scope.register.password)
		$http.post('/account/register',
			$.param(
				uuid: $scope.captcha.uuid
				text: $scope.captcha.text
				email: $scope.register.email
				password: pwdSha256
				nationalId: $scope.register.nationalId
				realName: $scope.register.realName))
			.success (data, status, headers, config) ->
				if data.success
					#$scope.$parent.message = Messages.account.registerSucceeded
					#$scope.$parent.message = ""
					$window.location.href = '/prompt/prompt.verifyEmailSent'
				else
					$scope.newCaptcha()
					$scope.$parent.message = data.message


  # $scope.sendVerifySms = function () {
  #   $http.post('/sendVerifySms', $.param({phoneNumber: $scope.register.phoneNumber}))
  #     .success(function(data, status, headers, config) {
  #       if (data.success) {
  #         $scope.register.verifyCodeUuid = data.data.uuid;
  #         console.log('uuid = ' + $scope.register.verifyCodeUuid);
  #         #$window.location.href = '/login';
  #       } else {
  #         showMessage(data.message);
  #       }
  #     });
  # };
