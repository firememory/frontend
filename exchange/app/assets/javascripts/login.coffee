


app = angular.module('coinport.login', ['ui.bootstrap', 'ngResource', 'navbar'])

app.config ($httpProvider) ->
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded'

class LoginCtrl
    constructor : (@$scope, $http, $window) ->
        $scope.login = {}
        $scope.msg = ''
        $scope.showMsg = false
        $scope.errorMessage = ''
        $scope.showError = false
        $scope.ifEmailNotVerified = false

        $scope.doLogin =  () =>
            $scope.login.password = $.sha256b64($scope.login.password)
            $scope.ifEmailNotVerified = false
            $http.post('account/login', $.param($scope.login))
                .success (data, status, headers, config) ->
                    if data.success
                        $window.location.href = '/trade'
                        $scope.showError= false
                    else
                        $scope.errorMessage = Messages.getMessage(data.code, data.message)
                        $scope.showError = true
                        $scope.login.password = ''
                        if data.code == 1006
                            $scope.ifEmailNotVerified = true
                .error (data, status, headers, config) ->
                    $scope.errorMessage = Messages.getMessage(status, 'request timeout.')
                    $scope.showError= true
                    $scope.login.password = ''

app.controller 'LoginCtrl', LoginCtrl
