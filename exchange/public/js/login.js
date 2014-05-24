angular.module('coinport.login', ['ui.bootstrap', 'ngResource', 'navbar'])
.config(function httpConfig($httpProvider) {
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
}).controller('LoginCtrl', function ($scope, $http, $window) {
    $scope.login = {};
    $scope.showEorror = false;

    var showMessage = function(message) {
        $scope.errorMessage = message;
        $scope.showError = true;
    };

    $scope.doLogin = function () {
        var pwdSha256 = $.sha256b64($scope.login.password);
        $scope.login.password = pwdSha256;

        $http.post('account/login', $.param($scope.login))
            .success(function(data, status, headers, config) {
                if (data.success) {
                    $window.location.href = '/trade';
                } else {
                    showMessage(data.message);
                }
            });
    };
});
