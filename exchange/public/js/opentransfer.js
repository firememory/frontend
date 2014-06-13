var app = angular.module('coinport.opentransfer', ['ui.bootstrap', 'ngRoute', 'coinport.app', 'navbar']);

function routeConfig($routeProvider) {
    $routeProvider.
        when('/', {
            redirectTo: '/transfer'
        }).
        when('/transfer', {
            templateUrl: 'views/transfer.html'
        }).
        otherwise({
            redirectTo: '/'
        });
}

app.config(routeConfig);

app.controller('TestCtrl', function ($scope, $http, $location) {
    console.log('good');
});
