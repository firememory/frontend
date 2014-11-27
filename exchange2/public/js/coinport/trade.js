var tradeApp = angular.module('coinport.trade', []);

function httpConfig($httpProvider) {
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
//    $httpProvider.defaults.xsrfCookieName = 'XSRF-TOKEN';
}

tradeApp.config(httpConfig);

tradeApp.controller('BidAskCtrl', function BidAskCtrl($scope, $http, $window) {

    if (!$window.location.hash) $window.location.hash = '#/' + COINPORT.defaultMarket;
    $scope.market = $window.location.hash.replace('#/', '').toUpperCase();
    $scope.account = {};
    $scope.showMessage = {bid: false, ask: false};
    $scope.info = {
        fundingLocked: 0,
        fundingRemaining: 0,
        quantityLocked: 0,
        quantityRemaining: 0,
        income: 0,
        message: {},
        bidButtonLabel: $scope.config.bidButtonLabel,
        askButtonLabel: $scope.config.askButtonLabel};


    var init = function() {
        $scope.subject = $scope.market.split("-")[0];
        $scope.currency = $scope.market.split("-")[1];
        $scope.coinName = Messages.coinName[$scope.subject];
    }

    var updateNav = function() {
        $("li[id^='nav-']").removeClass('active');
        $('#nav-' + $scope.market).addClass('active');
    };

    $scope.changeMarket = function(market) {
        $scope.market = market;
        init();
        console.debug("changeMarket: ", $scope.market, $scope.subject, $scope.currency, $scope.coinName);

        updateNav();
        // reload();
        // $scope.updateHistory();
    };

    init();
    updateNav();

    $scope.alert = function(operation, message) {
        $scope.showMessage[operation] = true;
        $scope.info.message[operation] = message;
        setTimeout(function() {
            $scope.showMessage[operation] = false;
        }, 3000);
    };

    $scope.updateAccount = function() {
        if ($scope.uid == 0)
            return;
        $http.get('/api/account/' + $scope.uid)
            .success(function(data, status, headers, config) {
                $scope.account = data.data.accounts;
        });
    };

    $scope.addBidOrder = function() {
        if($scope.bid.amount <= 0) {
            $scope.alert('bid', Messages.trade.inputAmount);
            return;
        }
        if (!$scope.account[$scope.currency] || $scope.bid.total > ($scope.account[$scope.currency].available.value || 0)) {
            $scope.alert('bid', Messages.trade.noEnough);
            return;
        }
        if (+$scope.bid.price <= 0) {
            $scope.alert('bid', Messages.trade.inputPrice);
            return;
        }
        if (+$scope.bid.total <= 0) {
            $scope.alert('bid', Messages.trade.inputTotal);
            return;
        }
        if (+$scope.bid.total < 0.0001) {
            $scope.alert('bid', Messages.trade.tooSmall);
            return;
        }

        $scope.info.bidButtonLabel = Messages.trade.submit;
        var payload = {type: 'bid'};
        if (+$scope.bid.price > 0)
            payload.price = +$scope.bid.price;
        if (+$scope.bid.amount > 0)
            payload.amount = +$scope.bid.amount;
        // TODO: market order is not supported
        //        if (+$scope.bid.limit > 0)
        //            payload.total = +$scope.bid.limit;

        $http.post('/trade/' + $scope.market + '/bid', $.param(payload))
          .success(function(data, status, headers, config) {
            $scope.info.bidButtonLabel = $scope.config.bidButtonLabel;
            if (data.success) {
                var order = data.data;
                $scope.orders.push(order);
                $scope.alert('bid', Messages.trade.submitted);
                setTimeout($scope.loadOrders, 1000);
                setTimeout($scope.updateAccount, 1000);
                // clear amount
                $scope.bid.amount = 0;
                $scope.bid.total = 0;
            } else {
                // handle errors
                $scope.alert('bid', Messages.trade.error);
            }
        }).error(function(data, status, headers, config) {
            if (status == 401) {
                $scope.alert('bid', 'please LOGIN first');
            } else {
                $scope.alert('bid', Messages.trade.error);
            }
            $scope.info.bidButtonLabel = $scope.config.bidButtonLabel;
        });
    };

});
