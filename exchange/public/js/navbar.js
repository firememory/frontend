var app = angular.module('navbar', []);
app.controller('NaviCtrl', function ($scope, $modal, $log) {
    $scope.openLoginWindow = function (activeTab) {
      $scope.activeTab = activeTab;
      var modalInstance = $modal.open({
        templateUrl: 'register.html',
        controller: function ($scope, $http, $modalInstance) {
          $scope.login = {};
          $scope.register = {};

          $scope.activeTab = $scope.$parent.activeTab;

          $scope.isRegisterActive = ($scope.activeTab == 1);

          $scope.doLogin = function () {
            $http.post('user/login', $scope.login)
              .success(function(data, status, headers, config) {
                if (data.success) {
                  $scope.$parent.username = $scope.login.username;
                  $scope.$parent.isLogin = true;
                  $modalInstance.close();
                } else {
                  $scope.$parent.loginErrorMessage = data.message;
                  $scope.$parent.showLoginError = true;
                }
              });
          };

          $scope.doRegister = function () {
            $http.post('user/register', $scope.register)
              .success(function(data, status, headers, config) {
                console.log(data);
                if (data.success) {
                  $scope.$parent.username = $scope.register.username;
                  $scope.$parent.isLogin = true;
                  $modalInstance.close();
                } else {
                  $scope.$parent.registerErrorMessage = data.message;
                  console.log(data);
                  $scope.$parent.showRegisterError = true;
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