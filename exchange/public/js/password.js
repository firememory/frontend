var app = angular.module('coinport.password', ['ui.bootstrap', 'ngResource', 'navbar']);

app.config(function httpConfig($httpProvider) {
  $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
});

app.controller('PasswordCtrl', function ($scope, $http, $window) {
  $scope.pwdreset = {};
  $scope.showEorror = false;

  var showMessage = function(message) {
    $scope.errorMessage = message;
    $scope.showError = true;
  };

  $scope.requestPwdReset = function () {
    $window.location.href = '/account/requestpwdreset/' + $scope.pwdreset.email;
  };
});

app.controller('ResetPasswordCtrl', function ($scope, $http, $window) {
  $scope.pwdreset = {};
  $scope.showEorror = false;
  $scope.token = '';

  var showMessage = function(message) {
    $scope.errorMessage = message;
    $scope.showError = true;
  };

  $scope.resetPassword = function () {
    $http.post('/account/dopwdreset', $.param(
      {password: $.sha256b64($scope.pwdreset.password), token: _token}))
      .success(function(data, status, headers, config) {
        if (data.success) {
          console.log("data: ", data);
          var msgKey = 'login.resetPwdSucceeded';
          $window.location.href = '/login?msg=' + msgKey;
        } else {
          showMessage(data.message);
        }
      });
  };
});
