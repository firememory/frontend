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
    // $http.post('/account/requestpwdreset', $.param({email: $scope.pwdreset.email}))
    //   .success(function(data, status, headers, config) {
    //     if (data.success) {
    //       // var promptMsg = data.message;
    //       // $window.location.href = '/prompt/' + promptMsg;
    //     } else {
    //       showMessage(data.message);
    //     }
    //   });
  };
});

app.controller('ResetPasswordCtrl', function ($scope, $http, $window) {
  $scope.pwdreset = {};
  $scope.showEorror = false;

  var showMessage = function(message) {
    $scope.errorMessage = message;
    $scope.showError = true;
  };

  $scope.resetPassword = function () {
    $http.post('/account/dopwdreset', $.param({password: $scope.pwdreset.password,
                                               token: $scope.pwdreset.token}))
      .success(function(data, status, headers, config) {
        if (data.success) {
          //var promptMsg = data.message;
          //$window.location.href = '/prompt/' + promptMsg;
        } else {
          showMessage(data.message);
        }
      });
  };
});
