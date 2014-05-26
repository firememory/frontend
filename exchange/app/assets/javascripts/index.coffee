


app = angular.module('coinport.home', ['ui.bootstrap', 'ngResource', 'navbar'])

app.config ($httpProvider) ->
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded'

class HomeCtrl
    constructor : (@$scope, $http, $window) ->
        $scope.login = {}
        $scope.msg = ''
        $scope.showMsg = false
        $scope.errorMessage = ''
        $scope.showError = false

        $scope.doLogin =  () =>
            $scope.login.password = $.sha256b64($scope.login.password)
            console.info 'data ', $scope.login

            $http.post('account/login', $.param($scope.login))
                .success (data, status, headers, config) ->
                    if data.success
                        $window.location.href = '/trade'
                        $scope.showError= false
                    else
                        $scope.errorMessage = data.message
                        $scope.showError= true
                .error (data, status, headers, config) ->
                    $scope.errorMessage = data.message
                    $scope.showError= true

app.controller 'HomeCtrl', HomeCtrl


$(window).load () ->
    $('.flexslider').flexslider
        animation: "slide"
