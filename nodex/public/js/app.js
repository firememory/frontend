// Declare app level module which depends on filters, and services
angular.module('coinport', ['ui.bootstrap', 'coinport.filters', 'coinport.services', 'coinport.directives']).
    config(['$routeProvider', function($routeProvider) {
        $routeProvider.when('/login', {templateUrl: 'login.html', controller: MyCtrl2});
        $routeProvider.when('/register', {templateUrl: 'register.html', controller: MyCtrl2});
        $routeProvider.otherwise({redirectTo: '/login'});
    }]);
