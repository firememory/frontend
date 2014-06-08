var app = angular.module('coinport.account', ['ui.bootstrap', 'ngResource', 'ngRoute', 'coinport.app', 'navbar', 'timer']);

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
    $http.get('/api/account/' + $scope.uid)
        .success(function (data, status, headers, config) {
            $scope.accounts = data.data.accounts;
        });
    $scope.status = {};
    $scope.depositAddresses = {};
    $scope.getCurrencyDetails = function(currency) {
        // get network status
        $scope.timestamp = new Date().getTime();
        $http.get('/api/open/network/' + currency)
            .success(function(data, status, headers, config) {
                $scope.status[currency] = data.data;
        });
        // get deposit address
        $http.get('/depoaddr/' + currency+ '/' + $scope.uid)
            .success(function (data, status, headers, config) {
                $scope.depositAddresses[currency] = data.data;
        });
    };
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
    $scope.loadWithdrawals = function () {
        $http.get('/api/' + $scope.currency + '/transfer/' + $scope.uid, {params: {limit: 15, page: $scope.page, 'type': 1}})
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
            $scope.depositAddress = data.data;
        });

    $scope.page = 1;
    $scope.loadDeposits = function () {
        $http.get('/api/' + $scope.currency + '/transfer/' + $scope.uid, {params: {limit: 15, page: $scope.page, 'type': 0}})
            .success(function (data, status, headers, config) {
                $scope.deposits = data.data.items;
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

    $http.get('/api/account/' + $scope.uid)
        .success(function (data, status, headers, config) {
            console.log(data.data.accounts[$scope.currency]);
            $scope.balance = data.data.accounts[$scope.currency];
        });

    $http.get('/withaddr/' +$scope.currency+ '/' + $scope.uid)
        .success(function (data, status, headers, config) {
            $scope.withdrawalData.address = data.data;
        });

    $scope.page = 1;
    $scope.loadWithdrawals = function () {
        $http.get('/api/' + $scope.currency + '/transfer/' + $scope.uid, {params: {limit: 15, page: $scope.page, 'type': 1}})
            .success(function (data, status, headers, config) {
                $scope.withdrawals = data.data.items;
                $scope.count = data.data.count;
            });
    };
    $scope.loadWithdrawals();

    $scope.withdrawalData = {currency: $scope.currency};
    $scope.withdrawal = function () {
        if (! $scope.withdrawalData.amount || $scope.withdrawalData.amount < 0) {
            alert('Invalid amount');
            return;
        }
        if (!$scope.withdrawalData.address || $scope.withdrawalData.address == '') {
            alert('Withdrawal address needed');
            return;
        }
        console.log('withdrawal ' + $scope.withdrawalData.amount);
        $http.post('/account/withdrawal', $.param($scope.withdrawalData))
            .success(function (data, status, headers, config) {
                if (data.success) {
                    var withdrawal = data.data.transfer;
                    alert('Withdrawal request submitted.');
                    setTimeout($scope.loadWithdrawals, 1000);
                } else {
                    alert('Withdrawal failed.');
                }
            });
    };

    $scope.changeCurrency = function() {
        $location.path('/withdrawal/' + $scope.currency);
    };

    // sms verification code and button timer:
    $scope.showWithdrawalError = false;
    $scope.verifyButton = Messages.account.getVerifyCodeButtonText;
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
                $scope.verifyButton = Messages.account.getVerifyCodeButtonText;
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
        $scope.verifyButton = Messages.account.getVerifyCodeButtonText;
    };

    $scope.$on('destroy', function () {
        $scope.stopTiming();
    });

    $scope.sendVerifySms = function () {
        $scope.showWithdrawalError = false;
        $scope.disableButton();

        $http.get('/smsverification2')
            .success(function (data, status, headers, config) {
                console.log('data in withdrawal: ', data);
                if (data.success) {
                    $scope.withdrawalData.verifyCodeUuid = data.data;
                } else {
                    $scope.stopTiming();
                    $scope.showWithdrawalError = true;
                    $scope.withdrawalErrorMessage = Messages.getMessage(data.code, data.message);
                }
            });
    };
}]);

app.controller('AssetCtrl', function ($scope, $http) {
    $scope.pieTitle = Messages.asset.assetComposition;
    $http.get('/api/asset/' + $scope.uid)
        .success(function (data, status, headers, config) {
            $scope.assets = data.data;
            drawHistoryChart($scope.assets);
            var map = $scope.assets[$scope.assets.length - 1].amountMap;
            $scope.pieData = [];
            var total = 0;
            for (asset in map) {
                total += map[asset];
            }

            for (asset in map) {
                $scope.pieData.push({title: asset, value: map[asset] / total});
            }

            drawPieChart($scope.pieData);
            $scope.updateAsset();
        });

    var drawPieChart = function (data) {
        var DURATION = 1500;
        var DELAY = 500;

        var containerEl = document.getElementById('user-finance-chart-pie'),
            width = containerEl.clientWidth,
            height = width * 0.8,
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
            .range(["rgba(15, 157, 88, 0.5)", "rgba(66, 133, 244, 0.5)"]);

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
            })
            .each('end', function handleAnimationEnd(d) {
                drawDetailedInformation(d.data, this);
            });

        drawChartCenter();

        function drawChartCenter() {
            var centerContainer = pie.append('g')
                .attr('class', 'pieChart--center');

            centerContainer.append('circle')
                .attr('class', 'pieChart--center--outerCircle')
                .attr('r', 0)
                .attr('filter', 'url(#pieChartDropShadow)')
                .transition()
                .duration(DURATION)
                .delay(DELAY)
                .attr('r', radius - 50);

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

            if (( bBox.x + bBox.width / 2 ) > 0) {
                infoContainer = detailedInfo.append('g')
                    .attr('width', infoWidth)
                    .attr(
                        'transform',
                        'translate(' + ( width - infoWidth ) + ',' + ( bBox.height + bBox.y ) + ')'
                    );
                anchor = 'end';
                position = 'right';
            } else {
                infoContainer = detailedInfo.append('g')
                    .attr('width', infoWidth)
                    .attr(
                        'transform',
                        'translate(' + 0 + ',' + ( bBox.height + bBox.y ) + ')'
                    );
                anchor = 'start';
                position = 'left';
            }

            infoContainer.data([ data.value * 100 ])
                .append('text')
                .text('0 %')
                .attr('class', 'pieChart--detail--percentage')
                .attr('x', ( position === 'left' ? 0 : infoWidth ))
                .attr('y', -10)
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
                .attr('y1', 0)
                .attr('y2', 0)
                .transition()
                .duration(DURATION)
                .attr('x2', infoWidth);

            infoContainer.data([ data.description ])
                .append('foreignObject')
                .attr('width', infoWidth)
                .attr('height', 100)
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
                layer.push({x: d.timestamp, y: d.amountMap[key]});
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

        var margin = {top: 10, right: 10, bottom: 30, left: 40},
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
            .range(["rgba(15, 157, 88, 0.5)", "rgba(66, 133, 244, 0.5)"]);

        var xAxis = d3.svg.axis()
            .scale(x)
            .tickSize(0)
            .tickPadding(6)
            .tickFormat(function (d) {
                return new Date(d).getDate()
            })
            .orient("bottom");

        var yAxis = d3.svg.axis()
            .scale(y)
            .orient("left")
            .tickFormat(d3.format(".2s"));

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
                var map = $scope.assets[$scope.assets.length - 1].amountMap;
                for (currency in $scope.accounts) {
                    var account = $scope.accounts[currency];
                    account.total = account.available.value + account.locked.value + account.pendingWithdrawal.value;
                    account.asset = map[currency];
                    account.price = (account.asset/account.total);
                }
            });
    };
});

app.controller('UserTxCtrl', ['$scope', '$http', function ($scope, $http) {
    $scope.market = COINPORT.defaultMarket;
    $scope.page = 1;
    $scope.limit = 15;

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
    $scope.limit = 15;

    $scope.changeMarket = function() {
        $scope.page = 1;
        $scope.reload();
    };

    $scope.reload = function() {
        $http.get('/api/' + $scope.market + '/order', {params: {limit: $scope.limit, page: $scope.page}})
            .success(function(data, status, headers, config) {
                $scope.orders = data.data;
            });
    };

    $scope.showDetail = function (order) {
        $scope.$parent.order = order;
        $location.path('/order');
    };

    $scope.cancelOrder = function (order) {
        var market = order.subject + order.currency;
        $http.get('/trade/' + market + '/order/cancel/' + order.id)
            .success(function (data, status, headers, config) {

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

app.controller('AccountSettingsCtrl', ['$scope', '$http', '$interval', '$window', function ($scope, $http, $interval, $window) {
    $scope.showUpdateAccountError = false;

    $scope.account = {};
    // $scope.credentialItems = [
    //     {"name": "身份证", "value": "1"},
    //     {"name": "护照", "value": "2"}
    // ];
    // $scope.credentialType = $scope.credentialItems[0];

    $scope.verifyButton = Messages.account.getVerifyCodeButtonText;

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

    //$scope.disableButton();

    $scope.sendVerifySms = function () {
        $scope.showUpdateAccountError = false;
        $scope.disableButton();

        $http.post('/smsverification', $.param({phoneNumber: $scope.account.mobile}))
            .success(function (data, status, headers, config) {
                if (data.success) {
                    $scope.account.verifyCodeUuid = data.data;
                    console.log('data = ' + data.data);
                    console.log('uuid = ' + $scope.account.verifyCodeUuid);
                } else {
                    $scope.showUpdateAccountError = true;
                    $scope.updateAccountErrorMessage = data.message;
                    $scope.stopTiming();
                }
            });
    };

    $scope.updateAccountSettings = function () {
        $scope.showUpdateAccountError = false;
        $http.post('/account/settings', $.param($scope.account))
            .success(function (data, status, headers, config) {
                if (data.success) {
                    $scope.showUpdateAccountError = true;
                    $scope.updateAccountErrorMessage = Messages.account.updateAccountProfileSucceeded;
                    //$window.location.href = '/account#/accountsettings';
                    $window.location.reload(true);
                } else {
                    $scope.showUpdateAccountError = true;
                    var errorMsg = Messages.getMessage(data.code, data.message);
                    $scope.updateAccountErrorMessage = errorMsg;
                }
            });
    };
}]);
