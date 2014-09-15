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

    // $.get("http://ipinfo.io", function (response) {
    //     console.debug('response: ', response);
    //     $scope.login.ip = response.ip;
    //     $scope.login.location = response.city + ' ' + response.country;
    // }, "jsonp");


    // $http.jsonp('http://ipinfo.io?callback=JSON_CALLBACK')
    //     .success(function(data, status, headers, config) {
    //         console.debug("data: ", data)
    //     });

    // $http({
    //     method: 'JSONP',
    //     url: 'http://ipinfo.io?callback=JSON_CALLBACK',
    //     headers: {'Access-Control-Allow-Origin': 'http://ipinfo.io'}
    // }).success(function(data, status, headers, config) {
    //     console.debug("data: ", data)
    // });


    $scope.doLogin = function () {
        $scope.login.password = $.sha256b64($scope.login.password);
        $scope.ifEmailNotVerified = false;

        $http.post('account/login', $.param($scope.login))
            .success(function(data, status, headers, config) {
                if (data.success) {
                    $window.location.href = '/trade';
                    $scope.showError= false;
                } else {
                    if (data.code == 9013) {
                        $scope.errorMessage = Messages.getLoginErrorMessage(data.data);
                    } else {
                        $scope.errorMessage = Messages.getMessage(data.code, data.message);
                    }
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
