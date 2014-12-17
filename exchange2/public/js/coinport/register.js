var app = angular.module('coinport.register', ['navbar'])

app.config(function httpConfig($httpProvider) {
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
})

app.controller('RegisterCtrl', function ($scope, $http, $window, $timeout) {
    $scope.register = {};
    $scope.showEorror = false;
    $scope.captcha = {};

    var showMessage = function(message) {
        $scope.errorMessage = message;
        $scope.showError = true;

        $timeout(function () {
            console.debug("fade register error.");
            $scope.showError = false;
            $scope.errorMessage = '';
        }, 6000);
    };

    $scope.newCaptcha = function() {
        $http.get('/captcha', $scope.captcha).success(function(data, status, headers, config) {
            $scope.captcha = data.data;
        });
    };

    //$scope.newCaptcha();

    $scope.doRegister = function () {
        $scope.showError = false
        var pwdSha256 = $.sha256b64($scope.register.password);
        $http.post('/account/register',
                   $.param({uuid: $scope.captcha.uuid,
                            text: $scope.captcha.text,
                            email: $scope.register.email,
                            password: pwdSha256,
                            nationalId: $scope.register.nationalId,
                            realName: $scope.register.realName}))
            .success(function(data, status, headers, config) {
                console.log("data:", data);
                if (data.success) {
                    return $window.location.href = '/prompt/prompt.verifyEmailSent';
                } else {
                    //$scope.newCaptcha();
                    $scope.register.password = '';
                    $scope.register.confirmPassword = '';
                    showMessage(Messages.getMessage(data.code, data.message));
                }
            });
    };

});
