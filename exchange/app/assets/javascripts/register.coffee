app = angular.module('coinport.register', ['ui.bootstrap', 'ngResource', 'navbar'])

app.config ($httpProvider) ->
  $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded'

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
          $scope.errorMessage = data.message
          $scope.showError = true

      .error (data, status, headers, config) ->
        console.debug 'failed: ', data, ', status: ', status
        $scope.errorMessage = data.message
        $scope.showError= true


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
