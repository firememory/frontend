angular.module('coinport.register', ['ui.bootstrap', 'ngResource', 'navbar'])
.config(function httpConfig($httpProvider) {
  $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
})
.controller('RegisterCtrl', function ($scope, $http, $window) {
  $scope.register = {};
  $scope.showEorror = false;
  $scope.captcha = {};

  var showMessage = function(message) {
    $scope.errorMessage = message;
    $scope.showError = true;
  };

  $scope.newCaptcha = function() {
    $http.get('/captcha', $scope.captcha).success(function(data, status, headers, config) {
      $scope.captcha = data.data;
    });
  };

  $scope.newCaptcha();

  $scope.doRegister = function () {
    var pwdSha256 = $.sha256b64($scope.register.password);
    $http.post('/account/register',
               $.param({uuid: $scope.captcha.uuid,
                        text: $scope.captcha.text,
                        email: $scope.register.email,
                        password: pwdSha256,
                        nationalId: $scope.register.nationalId,
                        realName: $scope.register.realName}))
      .success(function(data, status, headers, config) {
        if (data.success) {
          $scope.$parent.registerErrorMessage = Messages.account.registerSucceeded;
          $scope.$parent.showRegisterError = true;
          $window.location.href = '/prompt/prompt.verifyEmailSent';
        } else {
          $scope.$parent.registerErrorMessage = data.message;
          $scope.$parent.showRegisterError = true;
          $scope.newCaptcha();
        }
      });
  };

  // $scope.sendVerifySms = function () {
  //   $http.post('/sendVerifySms', $.param({phoneNumber: $scope.register.phoneNumber}))
  //     .success(function(data, status, headers, config) {
  //       if (data.success) {
  //         $scope.register.verifyCodeUuid = data.data.uuid;
  //         console.log('uuid = ' + $scope.register.verifyCodeUuid);
  //         //$window.location.href = '/login';
  //       } else {
  //         showMessage(data.message);
  //       }
  //     });
  // };

});
