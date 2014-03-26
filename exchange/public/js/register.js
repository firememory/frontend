var app = angular.module('coinport.register', ['ui.bootstrap', 'ngResource', 'navbar']);

app.controller('RegisterCtrl', function ($scope, $http) {
    $scope.doRegister = function () {
        console.log('register', $scope.register);
        $http.post('user/register', $scope.register)
          .success(function(data, status, headers, config) {
            console.log(data);
            if (data.success) {

            } else {
//              $scope.$parent.registerErrorMessage = data.message;
//              $scope.$parent.showRegisterError = true;
            }
          });
      };
});