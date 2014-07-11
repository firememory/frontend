var app = angular.module('coinport.account', ['ui.bootstrap', 'ngResource', 'ngRoute', 'coinport.app', 'navbar', 'timer', ]);

function routeConfig($routeProvider) {
    $routeProvider.
        when('/', {
            redirectTo: '/asset'
        }).
        when('/transfer', {
            controller: 'TransferCtrl',
            templateUrl: 'views/transfer.html'
        }).
        when('/deposit/:currency', {
            controller: 'DepositCtrl',
            templateUrl: 'views/deposit.html'
        }).
        when('/deposit/debug/:currency', {
            controller: 'DepositCtrl',
            templateUrl: 'debug/deposit.html'
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
        when('/accountprofiles', {
            controller: 'AccountProfilesCtrl',
            templateUrl: 'views/accountProfiles.html'
        }).
        when('/googleauth', {
            controller: 'GoogleAuthCtrl',
            templateUrl: 'views/googleAuth.html'
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

app.controller('TransferCtrl', ['$scope', '$http', function ($scope, $http) {
    $scope.addressUrl = COINPORT.addressUrl;

    $http.get('/api/account/' + $scope.uid)
        .success(function (data, status, headers, config) {
            $scope.accounts = data.data.accounts;
        });
    $scope.status = {};
    $scope.depositAddresses = {};
    $scope.getCurrencyDetails = function(currency) {
        // get network status
        $scope.timestamp = new Date().getTime();
//        $http.get('/api/open/network/' + currency)
//            .success(function(data, status, headers, config) {
//                $scope.status[currency] = data.data;
//        });
        // get deposit address
        $http.get('/depoaddr/' + $scope.uid)
            .success(function (data, status, headers, config) {
                $scope.depositAddresses[currency] = data.data[currency];
        });
    };

    $scope.page = 1;
    $scope.limit = 25;
    $scope.transfers = {};
    $scope.loadTransfers = function () {
        $http.get('/api/ALL/transfer/' + $scope.uid, {params: {limit: $scope.limit, page: $scope.page}})
            .success(function (data, status, headers, config) {
                $scope.transfers = data.data.items;
                $scope.transfers.forEach(function(item){
                    item.txlink =  COINPORT.txUrl[item.amount.currency]+item.txid;
                });
                $scope.count = data.data.count;
            });
    };

    $scope.loadTransfers();
}]);

app.controller('DepositRmbCtrl', ['$scope', '$http', function ($scope, $http) {
    $http.get('/api/account/' + $scope.uid)
        .success(function (data, status, headers, config) {
            $scope.balance = data.data.accounts['CNY'];
        });

    $scope.page = 1;
    $scope.loadDeposits = function () {
        $http.get('/api/CNY/transfer' + $scope.uid + {params: {limit: 15, page: $scope.page, 'type': 0}})
            .success(function (data, status, headers, config) {
                $scope.deposits = data.data.items;
                $scope.count = data.data.count;
            });
    };
    $scope.loadDeposits();

    $scope.depositData = {currency: 'CNY'};
    $scope.deposit = function () {
        var amount = $scope.amount;
        console.log('deposit ' + $scope.depositData.amount);
        $http.post('/account/deposit', $.param($scope.depositData))
            .success(function (data, status, headers, config) {
                var deposit = data.data.transfer;
                alert(Messages.transfer.depositSuccess + deposit.amount / 100 + Messages.transfer.cny);
            });
    };
}]);

app.controller('WithdrawalRmbCtrl', ['$scope', '$http', function ($scope, $http) {
    $http.get('/api/account/' + $scope.uid)
        .success(function (data, status, headers, config) {
            $scope.balance = data.data.accounts['CNY'];
        });

    $scope.page = 1;
    $scope.limit = 25;
    $scope.loadWithdrawals = function () {
        $http.get('/api/' + $scope.currency + '/transfer/' + $scope.uid, {params: {limit: $scope.limit, page: $scope.page, 'type': 1}})
            .success(function (data, status, headers, config) {
                $scope.withdrawals = data.data.items;
                $scope.count = data.data.count;
            });
    };
    $scope.loadWithdrawals();

    $scope.withdrawalData = {currency: 'CNY'};
    $scope.withdrawal = function () {
        var amount = $scope.amount;
        console.log('withdrawal ', $scope.withdrawalData);
        $scope.withdrawalData = {};
        $http.post('/account/withdrawal', $.param($scope.withdrawalData))
            .success(function (data, status, headers, config) {
                if (data.success) {
                    var withdrawal = data.data.transfer;
                    alert(Messages.transfer.withdrawalSuccess + withdrawal.amount / 100 + Messages.cny);
                } else {
                    alert(data.message);
                }
            });
    };
}]);

app.controller('DepositCtrl', ['$scope', '$http', '$routeParams', '$location', function ($scope, $http, $routeParams, $location) {
    $scope.currency = $routeParams.currency.toUpperCase();

    $http.get('/api/account/' + $scope.uid)
        .success(function (data, status, headers, config) {
            $scope.balance = data.data.accounts[$scope.currency];
        });

    $http.get('/depoaddr/' +$scope.currency+ '/' + $scope.uid)
        .success(function (data, status, headers, config) {
            $scope.depositAddress = data.data[$scope.currency];
        });

    $scope.page = 1;
    $scope.limit = 25;
    $scope.loadDeposits = function () {
        $http.get('/api/' + $scope.currency + '/transfer/' + $scope.uid, {params: {limit: $scope.limit, page: $scope.page, 'type': 0}})
            .success(function (data, status, headers, config) {
                $scope.deposits = data.data.items;
                $scope.deposits.forEach(function(item){
                    item.txlink =  COINPORT.txUrl[item.amount.currency]+item.txid;
                });

                $scope.count = data.data.count;
            });
    };
    $scope.loadDeposits();

    $scope.depositData = {currency: $scope.currency};
    $scope.deposit = function () {
        var amount = $scope.amount;
        console.log('deposit ' + $scope.depositData.amount);
        $http.post('/account/deposit', $.param($scope.depositData))
            .success(function (data, status, headers, config) {
                var deposit = data.data.transfer;
                alert(Messages.transfer.depositSuccess + deposit.amount / 1000 + $scope.currency);
            });
    };

    $scope.changeCurrency = function() {
        $location.path('/deposit/' + $scope.currency);
    }
}]);

app.controller('WithdrawalCtrl', ['$scope', '$http', '$routeParams', '$location', '$interval', function ($scope, $http, $routeParams, $location, $interval) {
    $scope.currency = $routeParams.currency.toUpperCase();
    $scope.withdrawalData = {};
    $scope.txUrl = COINPORT.txUrl[$scope.currency];
    $scope.addressUrl = COINPORT.addressUrl[$scope.currency];

    $http.get('/api/account/' + $scope.uid)
        .success(function (data, status, headers, config) {
            $scope.balance = data.data.accounts[$scope.currency];
        });

    $scope.page = 1;
    $scope.limit = 25;
    $scope.loadWithdrawals = function () {
        $http.get('/api/' + $scope.currency + '/transfer/' + $scope.uid, {params: {limit: $scope.limit, page: $scope.page, 'type': 1}})
            .success(function (data, status, headers, config) {
                $scope.withdrawals = data.data.items;
                $scope.count = data.data.count;
            });
    };
    $scope.loadWithdrawals();

    $scope.withdrawalData = {currency: $scope.currency};
    $scope.withdrawal = function () {
        if (! $scope.withdrawalData.amount || Number.isNaN(+$scope.withdrawalData.amount) ||+$scope.withdrawalData.amount < 0) {
            alert(Messages.transfer.messages['invalidAmount']);
            return;
        }
        if (!$scope.withdrawalData.address || $scope.withdrawalData.address == '') {
            alert(Messages.transfer.messages['invalidAddress']);
            return;
        }

        $http.post('/account/withdrawal', $.param($scope.withdrawalData))
            .success(function (data, status, headers, config) {
                if (data.success) {
                    var withdrawal = data.data.transfer;
                    alert(Messages.transfer.messages['ok']);
                } else {
                    alert(Messages.ErrorMessages['m' + data.code]);
                }
                setTimeout($scope.loadWithdrawals, 1000);
            });
    };

    $scope.cancelWithdrawal = function (tid) {
        $http.post('/account/cancelWithdrawal/' + $scope.uid + '/' + tid, {})
            .success(function (data, status, headers, config) {
               $scope.loadWithdrawals();
            });
    };

    $scope.changeCurrency = function() {
        $location.path('/withdrawal/' + $scope.currency);
    };

    // email verification code and button timer:
    $scope.showWithdrawalError = false;
    $scope.verifyButtonEmail = Messages.account.getEmailVerificationCode;
    var _stop;
    $scope.isTimingEmail = false;

    $scope.disableButtonEmail = function () {
        if (angular.isDefined(_stop)) {
            $scope.isTimingEmail = true;
            return;
        }

        $scope.secondsEmail = 120;

        _stop = $interval(function () {
            if ($scope.secondsEmail > 0) {
                $scope.secondsEmail = $scope.secondsEmail - 1;
                $scope.verifyButtonEmail = Messages.account.getVerifyCodeButtonTextPrefix + $scope.secondsEmail + Messages.account.getVerifyCodeButtonTextTail;
                $scope.isTimingEmail = true;
            }
            else {
                $scope.stopTimingEmail();
                $scope.verifyButtonEmail = Messages.account.getEmailVerificationCode;
            }
        }, 1000);
    };

    $scope.stopTimingEmail = function () {
        if (angular.isDefined(_stop)) {
            $interval.cancel(_stop);
            _stop = undefined;
        }
        $scope.isTimingEmail = false;
        $scope.secondsEmail = 0;
        $scope.verifyButtonEmail = Messages.account.getEmailVerificationCode;
    };

    $scope.$on('destroy', function () {
        $scope.stopTimingEmail();
    });

    $scope.sendVerifyEmail = function () {
        $scope.showWithdrawalError = false;
        $scope.disableButtonEmail();

        $http.get('/emailverification')
            .success(function (data, status, headers, config) {
                console.log('data in withdrawal: ', data);
                if (data.success) {
                    $scope.withdrawalData.emailuuid = data.data;
                } else {
                    $scope.stopTiming();
                    $scope.showWithdrawalError = true;
                    $scope.withdrawalErrorMessage = Messages.getMessage(data.code, data.message);
                }
            });
    };

    // sms verification code and button timer:
    $scope.showWithdrawalError = false;
    $scope.verifyButtonSms = Messages.account.getVerifyCodeButtonText;
    var _stop;
    $scope.isTimingSms = false;

    $scope.disableButtonSms = function () {
        if (angular.isDefined(_stop)) {
            $scope.isTimingSms = true;
            return;
        }

        $scope.secondsSms = 120;

        _stop = $interval(function () {
            if ($scope.secondsSms > 0) {
                $scope.secondsSms = $scope.secondsSms - 1;
                $scope.verifyButtonSms = Messages.account.getVerifyCodeButtonTextPrefix + $scope.secondsSms + Messages.account.getVerifyCodeButtonTextTail;
                $scope.isTimingSms = true;
            }
            else {
                $scope.stopTimingSms();
                $scope.verifyButtonSms = Messages.account.getVerifyCodeButtonText;
            }
        }, 1000);
    };

    $scope.stopTimingSms = function () {
        if (angular.isDefined(_stop)) {
            $interval.cancel(_stop);
            _stop = undefined;
        }
        $scope.isTimingSms = false;
        $scope.secondsSms = 0;
        $scope.verifyButtonSms = Messages.account.getVerifyCodeButtonText;
    };

    $scope.$on('destroy', function () {
        $scope.stopTimingSms();
    });

    $scope.sendVerifySms = function () {
        $scope.showWithdrawalError = false;
        $scope.disableButtonSms();

        $http.get('/smsverification')
            .success(function (data, status, headers, config) {
                console.log('data in withdrawal: ', data);
                if (data.success) {
                    $scope.withdrawalData.phonecode = data.data;
                } else {
                    $scope.stopTiming();
                    $scope.showWithdrawalError = true;
                    $scope.withdrawalErrorMessage = Messages.getMessage(data.code, data.message);
                }
            });
    };
}]);

app.controller('AssetCtrl', function ($scope, $http) {
    $http.get('/api/asset/' + $scope.uid)
        .success(function (data, status, headers, config) {
            $scope.assets = data.data;
            drawHistoryChart($scope.assets);
            var map = $scope.assets[$scope.assets.length - 1].amountMap;
            $scope.pieData = [];
            var total = 0;
            for (asset in map) {
                total += map[asset].value;
            }

            for (asset in map) {
                $scope.pieData.push({title: asset, value: map[asset].value / total});
            }

            drawPieChart($scope.pieData);
            $scope.updateAsset();
        });

    var drawPieChart = function (data) {
        var DURATION = 800;
        var DELAY = 200;

        var containerEl = document.getElementById('user-finance-chart-pie'),
            width = containerEl.clientWidth,
            height = width * 0.9,
            radius = Math.min(width, height) / 2,
            container = d3.select(containerEl),
            svg = container.select('svg')
                .attr('width', width)
                .attr('height', height);

        var pie = svg.append('g')
            .attr(
                'transform',
                'translate(' + width / 2 + ',' + height / 2 + ')'
            );

        var detailedInfo = svg.append('g')
            .attr('class', 'pieChart--detailedInformation');

        var twoPi = 2 * Math.PI;
        var pieData = d3.layout.pie()
            .value(function (d) {
                return d.value;
            });

        var arc = d3.svg.arc()
            .outerRadius(radius - 20)
            .innerRadius(0);

        var color = d3.scale.linear()
            .domain([0, data.length - 1])
            .range(["#2980b9", "#34495e"]);
//            .range(["#aad", "#556"]);
//            .range(["rgba(15, 157, 88, 0.5)", "rgba(66, 133, 244, 0.5)"]);

        $scope.color = color;

        var pieChartPieces = pie.datum(data)
            .selectAll('path')
            .data(pieData)
            .enter()
            .append('path')
            .style("fill", function (d, i) {
                return color(i);
            })
            .attr('filter', 'url(#pieChartInsetShadow)')
            .attr('d', arc)
            .each(function () {
                this._current = { startAngle: 0, endAngle: 0 };
            })
            .transition()
            .duration(DURATION)
            .attrTween('d', function (d) {
                var interpolate = d3.interpolate(this._current, d);
                this._current = interpolate(0);

                return function (t) {
                    return arc(interpolate(t));
                };
            });
//            .each('end', function handleAnimationEnd(d) {
//                drawDetailedInformation(d.data, this);
//            });

        drawChartCenter();

        function drawChartCenter() {
            var centerContainer = pie.append('g')
                .attr('class', 'pieChart--center');

//            centerContainer.append('circle')
//                .attr('class', 'pieChart--center--outerCircle')
//                .attr('r', 0)
//                .attr('filter', 'url(#pieChartDropShadow)')
//                .transition()
//                .duration(DURATION)
//                .delay(DELAY)
//                .attr('r', radius - 50);

            centerContainer.append('circle')
                .attr('id', 'pieChart-clippy')
                .attr('class', 'pieChart--center--innerCircle')
                .attr('r', 0)
                .transition()
                .delay(DELAY)
                .duration(DURATION)
                .attr('r', radius - 55)
                .attr('fill', '#fff');
        }

        function drawDetailedInformation(data, element) {
            var bBox = element.getBBox(),
                infoWidth = width * 0.3,
                anchor,
                infoContainer,
                position;
            var x = width - infoWidth;
            var y = bBox.height + bBox.y;
            if (( bBox.x + bBox.width / 2 ) > 0) {
                infoContainer = detailedInfo.append('g')
                    .attr('width', infoWidth)
                    .attr(
                        'transform',
                        'translate(' + x + ',' + y + ')'
                    );
                anchor = 'end';
                position = 'right';
            } else {
                infoContainer = detailedInfo.append('g')
                    .attr('width', infoWidth)
                    .attr(
                        'transform',
                        'translate(' + 0 + ',' + y + ')'
                    );
                anchor = 'start';
                position = 'left';
            }

            infoContainer.data([ data.value * 100 ])
                .append('text')
                .text('0 %')
                .attr('class', 'pieChart--detail--percentage')
                .attr('x', ( position === 'left' ? 0 : infoWidth ))
                .attr('y', 20)
                .attr('text-anchor', anchor)
                .transition()
                .duration(DURATION)
                .tween('text', function (d) {
                    var i = d3.interpolateRound(
                        +this.textContent.replace(/\s%/ig, ''),
                        d
                    );

                    return function (t) {
                        this.textContent = i(t) + ' %';
                    };
                });

            infoContainer.append('line')
                .attr('class', 'pieChart--detail--divider')
                .attr('x1', 0)
                .attr('x2', 0)
                .attr('y1', 30)
                .attr('y2', 30)
                .transition()
                .duration(DURATION)
                .attr('x2', infoWidth);

            infoContainer.data([ data.description ])
                .append('foreignObject')
                .attr('width', infoWidth)
                .attr('height', 24)
                .attr('y', 30)
                .append('xhtml:body')
                .attr(
                    'class',
                    'pieChart--detail--textContainer '
                )
                .html(data.title);
        }
    };

    var drawHistoryChart = function (assets) {
        var data = [];
        for (key in assets[0].amountMap) {
            var layer = [];//{name: key, values: []};
            var x = 0;
            assets.forEach(function (d) {
                layer.push({x: d.timestamp, y: d.amountMap[key].value});
            });
            data.push(layer);
        }
        var stack = d3.layout.stack(),
            layers = stack(data),
            yGroupMax = d3.max(layers, function (layer) {
                return d3.max(layer, function (d) {
                    return d.y;
                });
            }),
            yStackMax = d3.max(layers, function (layer) {
                return d3.max(layer, function (d) {
                    return d.y0 + d.y;
                });
            });

        var margin = {top: 10, right: 10, bottom: 30, left: 80},
            width = 600 - margin.left - margin.right,
            height = 250 - margin.top - margin.bottom;

        var x = d3.scale.ordinal()
            .domain(assets.map(function (d) {
                return d.timestamp;
            }))
            .rangeRoundBands([0, width], .08);

        var y = d3.scale.linear()
            .domain([0, yStackMax])
            .range([height, 0]);

        var color = d3.scale.linear()
            .domain([0, data.length - 1])
            .range(["#2980b9", "#34495e"]);
//            .range(["#aad", "#556"]);
//            .range(["rgba(15, 157, 88, 0.5)", "rgba(66, 133, 244, 0.5)"]);

        var xAxis = d3.svg.axis()
            .scale(x)
            .tickSize(6)
            .tickPadding(6)
            .tickFormat(function (d) {
                var date = new Date(d);
                var day = date.getDate();
                if (day % 5 == 1) return (date.getYear() - 100) + '-' + (date.getMonth() + 1) + '-' + day;
                return '';
            })
            .orient("bottom");

        var yAxis = d3.svg.axis()
            .scale(y)
            .orient("left")
            .tickFormat(d3.format(".1s"));

        var svg = d3.select("#user-finance-chart-history")
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom)
            .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        var layer = svg.selectAll(".layer")
            .data(layers)
            .enter().append("g")
            .attr("class", "layer")
            .style("fill", function (d, i) {
                return color(i);
            });

        var rect = layer.selectAll("rect")
            .data(function (d) {
                return d;
            })
            .enter().append("rect")
            .attr("x", function (d) {
                return x(d.x);
            })
            .attr("y", height)
            .attr("width", x.rangeBand())
            .attr("height", 0);

        rect.transition()
            .delay(function (d, i) {
                return i * 10;
            })
            .attr("y", function (d) {
                return y(d.y0 + d.y);
            })
            .attr("height", function (d) {
                return y(d.y0) - y(d.y0 + d.y);
            });

        svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + height + ")")
            .call(xAxis);

        svg.append("g")
            .attr("class", "y axis")
            .call(yAxis)
            .append("text")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", ".71em");

        d3.selectAll("input").on("change", change);

        // animate on start
//        var timeout = setTimeout(function () {
//            d3.select("input[value=\"grouped\"]").property("checked", true).each(change);
//        }, 2000);

        function change() {
//            clearTimeout(timeout);
            if (this.value === "grouped") transitionGrouped();
            else transitionStacked();
        }

        function transitionGrouped() {
//            y.domain([0, yGroupMax]);

            rect.transition()
                .duration(500)
                .delay(function (d, i) {
                    return i * 10;
                })
                .attr("x", function (d, i, j) {
                    return x(d.x) + x.rangeBand() / data.length * j;
                })
                .attr("width", x.rangeBand() / data.length)
                .transition()
                .attr("y", function (d) {
                    return y(d.y);
                })
                .attr("height", function (d) {
                    return height - y(d.y);
                });
        }

        function transitionStacked() {
//            y.domain([0, yStackMax]);

            rect.transition()
                .duration(500)
                .delay(function (d, i) {
                    return i * 10;
                })
                .attr("y", function (d) {
                    return y(d.y0 + d.y);
                })
                .attr("height", function (d) {
                    return y(d.y0) - y(d.y0 + d.y);
                })
                .transition()
                .attr("x", function (d) {
                    return x(d.x);
                })
                .attr("width", x.rangeBand());
        }
    };

    $scope.updateAsset = function () {
        $http.get('/api/account/' + $scope.uid)
            .success(function (response, status, headers, config) {
                $scope.accounts = response.data.accounts;
                var amountMap = $scope.assets[$scope.assets.length - 1].amountMap;

                var priceMap = $scope.assets[$scope.assets.length - 1].priceMap;

                for (currency in $scope.accounts) {
                    var account = $scope.accounts[currency];
                    account.asset = amountMap[currency].display;
                    account.price = priceMap[currency].display;
                }

            });
    };
});

app.controller('UserTxCtrl', ['$scope', '$http', function ($scope, $http) {
    $scope.market = COINPORT.defaultMarket;
    $scope.page = 1;
    $scope.limit = 25;

    $scope.changeMarket = function() {
        $scope.page = 1;
        $scope.reload();
    };

    $scope.reload = function() {
        $http.get('/api/user/' + $scope.uid + '/transaction/' + $scope.market, {params: {limit: $scope.limit, page: $scope.page}})
          .success(function(data, status, headers, config) {
                $scope.transactions = data.data;
        });
    };

    $scope.reload();
}]);

app.controller('UserOrderCtrl', ['$scope', '$http', '$location', function ($scope, $http, $location) {
    $scope.market = 'all';
    $scope.page = 1;
    $scope.limit = 25;

    $scope.changeMarket = function() {
        $scope.page = 1;
        $scope.reload();
    };

    $scope.reload = function() {
        $http.get('/api/user/' + $scope.uid + '/order/' + $scope.market, {params: {limit: $scope.limit, page: $scope.page}})
            .success(function(data, status, headers, config) {
                $scope.orders = data.data;
            });
    };

    $scope.showDetail = function (order) {
        $scope.$parent.order = order;
        $location.path('/order');
    };

    $scope.cancelOrder = function(order) {
        order.status = 100;
        $http.get('/trade/' + order.subject + order.currency + '/order/cancel/' + order.id)
            .success(function(data, status, headers, config) {
                if (data.success) {
                    setTimeout($scope.reload, 1000);
                }
            });
    };

    $scope.reload();
}]);

app.controller('OrderDetailCtrl', ['$scope', '$http', function ($scope, $http) {
    var order = $scope.order;
    $http.get('/api/order/' + order.id + '/transaction')
        .success(function (data, status, headers, config) {
            $scope.transactions = data.data;
        });
}]);

app.controller('AccountProfilesCtrl', ['$scope', '$http', function ($scope, $http) {

}]);

app.controller('AccountSettingsCtrl', function ($scope, $http, $interval, $window, $modal, $timeout) {
    //$scope.showMainDiv = true;

    $scope.showUpdateAccountError = false;
    $scope.account = {};
    // $scope.credentialItems = [
    //     {"name": "身份证", "value": "1"},
    //     {"name": "护照", "value": "2"}
    // ];
    // $scope.credentialType = $scope.credentialItems[0];


    angular.element(document).ready( function () {
        $('form select.bfh-countries, span.bfh-countries, div.bfh-countries').each(function () {
            var $countries;
            $countries = $(this);
            if ($countries.hasClass('bfh-selectbox')) {
                $countries.bfhselectbox($countries.data());
            }
            $countries.bfhcountries($countries.data());
        });

        $('form input[type="text"].bfh-phone, form input[type="tel"].bfh-phone, span.bfh-phone').each(function () {
            var $phone;
            $phone = $(this);
            $phone.bfhphone($phone.data());
        });

    });

//========================== bind/modify mobile phone number ==================
//---------------------------- button timer --------------------------
    var stop;
    $scope.isTiming = false;

    $scope.disableButton = function () {
        if (angular.isDefined(stop)) {
            $scope.isTiming = true;
            return;
        }

        $scope.seconds = 120;

        stop = $interval(function () {
            if ($scope.seconds > 0) {
                $scope.seconds = $scope.seconds - 1;
                $scope.verifyButton = Messages.account.getVerifyCodeButtonTextPrefix + $scope.seconds + Messages.account.getVerifyCodeButtonTextTail;
                $scope.isTiming = true;
            }
            else {
                $scope.stopTiming();
                $scope.verifyButton = Messages.account.getVerifyCodeButtonText;
                $scope.isTiming = false;
            }
        }, 1000);
    };

    $scope.stopTiming = function () {
        if (angular.isDefined(stop)) {
            $interval.cancel(stop);
            stop = undefined;
        }
        $scope.isTiming = false;
        $scope.seconds = 0;
        $scope.verifyButton = Messages.account.getVerifyCodeButtonText;
    };

    $scope.$on('destroy', function () {
        $scope.stopTiming();
    });

    var stop2;
    $scope.isTiming2 = false;

    $scope.disableButton2 = function () {
        if (angular.isDefined(stop2)) {
            $scope.isTiming2 = true;
            return;
        }

        $scope.seconds = 120;

        stop2 = $interval(function () {
            if ($scope.seconds > 0) {
                $scope.seconds = $scope.seconds - 1;
                $scope.verifyButton2 = Messages.account.getVerifyCodeButtonTextPrefix + $scope.seconds + Messages.account.getVerifyCodeButtonTextTail;
                $scope.isTiming2 = true;
            }
            else {
                $scope.stopTiming2();
                $scope.verifyButton2 = Messages.account.getVerifyCodeButtonText;
                $scope.isTiming2 = false;
            }
        }, 1000);
    };

    $scope.stopTiming2 = function () {
        if (angular.isDefined(stop2)) {
            $interval.cancel(stop2);
            stop2 = undefined;
        }
        $scope.isTiming2 = false;
        $scope.seconds = 0;
        $scope.verifyButton2 = Messages.account.getVerifyCodeButtonText;
    };

    $scope.$on('destroy', function () {
        $scope.stopTiming2();
    });

    //$scope.disableButton();
    $scope.bindMobile = {};
// ---------------------- send verification sms -----------------------
    $scope.sendVerifySms = function () {
        $scope.showBindMobileError = false;
        $scope.disableButton();

        $http.post('/smsverification', $.param({phoneNumber: $scope.bindMobile.mobile}))
            .success(function (data, status, headers, config) {
                console.log("send sms result: ", data)
                if (data.success) {
                    $scope.bindMobile.verifyCodeUuid = data.data;
                    //console.log('data = ' + data.data);
                    //console.log('uuid = ' + $scope.account.verifyCodeUuid);
                } else {
                    $scope.showBindMobileError = true;
                    var smsErrorMsg = Messages.getMessage(data.code, data.message);
                    $scope.bindMobileError = smsErrorMsg;
                    if (data.code == 9009) {
                        $scope.stopTiming();
                    }
                }
            });
    };

    $scope.sendVerifySms2 = function () {
        $scope.showBindMobileError = false;
        $scope.disableButton2();

        $http.get('/smsverification2')
            .success(function (data, status, headers, config) {
                console.log('data in withdrawal: ', data);
                if (data.success) {
                    $scope.bindMobile.verifyCodeUuidOld = data.data;
                } else {
                    $scope.stopTiming2();
                    $scope.showBindMobileError = true;
                    $scope.bindMobileError = Messages.getMessage(data.code, data.message)
                    if (data.code == 9009) {
                        $scope.stopTiming2();
                    }
                }
            });
    };

// ----------------------- bindMobile ---------------------
    $scope.verifyButton = Messages.account.getVerifyCodeButtonText;
    $scope.verifyButton2 = Messages.account.getVerifyCodeButtonText;

    $scope.gotoMobileBind = function() {
        $scope.showMainDiv = false;
        $scope.showBindMobileDiv = true;
        $scope.showBindMobileError = false;
        $scope.bindMobile = {};
        $scope.bindMobileForm.$setPristine(true);
    };

    $scope.doBindMobile = function() {
        $scope.showBindMobileError = false;
        $http.post('/account/bindmobile', $.param($scope.bindMobile))
            .success(function (data, status, headers, config) {
                if (data.success) {
                    $scope.showBindMobileError = true;
                    $scope.bindMobileError = Messages.account.bindMobileSucceeded;
                    $window.location.href = '/account#/accountsettings';
                    $window.location.reload();
                } else {
                    $scope.showBindMobileError = true;
                    var errorMsg = Messages.getMessage(data.code, data.message);
                    $scope.bindMobileError = errorMsg;
                }
            });
    };


    var ModalInstanceCtrlMobile = function ($scope, $modalInstance, mobileStatus) {
        $scope.verifyButton = Messages.account.getVerifyCodeButtonText;
        $scope.showChangeMobileSecPreferError = false;
        $scope.changeMobileSec = {};

        var stop;
        $scope.isTiming = false;

        $scope.disableButton = function () {
            if (angular.isDefined(stop)) {
                $scope.isTiming = true;
                return;
            }

            $scope.seconds = 120;

            stop = $interval(function () {
                if ($scope.seconds > 0) {
                    $scope.seconds = $scope.seconds - 1;
                    $scope.verifyButton = Messages.account.getVerifyCodeButtonTextPrefix + $scope.seconds + Messages.account.getVerifyCodeButtonTextTail;
                    $scope.isTiming = true;
                }
                else {
                    $scope.stopTiming();
                    $scope.verifyButton = Messages.account.getVerifyCodeButtonText;
                    $scope.isTiming = false;
                }
            }, 1000);
        };

        $scope.stopTiming = function () {
            if (angular.isDefined(stop)) {
                $interval.cancel(stop);
                stop = undefined;
            }
            $scope.isTiming = false;
            $scope.seconds = 0;
            $scope.verifyButton = Messages.account.getVerifyCodeButtonText;
        };

        $scope.$on('destroy', function () {
            $scope.stopTiming();
        });

        $scope.sendVerifySms = function () {
            $scope.showChangeMobileSecPreferError = false;
            $scope.disableButton();

            $http.get('/smsverification2')
                .success(function (data, status, headers, config) {
                    $scope.showChangeMobileSecPreferError = true;
                    console.log("send sms result: ", data)
                    if (data.success) {
                        $scope.verifyCodeUuidMobile = data.data;
                        //console.log('data = ' + data.data);
                        //console.log('uuid = ' + $scope.account.verifyCodeUuid);
                    } else {
                        var smsErrorMsg = Messages.getMessage(data.code, data.message);
                        $scope.changeMobileSecPreferError = smsErrorMsg;
                        if (data.code == 9009) {
                            $scope.stopTiming();
                        }
                    }
                });
        };

        $scope.ok = function () {
            console.debug("params: ", $scope.verifyCodeUuidMobile, $scope.changeMobileSec.verifycode, mobileStatus);
            $http.post('/preference/phone', $.param({'uuid': $scope.verifyCodeUuidMobile, 'phonecode': $scope.changeMobileSec.verifycode, 'phoneprefer': mobileStatus}))
                .success(function (data, status, headers, config) {
                    $scope.showChangeMobileSecPreferError = true;
                    if (data.success) {
                        $scope.changeMobileSecPreferError = Messages.account.changMobileSecPreferSucceeded;
                        $modalInstance.close();
                    } else {
                        var errorMsg = Messages.getMessage(data.code, data.message);
                        $scope.changeMobileSecPreferError = errorMsg;
                    }
                });
        };

        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };
    };

    var ModalInstanceCtrlEmail = function ($scope, $modalInstance, emailStatus) {
        $scope.showChangeEmailSecPreferError = false;
        $scope.verifyButton = Messages.account.getEmailVerificationCode;
        $scope.emailStatus = emailStatus;
        $scope.changeSecPrefer = {};

        var _stop;
        $scope.isTiming = false;

        $scope.disableButton = function () {
            if (angular.isDefined(_stop)) {
                $scope.isTiming = true;
                return;
            }

            $scope.seconds = 120;

            _stop = $interval(function () {
                if ($scope.seconds > 0) {
                    $scope.seconds = $scope.seconds - 1;
                    $scope.verifyButton = Messages.account.getVerifyCodeButtonTextPrefix + $scope.seconds + Messages.account.getVerifyCodeButtonTextTail;
                    $scope.isTiming = true;
                }
                else {
                    $scope.stopTiming();
                    $scope.verifyButton = Messages.account.getEmailVerificationCode;
                }
            }, 1000);
        };

        $scope.stopTiming = function () {
            if (angular.isDefined(_stop)) {
                $interval.cancel(_stop);
                _stop = undefined;
            }
            $scope.isTiming = false;
            $scope.seconds = 0;
            $scope.verifyButton = Messages.account.getEmailVerificationCode;
        };

        $scope.$on('destroy', function () {
            $scope.stopTiming();
        });

        $scope.sendVerifyEmail = function () {
            $scope.showChangeEmailSecPreferError = false;
            $scope.disableButton();

            $http.get('/emailverification')
                .success(function (data, status, headers, config) {
                    console.debug('data: ', data);
                    if (data.success) {
                        $scope.verifyCodeUuidEmail = data.data;
                    } else {
                        $scope.stopTiming();
                        $scope.showChangeEmailSecPreferError = true;
                        $scope.changeEmailSecPreferError = Messages.getMessage(data.code, data.message);
                    }
                });
        };

        $scope.ok = function () {
            console.debug('params', $scope.verifyCodeUuidEmail, $scope.changeSecPrefer.emailCode);
            $http.post('/preference/email', $.param({'uuid': $scope.verifyCodeUuidEmail, 'emailcode': $scope.changeSecPrefer.emailCode, 'emailprefer': $scope.emailStatus}))
                .success(function (data, status, headers, config) {
                    $scope.showChangeEmailSecPreferError = true;
                    if (data.success) {
                        $scope.changeEmailSecPreferError = Messages.account.changeMailSecPreferSucceeded;
                        $modalInstance.close();
                    } else {
                        var errorMsg = Messages.getMessage(data.code, data.message);
                        $scope.changeEmailSecPreferError = errorMsg;
                    }
                });
        };

        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };
    };

    $scope.changeEmailSecPrefer = function(size) {
        if (!$scope.mobileVerOn && !$scope.googleAuthOn && $scope.emailVerOn) {
            alert(Messages.account.canNotDisableEmailVerify);
            return;
        } else {
            var modalInstance = $modal.open({
                templateUrl: 'ModalContentEmailVer.html',
                controller: ModalInstanceCtrlEmail,
                size: size,
                resolve: {
                    emailStatus: function() {
                        if ($scope.emailVerOn) return "0";
                        else return "1";
                    }
                }
            });

            modalInstance.result.then(function (setRes) {
                $window.location.href = '/account#/accountsettings';
                $window.location.reload();

                //$scope.emailVerOn = ! $scope.emailVerOn;
            }, function () {
                console.info('Modal dismissed at: ' + new Date());
            });
        }
    };

    $scope.changeMobileSecPrefer = function(size) {
        if (!$scope.emailVerOn && !$scope.googleAuthOn && $scope.mobileVerOn) {
            alert(Messages.account.canNotDisableMobileVerify);
            return;
        } else {
            var modalInstance = $modal.open({
                templateUrl: 'ModalContentMobileVer.html',
                controller: ModalInstanceCtrlMobile,
                size: size,
                resolve: {
                    mobileStatus: function() {
                        if ($scope.mobileVerOn) return "0";
                        else return "1";
                    }
                }
            });

            modalInstance.result.then(function (setRes) {
                $window.location.href = '/account#/accountsettings';
                $window.location.reload();

                //$scope.mobileVerOn = ! $scope.mobileVerOn;
            }, function () {
                console.info('Modal dismissed at: ' + new Date());
            });
        }
    };

    $scope.unBindGoogleAuth = function() {
        if (!$scope.emailVerOn && !$scope.mobileVerOn && $scope.googleAuthOn) {
            alert(Messages.account.canNotDisableGoogleAuth);
            return;
        } else {
            $window.location.href = '/account#/googleauth';
        }
    };

//========================== bind/modify mobile phone number end ==================

// --------------------- updateaccountsettings ----------------
    $scope.updateAccountSettings = function () {
        $scope.showUpdateAccountError = false;
        $http.post('/account/settings', $.param($scope.account))
            .success(function (data, status, headers, config) {
                if (data.success) {
                    $scope.showUpdateAccountError = true;
                    $scope.updateAccountErrorMessage = Messages.account.updateAccountProfileSucceeded;
                    $window.location.href = '/account#/accountprofiles';
                    $window.location.reload();
                } else {
                    $scope.showUpdateAccountError = true;
                    var errorMsg = Messages.getMessage(data.code, data.message);
                    $scope.updateAccountErrorMessage = errorMsg;
                }
            });
    };

// ----------------------- changepwd ---------------------
    $scope.changepwd = {};

    $scope.gotoChangePwd = function() {
        $scope.showMainDiv = false;
        $scope.showChangePwdDiv = true;
        $scope.changepwd = {};
        $scope.showChangePwdError = false;
        $scope.changePwdForm.$setPristine(true);
    };

    $scope.doChangePassword = function () {
        $scope.showChangePwdError = false;
        var oldPwd = $.sha256b64($scope.changepwd.oldPassword);
        var newPwd = $.sha256b64($scope.changepwd.newPassword);
        $http.post('/account/dochangepwd', $.param({'oldPassword': oldPwd, 'newPassword': newPwd}))
            .success(function (data, status, headers, config) {
                console.debug("changepwd result: ", data)
                $scope.showChangePwdError = true;
                $scope.changePwdErrorMessage = Messages.account.changePwdSucceeded;
                if (data.success) {
                    $timeout(function() {
                        //$window.location.href = '/account#/accountsettings';
                        //$window.location.reload();
                        $scope.showMainDiv = true;
                        $scope.showChangePwdDiv = false;
                        //console.debug("show main div");
                    }, 1000);
                } else {
                    var errorMsg = Messages.getMessage(data.code, data.message);
                    $scope.changePwdErrorMessage = errorMsg;
                }
            });
    };

});


//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

//------------------- change email security preference ----------------


//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~



app.controller('GoogleAuthCtrl', function ($scope, $http, $interval, $location, $window) {
    $scope.verifyButton = Messages.account.getEmailVerificationCode;
    var qrcode = new QRCode(document.getElementById("qrcode"), {
                            width : 200,
                            height : 200
                        });

    $http.get('/googleauth/get')
    .success(function(data, status, headers, config) {
        if (data.success) {
                $scope.showBind = false;
                $scope.authUrl = data.data.authUrl;
                $scope.secret = data.data.secret;

                qrcode.makeCode($scope.authUrl);
        } else {
            // TODO: handle error & show error messages
            console.log('error', data);
        }
    });

    $scope.bind = function () {
        $http.post('/googleauth/bind/',
            $.param({googlesecret: $scope.secret, googlecode: $scope.verifycode}))
            .success(function (data, status, headers, config) {
                if (data.success) {
                    $window.location.href = '/account#/accountsettings';
                    $window.location.reload();
                } else {
                    alert(Messages.ErrorMessages['m' + data.code]);
                }
            });
    };

    $scope.unbind = function () {
        $http.post('/googleauth/unbind/',
            $.param({googlecode: $scope.verifycode}))
            .success(function (data, status, headers, config) {
            if (data.success) {
                $window.location.href = '/account#/accountsettings';
                $window.location.reload();
            }
            else {
                alert(Messages.ErrorMessages['m' + data.code]);
            }
        });
    };

    // sms verification code and button timer:
//    $scope.showWithdrawalError = false;
//    var _stop;
//    $scope.isTiming = false;
//
//    $scope.disableButton = function () {
//        if (angular.isDefined(_stop)) {
//            $scope.isTiming = true;
//            return;
//        }
//
//        $scope.seconds = 120;
//
//        _stop = $interval(function () {
//            if ($scope.seconds > 0) {
//                $scope.seconds = $scope.seconds - 1;
//                $scope.verifyButton = Messages.account.getVerifyCodeButtonTextPrefix + $scope.seconds + Messages.account.getVerifyCodeButtonTextTail;
//                $scope.isTiming = true;
//            }
//            else {
//                $scope.stopTiming();
//                $scope.verifyButton = Messages.account.getEmailVerificationCode;
//            }
//        }, 1000);
//    };
//
//    $scope.stopTiming = function () {
//        if (angular.isDefined(_stop)) {
//            $interval.cancel(_stop);
//            _stop = undefined;
//        }
//        $scope.isTiming = false;
//        $scope.seconds = 0;
//        $scope.verifyButton = Messages.account.getEmailVerificationCode;
//    };
//
//    $scope.$on('destroy', function () {
//        $scope.stopTiming();
//    });
//
//    $scope.sendVerifyEmail = function () {
//        $scope.showWithdrawalError = false;
//        $scope.disableButton();
//
//        $http.get('/emailverification')
//            .success(function (data, status, headers, config) {
//                if (data.success) {
//                    $scope.verifyCodeUuid = data.data;
//                } else {
//                    $scope.stopTiming();
//                    $scope.showWithdrawalError = true;
//                    $scope.withdrawalErrorMessage = Messages.getMessage(data.code, data.message);
//                }
//            });
//    };
});
//
////ModalDemoCtrl
//app.controller('GoogleModalCtrl', function ($scope, $http, $modal) {
//    $scope.googleAuthButton = Messages.account.getGoogleAuthCodeButtonText;
//    $scope.unbindGoogleAuthButton = Messages.account.unbindGoogleAuthButtonText;
//    $scope.showGoogleAuthButton = Messages.account.showGoogleAuthButtonText;
//
//    $http.get('/googleauth/get')
//        .success(function(data, status, headers, config) {
//            if (data.success) {
//                if(data.data) {
//                    $scope.showGoogleAuth = true;
//                } else {
//                    $scope.showGoogleAuth = false;
//                }
//            }
//        });
//
//    $scope.showGoogleAuthCode = function () {
//        $modal.open({
//            templateUrl: 'showGoogleModal.html',
//            controller: ModalInstanceCtrl,
//            size: "sm",
//            resolve: {
//                showGoogleAuth: function () {
//                    return $scope.showGoogleAuth;
//                }
//            }
//        });
//    };
//
//    $scope.getGoogleAuthCode = function () {
//        var getcodemodal = $modal.open({
//            templateUrl: 'bindGoogleModal.html',
//            controller: ModalInstanceCtrl,
//            size: "sm",
//            resolve: {
//                showGoogleAuth: function () {
//                    return $scope.showGoogleAuth;
//                }
//            }
//        });
//
//        getcodemodal.result.then(function(bindrv){
//            console.log('$scope.showGoogleAuth', bindrv);
//            $scope.showGoogleAuth = bindrv;
//        });
//    };
//
//    $scope.unbindGoogleAuthCode = function () {
//        var unbindcodemodal = $modal.open({
//            templateUrl: 'unbindGoogleModal.html',
//            controller: ModalInstanceCtrl,
//            size: "sm",
//            resolve: {
//                showGoogleAuth: function () {
//                return $scope.showGoogleAuth;
//            }}
//        });
//
//        unbindcodemodal.result.then(function(unbindrv){
//            console.log('$scope.showGoogleAuth', unbindrv);
//            $scope.showGoogleAuth = !unbindrv;
//        })
//    };
//
//    var ModalInstanceCtrl = function ($scope, $modalInstance, showGoogleAuth) {
//        if(showGoogleAuth) {
//            $http.get('/googleauth/get')
//                .success(function(data, status, headers, config) {
//                    if (data.success) {
//                        $scope.authUrl = data.data.authUrl;
//                        $scope.secret = data.data.secret;
//                    }
//                });
//            console.log("get")
//        } else {
//            $http.get('/googleauth/generate/'+$scope.uid)
//                .success(function(data, status, headers, config) {
//                    if (data.success) {
//                        $scope.authUrl = data.data.authUrl;
//                        $scope.secret = data.data.secret;
//                    }
//            });
//            console.log("generate")
//        }
//
//        $scope.bind = function (secret) {
//            $http.post('/googleauth/bind/'+ secret)
//                .success(function (data, status, headers, config) {
//                    $modalInstance.close(true);
//                });
//        };
//
//        $scope.unbind = function (verifycode) {
//            $http.post('/googleauth/unbind/'+verifycode)
//                .success(function (data, status, headers, config) {
//                    if (data.success) {
//                        alert(Messages.account['unbindGoogleAuthOk']);
//                        $modalInstance.close(true);
//                    }
//                    else alert(Messages.ErrorMessages['m' + data.code]);
//                });
//        };
//
//        $scope.close = function () {
//            $modalInstance.close(false);
//        };
//    };
//});
