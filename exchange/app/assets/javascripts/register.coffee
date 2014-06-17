app = angular.module('coinport.register', ['ui.bootstrap', 'ngResource', 'navbar'])

app.config ($httpProvider) ->
  $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded'

# app.directive ('ngFocus', () ->
#     FOCUS_CLASS = "ng-focused"
#     {
#         restrict: 'A',
#         require: 'ngModel',
#         link: (scope, element, attrs, ctrl) ->
#             ctrl.$focused = false
#             element.bind('focus', (evt) ->
#                 element.addClass(FOCUS_CLASS)
#                 scope.$apply(() -> ctrl.$focused = true)
#             ).bind('blur', (evt) ->
#                 element.removeClass(FOCUS_CLASS)
#                 scope.$apply(() -> ctrl.$focused = false)
#             )
#     }
# )

app.controller 'RegisterCtrl', ($scope, $http, $window) ->
  $scope.register = {}
  $scope.errorMessage = ""
  $scope.showError = false
  $scope.captcha = {}

  $scope.newCaptcha = () ->
    $http.get('/captcha', $scope.captcha)
      .success (data, status, headers, config) -> $scope.captcha = data.data

  $scope.newCaptcha()

  $scope.cancel = () -> $scope.register = {}

  $scope.doRegister = () ->
    $http.post('/account/register',
      $.param(
        uuid: $scope.captcha.uuid
        text: $scope.captcha.text
        email: $scope.register.email
        password: $.sha256b64($scope.register.password)
        nationalId: $scope.register.nationalId
        realName: $scope.register.realName))

      .success (data, status, headers, config) ->
        if data.success
          console.debug 'success: ', data
          $window.location.href = '/prompt/prompt.verifyEmailSent'
          $scope.showError = false
        else
          console.debug 'failed: ', data
          $scope.newCaptcha()
          $scope.register.password = ''
          $scope.register.confirmPassword = ''
          $scope.errorMessage = Messages.getMessage(data.code, data.message)
          $scope.showError = true

      .error (data, status, headers, config) ->
        console.debug 'failed: ', data, ', status: ', status
        $scope.errorMessage = Messages.getMessage(data.code, data.message)
        console.debug 'errorMessage: ', $scope.errorMessage
        $scope.showError= true
