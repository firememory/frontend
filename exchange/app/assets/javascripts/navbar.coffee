app = angular.module('navbar', ['ngCookies'])

app.config ($httpProvider) ->
	$httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded'

app.controller 'AlertCtrl', ($scope, $http, $cookieStore) ->
	$http.get('/notifications').success (data, status, headers, config) ->
		cookieAlerts = $cookieStore.get('alerts') || {}
		$scope.alerts = []
		if data.data
			data.data.forEach (alert) ->
				$scope.alerts.push(alert) if !cookieAlerts[alert.id]

	$scope.closeAlert = (index) ->
		alert = $scope.alerts[index]
		$scope.alerts.splice(index, 1)
		cookieAlerts = $cookieStore.get('alerts') || {}
		cookieAlerts[alert.id] = new Date().getTime()
		$cookieStore.put('alerts', cookieAlerts)


app.controller 'NaviCtrl', ($scope, $modal, $log) ->
	$scope.openLoginWindow =  (activeTab) ->
		$scope.activeTab = activeTab
		modalInstance = $modal.open
			templateUrl: '/views/register.html'
			controller: ($scope, $http, $modalInstance, $window) ->
				$scope.login = {}
				$scope.register = {}
				$scope.captcha = {}
				$scope.activeTab = $scope.$parent.activeTab
				$scope.isRegisterActive = ($scope.activeTab == 1)

				$scope.newCaptcha = () ->
				  $http.get('/captcha', $scope.captcha)
					  .success (data, status, headers, config) -> $scope.captcha = data.data

				$scope.newCaptcha()

				$scope.doLogin = () ->
				  pwdSha256 = $.sha256b64($scope.login.password)
				  $http.post('/account/login',
						$.param(
							username: $scope.login.username
							password: pwdSha256)).success (data, status, headers, config) ->
					if data.success
						result = data.data
						$scope.$parent.username = result.email
						$scope.$parent.uid = result.id
						$scope.$parent.isLogin = true
						$modalInstance.close()
					else
						$scope.$parent.loginErrorMessage = data.message
						$scope.$parent.showLoginError = true

				$scope.forgetPassword = () ->
				  $window.location.href = '/account/forgetpassword'

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
							$scope.$parent.registerErrorMessage = Messages.account.registerSucceeded
							$scope.$parent.showRegisterError = true
							$window.location.href = '/prompt/prompt.verifyEmailSent'
						else
							$scope.$parent.registerErrorMessage = data.message
							$scope.$parent.showRegisterError = true
							$scope.newCaptcha()

				$scope.cancel =  () -> $modalInstance.dismiss('cancel')
			scope: $scope



# Is this still deprecated?
app.directive "repeatInput", () ->
	console.log("repeatInput directive invoked.")

	require: "ngModel"
	link: (scope, elem, attrs, ctrl) ->
		otherInput = elem.inheritedData("$formController")[attrs.repeatInput]

		ctrl.$parsers.push (value) ->
			if value == otherInput.$viewValue
				ctrl.$setValidity("repeat", true)
				return value

			ctrl.$setValidity("repeat", false)

		otherInput.$parsers.push (value) ->
			ctrl.$setValidity("repeat", value == ctrl.$viewValue)
			return value



# mouse over a dropdown nav button will automatically trigger the dropdown.
#$(document).ready () ->
#      if $(window).width() > 768
#            $('.navbar .dropdown').on('mouseover', ()->
#                $('.dropdown-toggle', this).trigger('click')
#            ).on('mouseout',() ->
#                $('.dropdown-toggle', this).trigger('click').blur()
#            )
#        else
#            $('.navbar .dropdown').off('mouseover').off('mouseout')
#    toggleNavbarMethod()

#    $(window).resize(toggleNavbarMethod)
