var openApp = angular.module('coinport.open', ['ui.bootstrap', 'ngResource', 'navbar']);

openApp.controller('OpenCtrl', function ($scope, $http) {
    $scope.messagesPage = 1;
    $scope.snapshotsPage = 1;

    $scope.loadSnapshots = function() {
        $http.get('/api/open/data/snapshot', {params: {limit: 10, page: $scope.snapshotsPage}})
        .success(function(data, status, headers, config) {
            $scope.snapshots = data.data;
        });
    }

    $scope.loadMessages = function() {
        $http.get('/api/open/data/messages', {params: {limit: 10, page: $scope.messagesPage}})
        .success(function(data, status, headers, config) {
            $scope.messages = data.data;
        });
    }

    $scope.loadSnapshots();
    $scope.loadMessages();


    $http.get('/api/account/-1')
    .success(function(data, status, headers, config) {
        $scope.accounts = data.data.accounts;
    });
});