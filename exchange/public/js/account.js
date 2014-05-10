var app = angular.module('coinport.account', ['ui.bootstrap', 'ngResource', 'ngRoute', 'coinport.app', 'navbar', 'timer']);

function routeConfig($routeProvider) {
    $routeProvider.
    when('/', {
        redirectTo: '/asset'
    }).
    when('/deposit/rmb', {
        controller: 'DepositRmbCtrl',
        templateUrl: 'views/deposit-CNY.html'
    }).
    when('/deposit/:currency', {
        controller: 'DepositCtrl',
        templateUrl: 'views/deposit.html'
    }).
    when('/withdrawal/rmb', {
        controller: 'WithdrawalRmbCtrl',
        templateUrl: 'views/withdrawal-CNY.html'
    }).
    when('/withdrawal/:currency', {
        controller: 'WithdrawalCtrl',
        templateUrl: 'views/withdrawal.html'
    }).
    when('/asset', {
            controller: 'AssetCtrl',
            templateUrl: 'views/asset.html'
    }).
    when('/order', {
            controller: 'OrderDetailCtrl',
            templateUrl: 'views/order.html'
    }).
    when('/orders', {
            controller: 'UserOrderCtrl',
            templateUrl: 'views/orders.html'
    }).
    when('/transaction', {
        controller: 'UserTxCtrl',
        templateUrl: 'views/transactions.html'
    }).
    when('/accountsettings', {
        controller: 'AccountSettingsCtrl',
        templateUrl: 'views/accountSettings.html'
    }).
    when('/test', {
            controller: 'DepositBtcCtrl',
            templateUrl: 'views/test.html'
    }).
    otherwise({
        redirectTo: '/'
    });
}
function httpConfig($httpProvider) {
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
}

app.config(routeConfig);
app.config(httpConfig);

app.controller('DepositRmbCtrl', ['$scope', '$http', function($scope, $http) {
    $http.get('/api/account/' + $scope.uid)
      .success(function(data, status, headers, config) {
        $scope.balance = data.data.accounts['CNY'];
    });

//    $http.get('/api/CNY/transfer/' + $scope.uid, {params: {'type': 0}})
//      .success(function(data, status, headers, config) {
//        $scope.deposits = data.data;
//    });

    $scope.page = 1;
    $scope.loadDeposits = function() {
        $http.get('/api/CNY/transfer' + $scope.uid + {params: {limit: 15, page: $scope.page, 'type':0}})
            .success(function(data, status, headers, config) {
                $scope.deposits = data.data.items;
                $scope.count = data.data.count;
            });
    };
    $scope.loadDeposits();

    $scope.depositData = {currency: 'CNY'};
    $scope.deposit = function() {
        var amount = $scope.amount;
        console.log('deposit ' + $scope.depositData.amount);
        $http.post('/account/deposit', $.param($scope.depositData))
          .success(function(data, status, headers, config) {
            var deposit = data.data.transfer;
            alert(Messages.transfer.depositSuccess + deposit.amount/100 + Messages.transfer.cny);
          });
    };
}]);

app.controller('WithdrawalRmbCtrl', ['$scope', '$http', function($scope, $http) {
    $http.get('/api/account/' + $scope.uid)
        .success(function(data, status, headers, config) {
            $scope.balance = data.data.accounts['CNY'];
        });

//    $http.get('/api/CNY/transfer/' + $scope.uid, {params: {'type': 1}})
//        .success(function(data, status, headers, config) {
//            $scope.withdrawals = data.data;
//        });

    $scope.page = 1;
    $scope.loadWithdrawals = function() {
        $http.get('/api/' + $scope.currency + '/transfer/' + $scope.uid, {params: {limit: 15, page: $scope.page, 'type':1}})
            .success(function(data, status, headers, config) {
                $scope.withdrawals = data.data.items;
                $scope.count = data.data.count;
            });
    };
    $scope.loadWithdrawals();

    $scope.withdrawalData = {currency: 'CNY'};
    $scope.withdrawal = function() {
        var amount = $scope.amount;
        console.log('withdrawal ' , $scope.withdrawalData);
        $http.post('/account/withdrawal', $.param($scope.withdrawalData))
            .success(function(data, status, headers, config) {
                if (data.success) {
                    var withdrawal = data.data.transfer;
                    alert(Messages.transfer.withdrawalSuccess + withdrawal.amount/100 + Messages.cny);
                } else {
                    alert(data.message);
                }
            });
    };
}]);

app.controller('DepositCtrl', ['$scope', '$http', '$routeParams', function($scope, $http, $routeParams) {
    $scope.currency = $routeParams.currency.toUpperCase();
    $http.get('/api/account/' + $scope.uid)
          .success(function(data, status, headers, config) {
            $scope.balance = data.data.accounts[$scope.currency];
    });

    $scope.page = 1;
    $scope.loadDeposits = function() {
        $http.get('/api/' + $scope.currency + '/transfer/' + $scope.uid, {params: {limit: 15, page: $scope.page, 'type':0}})
            .success(function(data, status, headers, config) {
                $scope.deposits = data.data.items;
                $scope.count = data.data.count;
            });
    };
    $scope.loadDeposits();

    $scope.depositData = {currency: $scope.currency};
    $scope.deposit = function() {
        var amount = $scope.amount;
        console.log('deposit ' + $scope.depositData.amount);
        $http.post('/account/deposit', $.param($scope.depositData))
          .success(function(data, status, headers, config) {
            var deposit = data.data.transfer;
            alert(Messages.transfer.depositSuccess + deposit.amount/1000 + $scope.currency);
          });
    };
}]);

app.controller('WithdrawalCtrl', ['$scope', '$http', '$routeParams', function($scope, $http, $routeParams) {
    $scope.currency = $routeParams.currency.toUpperCase();
    $http.get('/api/account/' + $scope.uid)
        .success(function(data, status, headers, config) {
            console.log(data.data.accounts[$scope.currency])
            $scope.balance = data.data.accounts[$scope.currency];
        });

//    $http.get('/api/' + $scope.currency + '/transfer/' + $scope.uid, {params: {'type': 1}})
//        .success(function(data, status, headers, config) {
//            $scope.withdrawals = data.data;
//        });
    $scope.page = 1;
    $scope.loadWithdrawals = function() {
        $http.get('/api/' + $scope.currency + '/transfer/' + $scope.uid, {params: {limit: 15, page: $scope.page, 'type':1}})
            .success(function(data, status, headers, config) {
                $scope.withdrawals = data.data.items;
                $scope.count = data.data.count;
            });
    };
    $scope.loadWithdrawals();


    $scope.withdrawalData = {currency: $scope.currency};
    $scope.withdrawal = function() {
        console.log('withdrawal ' + $scope.withdrawalData.amount);
        $http.post('/account/withdrawal', $.param($scope.withdrawalData))
            .success(function(data, status, headers, config) {
                if (data.success) {
                    var withdrawal = data.data.transfer;
                    alert(Messages.transfer.withdrawalSuccess + withdrawal.amount/1000 + $scope.currency);
                } else {
                    alert(data.message);
                }
            });
    };
}]);

app.controller('AssetCtrl', function ($scope, $http) {
    $http.get('/api/asset/' + $scope.uid)
        .success(function(data, status, headers, config) {
            $scope.assets = data.data;
            var map = $scope.assets[$scope.assets.length-1].amountMap;
            $scope.pieData = [];
            for(asset in map) {
                $scope.pieData.push([asset, map[asset]]);
            }
            console.log($scope.pieData)
            $scope.updateAsset();
        });

    $scope.updateAsset = function() {
        $http.get('/api/account/' + $scope.uid)
            .success(function(data, status, headers, config) {
                $scope.accounts = data.data.accounts;
                var map = $scope.assets[$scope.assets.length-1].amountMap;
                for (currency in $scope.accounts) {
                    var account = $scope.accounts[currency];
                    account.total = account.available.value + account.locked.value + account.pendingWithdrawal.value;
                    account.asset = map[currency];
                }
                console.log($scope.accounts)
            });

        $('#user-finance-chart-pie').jqChart({
            title: { text: Messages.asset.assetComposition },
            legend: {},
            shadows: {
                enabled: true
            },
            series: [
                {
                    type: 'pie',
                    fillStyles: ['#418CF0', '#FCB441', '#E0400A', '#056492', '#BFBFBF', '#1A3B69', '#FFE382'],
                    data: $scope.pieData,
                    labels: {
                        stringFormat: '%.1f%%',
                        valueType: 'percentage',
                        font: '15px sans-serif',
                        fillStyle: 'white'
                    },
                    explodedRadius: 10,
                    explodedSlices: [0],
                    strokeStyle : '#cccccc',
                    lineWidth : 1
                }
            ]
        });

        var assetData = [];
            $scope.assets.forEach(function(row) {
                var asset = row;
                console.log("asset", asset);
                var time = new Date(asset.timestamp);
                var btc = asset.amountMap["BTC"];
                var cny = asset.amountMap["CNY"];

                assetData.push({date: time, value1: cny, value2: btc});
            });


        // jQuery code
        $('#user-finance-chart-history').jqChart({
            title: { text: Messages.asset.assetTrend },
            dataSource: assetData,
            axes: [
                {
                    type: 'dateTime',
                    location: 'bottom',
                    minimum: new Date($scope.assets[0].timestamp),
                    maximum: new Date($scope.assets[$scope.assets.length-1].timestamp),
                    interval: 1,
                    intervalType: 'minutes'
                },
                {
                    name: 'y1',
                    location: 'left'
                }
            ],
            series: [
                {
                    type: 'stackedArea',
                    title: 'CNY',
                    xValuesField: 'date',
                    yValuesField: 'value1',
                    axisY: 'y1'
                },
                {
                    type: 'stackedArea',
                    title: 'BTC',
                    xValuesField: 'date',
                    yValuesField: 'value2',
                    axisY: 'y1'
                }
            ]
        });
    };
});

app.controller('UserTxCtrl', ['$scope', '$http', function($scope, $http) {
//    $scope.update = function(market) {
//        $scope.market = market;
//        $http.get('/api/user/' + $scope.uid + '/transaction/' + $scope.market, {params: {}})
//          .success(function(data, status, headers, config) {
//                $scope.transactions = data.data;
//                console.log("transactions", $scope.transactions)
//        });
//    };

//    $scope.update('BTCCNY');

    $scope.page = 1;
    $scope.loadTransactions = function() {
        $http.get('/api/user/' + $scope.uid + '/transaction/BTCCNY', {params: {limit: 15, page: $scope.page}})
            .success(function(data, status, headers, config) {
                $scope.transactions = data.data.items;
                $scope.count = data.data.count;
            });
    };

    $scope.loadTransactions();

}]);

app.controller('UserOrderCtrl', ['$scope', '$http', '$location', function($scope, $http, $location) {
//    $scope.update = function(market) {
//        $scope.market = market;
//        $http.get('/api/' + $scope.market + '/order')
//            .success(function(data, status, headers, config) {
//                $scope.orders = data.data.items;
//                $scope.count = data.data.count;
//            });
//    };

//    $scope.update('all');

    $scope.showDetail = function(order) {
        $scope.$parent.order = order;
        $location.path('/order');
    };

    $scope.cancelOrder = function(order) {
        var market = order.subject + order.currency;
        $http.get('/trade/' + market + '/order/cancel/' + order.id)
            .success(function(data, status, headers, config) {

            });
    };

    $scope.page = 1;
    $scope.loadOrders = function() {
        $http.get('/api/all/order', {params: {limit: 15, page: $scope.page}})
            .success(function(data, status, headers, config) {
                $scope.orders = data.data.items;
                $scope.count = data.data.count;
            });
    };

    $scope.loadOrders();
}]);

app.controller('OrderDetailCtrl', ['$scope', '$http', function($scope, $http) {
    var order = $scope.order;
    $http.get('/api/order/' + order.id + '/transaction')
        .success(function(data, status, headers, config) {
            $scope.transactions = data.data;
        });
}]);

app.controller('AccountSettingsCtrl', ['$scope', '$http', '$interval', function($scope, $http, $interval) {
    $scope.showUpdateAccountError = false;
    $scope.account = {};
    $scope.credentialItems = [{"name" : "身份证", "value" : "1"},
                              {"name" : "护照", "value" : "2"}];
    $scope.credentialType = $scope.credentialItems[0];
    $scope.verifyButton = Messages.account.getVerifyCodeButtonText;

    var stop;
    $scope.isTiming = false;

    $scope.disableButton = function() {
        if (angular.isDefined(stop)) {
            $scope.isTiming = true;
            return;
        }

        stop = $interval(function() {
            if ($scope.seconds > 0) {
                $scope.seconds = $scope.seconds -1;
                $scope.verifyButton = $scope.seconds + " " + Messages.account.getVerifyCodeButtonText;
                $scope.isTiming = true;
            }
            else {
                $scope.stopTiming();
                $scope.verifyButton = Messages.account.getVerifyCodeButtonText;
                $scope.isTiming = false;
            }
        }, 1000);
    };

    $scope.stopTiming = function() {
        if (angular.isDefined(stop)) {
            $interval.cancel(stop);
            stop = undefined;
        }
        $scope.isTiming = false;
    };

    $scope.$on('destroy', function() {
        $scope.stopTiming();
    });

    $scope.disableButton();

    var sending = false;
    $scope.sendVerifySms = function () {
        $scope.showUpdateAccountError = false;
        $scope.isTiming = true;
        sending = true;
        $http.post('/smsverification', $.param({phoneNumber: $scope.account.mobile}))
            .success(function(data, status, headers, config) {
                if (data.success) {
                    $scope.account.verifyCodeUuid = data.data;
                    console.log('data = ' + data.data);
                    console.log('uuid = ' + $scope.account.verifyCodeUuid);
                    $scope.seconds = 60;
                    $scope.disableButton();
                    sending = false;
                } else {
                    $scope.showUpdateAccountError = true;
                    $scope.updateAccountErrorMessage = data.message;
                    $scope.stopTiming();
                    sending = false;
                }
            });
    };

    $scope.updateAccountSettings = function () {
        $scope.showUpdateAccountError = false;
        $http.post('/account/settings', $.param({ realName: $scope.account.realName,
                                                  mobile: $scope.account.mobile }))
            .success(function(data, status, headers, config) {
                if (data.success) {
                    $scope.showUpdateAccountError = true;
                    $scope.updateAccountErrorMessage = Messages.account.updateAccountProfileSucceeded;
                } else {
                    $scope.showUpdateAccountError = true;
                    $scope.updateAccountErrorMessage = data.message;
                }
            });
    };
}]);
