var app = angular.module('navbar', ['ngCookies']);
function httpConfig($httpProvider) {
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
}
app.config(httpConfig);

app.controller('AlertCtrl', function ($scope, $http, $cookieStore) {
  $http.get('/notifications').success(function(data, status, headers, config) {
      var cookieAlerts = $cookieStore.get('alerts') || {};
      $scope.alerts = [];
      if (!data.data)
        return;
      data.data.forEach(function(alert) {
        if (!cookieAlerts[alert.id])
            $scope.alerts.push(alert);
      });
  });

  $scope.closeAlert = function(index) {
    var alert = $scope.alerts[index];
    $scope.alerts.splice(index, 1);
    var cookieAlerts = $cookieStore.get('alerts') || {};
    cookieAlerts[alert.id] = new Date().getTime();
    $cookieStore.put('alerts', cookieAlerts);
  };
});

app.controller('NaviCtrl', function ($scope, $modal, $log) {
  $scope.openLoginWindow = function (activeTab) {
    $scope.activeTab = activeTab;
    var modalInstance = $modal.open({
      templateUrl: '/views/register.html',
      controller: function ($scope, $http, $modalInstance, $window) {
        $scope.login = {};
        $scope.register = {};
        $scope.captcha = {};
        $scope.activeTab = $scope.$parent.activeTab;

        $scope.isRegisterActive = ($scope.activeTab == 1);

        $scope.newCaptcha = function() {
          $http.get('/captcha', $scope.captcha).success(function(data, status, headers, config) {
            $scope.captcha = data.data;
          });
        };

        $scope.newCaptcha();

        $scope.doLogin = function () {
          var pwdSha256 = $.sha256b64($scope.login.password);
          $http.post('/account/login',
                     $.param({username: $scope.login.username,
                              password: pwdSha256}))
            .success(function(data, status, headers, config) {
              if (data.success) {
                var result = data.data;
                $scope.$parent.username = result.email;
                $scope.$parent.uid = result.id;
                $scope.$parent.isLogin = true;
                $modalInstance.close();
              } else {
                $scope.$parent.loginErrorMessage = data.message;
                $scope.$parent.showLoginError = true;
              }
            });
        };

        $scope.forgetPassword = function() {
          $window.location.href = '/account/forgetpassword';
        };

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

        $scope.cancel = function () {
          $modalInstance.dismiss('cancel');
        };
      },
      scope: $scope
    });
  };
});

app.filter('UID', function() {
    return function(input) {
        return parseInt(input).toString(35).toUpperCase().replace('-','Z');
    }
});

app.directive("repeatInput", function() {
  console.log("repeatInput directive invoked.");
  return {
    require: "ngModel",
    link: function(scope, elem, attrs, ctrl) {
      var otherInput = elem.inheritedData("$formController")[attrs.repeatInput];

      ctrl.$parsers.push(function(value) {
        if(value === otherInput.$viewValue) {
          ctrl.$setValidity("repeat", true);
          return value;
        }
        ctrl.$setValidity("repeat", false);
      });

      otherInput.$parsers.push(function(value) {
        ctrl.$setValidity("repeat", value === ctrl.$viewValue);
        return value;
      });
    }
  };
});
