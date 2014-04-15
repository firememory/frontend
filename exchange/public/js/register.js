var app = angular.module('coinport.register', ['ui.bootstrap', 'ngResource', 'navbar']);

app.controller('RegisterCtrl', function ($scope, $http, $window) {
    $scope.register = {};
    $scope.showEorror = false;

    var showMessage = function(message) {
        $scope.errorMessage = message;
        $scope.showError = true;
    };

    $scope.doRegister = function () {
        console.log('register', $scope.register);
        $http.post('account/register', $scope.register)
          .success(function(data, status, headers, config) {
            if (data.success) {
                $window.location.href = '/login';
            } else {
                showMessage(data.message);
            }
          });
      };
});