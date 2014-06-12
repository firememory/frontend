app = angular.module('navbar', ['ngCookies'])
app.controller('NotificationCtrl', function($scope, $http, $cookieStore) {
	$http.get('/notifications')
	    .success(function(result, status, headers, config) {
	        cookieAlerts = $cookieStore.get('alerts') || {};
	        $scope.alerts = [];
	        result.data.forEach(function(alert) {
	        if(!cookieAlerts[alert.id])
	            $scope.alerts.push(alert);
	        });
	    });

	$scope.closeAlert = function(index) {
		alert = $scope.alerts[index];
		$scope.alerts.splice(index, 1);
		cookieAlerts = $cookieStore.get('alerts') || {};
		cookieAlerts[alert.id] = new Date().getTime();
		$cookieStore.put('alerts', cookieAlerts);
    };
});