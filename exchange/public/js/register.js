angular.module('coinport.register', ['ui.bootstrap', 'ngResource', 'navbar'])
.config(function httpConfig($httpProvider) {
  $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
})
.controller('RegisterCtrl', function ($scope, $http, $window) {
    $scope.register = {};
    $scope.showEorror = false;

    var showMessage = function(message) {
        $scope.errorMessage = message;
        $scope.showError = true;
    };

    $scope.doRegister = function () {
        console.log('register', $.param($scope.register));
        $http.post('account/register', $.param($scope.register))
          .success(function(data, status, headers, config) {
            if (data.success) {
                $window.location.href = '/login';
            } else {
                showMessage(data.message);
            }
          });
      };
});