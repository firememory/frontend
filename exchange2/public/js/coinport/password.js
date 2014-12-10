(function() {
  var app;

  app = angular.module('coinport.password', ['navbar']);

  app.config(function($httpProvider) {
    return $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
  });

  app.controller('PasswordCtrl', function($scope, $http, $window) {
    var showMessage;
    $scope.pwdreset = {};
    $scope.errorMessage = '';
    showMessage = function(message) {
      return $scope.errorMessage = message;
    };
    return $scope.requestPwdReset = function() {
      return $window.location.href = '/account/requestpwdreset/' + $scope.pwdreset.email;
    };
  });

  app.controller('ResetPasswordCtrl', function($scope, $http, $window) {
    var showMessage;
    $scope.pwdreset = {};
    $scope.errorMessage = '';
    showMessage = function(message) {
      return $scope.errorMessage = message;
    };

    return $scope.resetPassword = function() {
        console.debug("token: ", $scope.token);
      return $http.post('/account/dopwdreset', $.param({
        password: $.sha256b64($scope.pwdreset.password),
        token: $scope.token
      })).success(function(data, status, headers, config) {
        console.debug("data: ", data);
        if (data.success) {
          return $window.location.href = '/login?msg=login.resetPwdSucceeded';
        } else {
          return showMessage(data.message);
        }
      });
    };
  });

}).call(this);
