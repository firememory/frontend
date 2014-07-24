angular.module('coinport.login', ['ui.bootstrap', 'ngResource', 'navbar'])
.config(function httpConfig($httpProvider) {
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
}).controller('LoginCtrl', function ($scope, $http, $window) {
    $scope.login = {};
    $scope.showError = false;
    //$scope.msg = '';
    //$scope.showMsg = false;
    $scope.errorMessage = '';
    $scope.ifEmailNotVerified = false;

    $.get("https://ipinfo.io", function (response) {
        console.debug('response: ', response);
        $scope.login.ip = response.ip;
        $scope.login.location = response.city + ' ' + response.country;
    }, "jsonp");

    $scope.doLogin = function () {
        $scope.login.password = $.sha256b64($scope.login.password);
        $scope.ifEmailNotVerified = false;

        $http.post('account/login', $.param($scope.login))
            .success(function(data, status, headers, config) {
                if (data.success) {
                    $window.location.href = '/trade';
                    $scope.showError= false;
                } else {
                    $scope.errorMessage = Messages.getMessage(data.code, data.message);
                    $scope.showError = true;
                    $scope.login.password = '';
                    if (data.code == 1006)
                        $scope.ifEmailNotVerified = true;
                }
            })
            .error(function(data, status, headers, config) {
                $scope.errorMessage = Messages.getMessage(status, 'request timeout.');
                $scope.showError= true;
                $scope.login.password = '';
            });
    };

});
