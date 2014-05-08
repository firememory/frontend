var app = angular.module('navbar', ['ngCookies']);
function httpConfig($httpProvider) {
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
}
app.config(httpConfig);

app.controller('AlertCtrl', function ($scope, $http, $cookieStore) {
  $http.get('/notifications').success(function(data, status, headers, config) {
      var cookieAlerts = $cookieStore.get('alerts') || {};
      $scope.alerts = [];
      data.data.forEach(function(alert) {
        if (!cookieAlerts[alert.type])// TODO: use alert.id instead of alert.type
            $scope.alerts.push(alert);
      });
  });

  $scope.closeAlert = function(index) {
    var alert = $scope.alerts[index];
    $scope.alerts.splice(index, 1);
    var cookieAlerts = $cookieStore.get('alerts') || {};
    cookieAlerts[alert.type] = new Date().getTime(); // TODO: use alert.id instead of alert.type
    $cookieStore.put('alerts', cookieAlerts);
  };
});

app.controller('NaviCtrl', function ($scope, $modal, $log) {
    $scope.openLoginWindow = function (activeTab) {
        $scope.activeTab = activeTab;
        var modalInstance = $modal.open({
            templateUrl: 'views/register.html',
            controller: function ($scope, $http, $modalInstance, $window) {
                $scope.login = {};
                $scope.register = {};
                $scope.captcha = {};
                $scope.activeTab = $scope.$parent.activeTab;

                $scope.isRegisterActive = ($scope.activeTab == 1);

                $scope.newCaptcha = function() {
                    $http.get('/captcha', $scope.captcha).success(function(data, status, headers, config) {
                        console.log(data.data);
                        $scope.captcha = data.data;
                    });
                };

                $scope.newCaptcha();

                $scope.doLogin = function () {
                    $http.post('account/login',
                               $.param({username: $scope.login.username,
                                        password: $scope.login.password}))
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

                $scope.doRegister = function () {
                    $http.post('account/register',
                               $.param({uuid: $scope.captcha.uuid,
                                        text: $scope.captcha.text,
                                        email: $scope.register.email,
                                        password: $scope.register.password,
                                        nationalId: $scope.register.nationalId,
                                        realName: $scope.register.realName}))
                        .success(function(data, status, headers, config) {
                            console.log(data);
                            if (data.success) {
                                $scope.$parent.registerErrorMessage = '注册成功';
                                $scope.$parent.showRegisterError = true;
                                $window.location.href = '/account/registersucceeded';
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
