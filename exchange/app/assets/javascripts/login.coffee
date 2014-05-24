
app = angular.module('coinport.login', ['ui.bootstrap', 'ngResource', 'navbar'])

app.config ($httpProvider) ->
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded'

app.controller 'LoginCtrl', ($scope, $http, $window) ->
    $scope.login = {}
    $scope.showEorror = false

    showMessage = (message) ->
        $scope.errorMessage = message
        $scope.showError = true

    $scope.doLogin = () ->
        $scope.login.password = $.sha256b64($scope.login.password)
        $http.post('account/login', $.param($scope.login))
            .success (data, status, headers, config) ->
                if data.success
                    $window.location.href = '/trade'
                else
                    showMessage(data.message)
