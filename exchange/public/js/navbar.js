var app = angular.module('navbar', []);
app.controller('NaviCtrl', function ($scope, $modal, $log) {
    $scope.openLoginWindow = function (activeTab) {
      $scope.activeTab = activeTab;
      var modalInstance = $modal.open({
        templateUrl: 'views/register.html',
        controller: function ($scope, $http, $modalInstance) {
          $scope.login = {};
          $scope.register = {};

          $scope.activeTab = $scope.$parent.activeTab;

          $scope.isRegisterActive = ($scope.activeTab == 1);

          $scope.doLogin = function () {
            $http.post('user/login', $scope.login)
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
            $http.post('user/register', $scope.register)
              .success(function(data, status, headers, config) {
                console.log(data);
                if (data.success) {
                  $scope.$parent.registerErrorMessage = '注册成功';
                  $scope.$parent.showRegisterError = true;
                } else {
                  $scope.$parent.registerErrorMessage = data.message;
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