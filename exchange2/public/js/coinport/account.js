var app = angular.module('coinport.account', ['ui.bootstrap', 'coinport.app', 'navbar']);

function httpConfig($httpProvider) {
    $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
}

app.config(httpConfig);

app.controller('TransferCtrl', ['$scope', '$http', '$timeout', function ($scope, $http, $timeout) {
    $scope.page = 1;
    $scope.limit = 25;

    $scope.addressUrl = COINPORT.addressUrl[$scope.currency];
    $scope.txUrl = COINPORT.txUrl[$scope.currency];

    $scope.withdrawalLimit = 0.01;
    $scope.withdrawalFee = '0.0005';

    $scope.allCoinsWithName = Messages.coinName;
    $scope.allCoins = Messages.coins;
    if (!$scope.currency) { $scope.currency = $scope.allCoins[0]; }

    $scope.loadAccount = function () {
        $http.get('/api/account/' + $scope.uid)
            .success(function (data, status, headers, config) {
                $scope.accounts = data.data.accounts;
            });
    }
    $scope.loadAccount();

    $scope.status = {};
    $scope.depositAddresses = [];

    $http.get('/depoaddr/' + $scope.uid)
        .success(function (data, status, headers, config) {
            for (var curr in Messages.coins) {
                //hack BTSX
                if (Messages.coins[curr] == 'BTSX') {
                    $scope.depositAddresses['BTSX'] = 'BTS5FPJkXFwokNEsRLwfWvPKAbzriNLS5ut823rMzHbpKMg9QgYWZ';//'cpdeposit' + (+$scope.uid - 1000000000);
                } else if (Messages.coins[curr] == 'XRP') {
                    $scope.depositAddresses['XRP'] = 'r9AzyYGGQAvgefdgeu3eDHaVdxLdpAvchE';
                } else if (Messages.coins[curr] == "NXT") {
                    var nxtAddrs = data.data['NXT'].split("//");
                    $scope.depositAddresses['NXT'] = nxtAddrs[0] + Messages.transfer.nxtOr + nxtAddrs[1];
                    $scope.nxtPublicKey = nxtAddrs[2];
                } else if (Messages.coins[curr] == 'GOOC') {
                    $scope.depositAddresses['GOOC'] = '15026841984';
                } else $scope.depositAddresses[Messages.coins[curr]] = data.data[Messages.coins[curr]];
            }
            //console.debug("$scope.depositAddresses: ", $scope.depositAddresses);
        });

    $scope.deposits = {};
    // $scope.transfers = {};
    // $scope.loadTransfers = function () {
    //     $http.get('/api/ALL/transfer/' + $scope.uid, {params: {limit: $scope.limit, page: $scope.page}})
    //         .success(function (data, status, headers, config) {
    //             $scope.transfers = data.data.items;
    //             $scope.transfers.forEach(function(item){
    //                 item.txlink =  COINPORT.txUrl[item.amount.currency]+item.txid;
    //             });
    //             $scope.count = data.data.count;
    //         });
    // };

    // $scope.loadTransfers();
    //$scope.depositAddress = [];
    //console.debug("currency and address:  ", $scope.currency, $scope.depositAddresses);

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


    $scope.loadWithdrawals = function () {
        //console.debug("loadWithdrawals...");
        $http.get('/api/' + $scope.currency + '/transfer/' + $scope.uid, {params: {limit: $scope.limit, page: $scope.page, 'type': 1}})
            .success(function (data, status, headers, config) {
                //console.debug("loadWithdrawals:", data.data);
                $scope.withdrawals = data.data.items;
                $scope.count = data.data.count;
            });
    };

    $scope.changeCurrency = function() {
        //console.debug("currency and address:  ", $scope.currency, $scope.depositAddresses);
        $scope.loadDeposits();
        $scope.loadWithdrawals();

        switch ($scope.currency)  {
          case "NXT":
            $scope.withdrawalLimit = 10;
            break;
          case "BTSX":
            $scope.withdrawalLimit = 10;
            break;
          case "XRP":
            $scope.withdrawalLimit = 10;
            break;
          case "DOGE":
            $scope.withdrawalLimit = 5;
            break;
          case "CNY":
            $scope.withdrawalLimit = 500;
            break;
          case "GOOC":
            $scope.withdrawalLimit = 1000;
            break;
          default :
            $scope.withdrawalLimit = 0.01;
            break;
        };

        switch ($scope.currency) {
          case "NXT":
            $scope.withdrawalFee = '2';
            break;
          case "BTSX":
            $scope.withdrawalFee = '2';
            break;
          case "XRP":
            $scope.withdrawalFee = '1';
            break;
          case "DOGE":
            $scope.withdrawalFee = '2';
            break;
          case "CNY":
            $scope.withdrawalFee = '0.4%';
            break;
          case "GOOC":
            $scope.withdrawalFee = '1';
            break;
          default :
            $scope.withdrawalFee = '0.0005';
            break;
        };
    };

    $scope.changeCurrency();

    /* back card management.  */
    $scope.loadBankCards = function() {
        $http.get('/account/querybankcards').success(function(data, status, headers, config) {
            $scope.bankCards = data.data || [];
            if ($scope.bankCards.length === 0) {
                $scope.selectedBankCard = null;
            } else {
                $scope.selectedBankCard = $scope.bankCards[0];
            }
        });
    };

    if ($scope.currency === 'CNY') {
        $scope.loadBankCards();
    }

    $scope.bankCardToString = function(card) {
        if (card)
            return card.ownerName + ' | ' + card.cardNumber +
            ' | ' + card.bankName + ' | ' + card.branchBankName;
        else
            return '';
    };

    $('#add-bankcard').popover({
        html: true,
        trigger: 'manual'
    }).on('shown.bs.popover', function () {
        var $popup = $(this);

        var bankCard = {
            ownerName: '',
            bankName: '',
            branchBankName: '',
            cardNumber: '',
            emailCode: '',
            verifyCodeUuidEmail: ''
        };

        $(this).next('.popover').find('button.cancel').click(function (e) {
            $popup.popover('hide');
        });

        $(this).next('.popover').find('button.sendmail').click(function (e) {
            $http.get('/emailverification')
                .success(function (data, status, headers, config) {
                    //console.debug('add bank emailverification data: ', data);
                    if (data.success) {
                        bankCard.verifyCodeUuidEmail = data.data;
                    } else {
                        $popup.popover('hide');
                        var errorMsg = Messages.getMessage(data.code, data.message);
                        $scope.displayWithdrawalError(errorMsg);
                    }
                });
        });

        $(this).next('.popover').find('button.save').click(function (e) {
            bankCard.ownerName = $(".popover #owner-name").val();
            bankCard.cardNumber = $(".popover #card-number").val();
            bankCard.bankName = $(".popover #bank-name").val();
            bankCard.emailCode = $(".popover #email-code").val();
            bankCard.branchBankName = $(".popover #bank-branch-name").val();
            $scope.addBankCard(bankCard);
            $popup.popover('hide');
        });
    });

    $scope.addBankCard = function(card) {
        console.debug("add bank card: ", card);
        $http.post('/account/addbankcard', $.param(card))
            .success(function (data, status, headers, config) {
                console.debug("addBankCard result: ", data.data);
                if (data.success) {
                    $scope.loadBankCards();
                } else {
                    $scope.displayWithdrawalError(Messages.getMessage(data.code));
                }
            });
    };

    $scope.deleteBankCard = function() {
        console.debug("bankcard to be deleted: ", $scope.selectedBankCard);
        $http.post('/account/deletebankcard', $.param($scope.selectedBankCard)).success(function (data, status, headers, config) {
            if (data.success) {
                $scope.loadBankCards();
            } else {
                $scope.displayWithdrawalError(Messages.getMessage(data.code));
            }
        });
    };
    /* back card management end.  */

    /* withdrawal control:  */

    $scope.displayWithdrawalError = function (msg) {
        $scope.showWithdrawalError = true;
        $scope.withdrawalErrorMessage = msg;
        //$scope.$apply();
        $timeout(function () {
            console.debug("fade withdrawal error.");
            $scope.showWithdrawalError = false;
            $scope.withdrawalErrorMessage = '';
        }, 6000);
    };

    $scope.withdrawalData = {currency: $scope.currency};

    $scope.renewWithdrawalPage = function() {
        $scope.withdrawalData = {currency: $scope.currency};
        $scope.isTimingEmail = false;
        $scope.isTimingSms = false;
    };

    $scope.withdrawal = function () {
        if (! $scope.withdrawalData.amount || +$scope.withdrawalData.amount > $scope.accounts[$scope.currency].available.value) {
            $scope.displayWithdrawalError(Messages.transfer.messages['invalidAmount']);
            return;
        }
        if ($scope.currency === 'CNY') {
            if (!$scope.selectedBankCard) {
                $scope.displayWithdrawalError(Messages.transfer.messages['invalidAddress']);
                return;
            } else {
                var card = $scope.selectedBankCard;
                $scope.withdrawalData.address = card.ownerName + '|' + card.cardNumber + '|' + card.bankName + '|' + (card.branchBankName || '');
            }
        } else {
            if (!$scope.withdrawalData.address || $scope.withdrawalData.address == '') {
                $scope.displayWithdrawalError(Messages.transfer.messages['invalidAddress']);
                return;
            }
        }

        if ($scope.emailVerOn && (!$scope.withdrawalData.emailuuid || $scope.withdrawalData.emailuuid == '')) {
            $scope.displayWithdrawalError(Messages.transfer.messages['emailNotSend']);
            return;
        }

        if ($scope.mobileVerOn && (!$scope.withdrawalData.phoneuuid || $scope.withdrawalData.phoneuuid == '')) {
            $scope.displayWithdrawalError(Messages.transfer.messages['smsNotSend']);
            return;
        }

        $scope.withdrawalData.currency = $scope.currency;
        $http.post('/account/withdrawal', $.param($scope.withdrawalData))
            .success(function (data, status, headers, config) {
                if (data.success) {
                    var withdrawal = data.data.transfer;
                    var wd_address = $scope.withdrawalData.address;
                    $scope.withdrawalData = {currency: $scope.currency};
                    $scope.withdrawalData.address = wd_address;
                    $scope.displayWithdrawalError(Messages.transfer.messages['ok']);
                } else {
                    $scope.renewWithdrawalPage();
                    $scope.displayWithdrawalError(Messages.getMessage(data.code));
                }
                setTimeout(function() {
                    $scope.loadAccount();
                    $scope.loadWithdrawals();
                }, 1000);
            });
    };

    $scope.cancelWithdrawal = function (tid) {
        $http.post('/account/cancelWithdrawal/' + $scope.uid + '/' + tid, {})
            .success(function (data, status, headers, config) {
                $scope.loadAccount();
               $scope.loadWithdrawals();
            });
    };

    // email verification:
    $scope.sendVerifyEmail = function () {
        $scope.isTimingEmail = true;
        $http.get('/emailverification')
            .success(function (data, status, headers, config) {
                console.log('sendVerifyEmail result : ', data);
                if (data.success) {
                    $scope.withdrawalData.emailuuid = data.data;
                    setTimeout(function () { $scope.isTimingEmail = false }, 60000);
                } else {
                    $scope.displayWithdrawalError(Messages.getMessage(data.code, data.message));
                    $scope.isTimingEmail = false;
                }
            });
    };

    $scope.sendVerifySms = function () {
        $scope.isTimingSms = true;
        $http.get('/smsverification2')
            .success(function (data, status, headers, config) {
                console.log('sendVerifySms result: ', data);
                if (data.success) {
                    $scope.withdrawalData.phoneuuid = data.data;
                    setTimeout(function () { $scope.isTimingSms = false }, 60000);
                } else {
                    $scope.displayWithdrawalError(Messages.getMessage(data.code, data.message));
                    $scope.isTimingSms = false;
                }
            });
    };

    /* withdrawal control end */

}]);

app.controller('AssetCtrl', function ($scope, $http) {
    $http.get('/api/asset/' + $scope.uid)
        .success(function (data, status, headers, config) {
            $scope.assets = data.data;
            var map = $scope.assets[$scope.assets.length - 1].amountMap;
            var total = 0;
            for (asset in map) {
                total += map[asset].value;
            }

            $scope.updateAsset();
        });

    $http.get('/api/BTC-CNY/transaction?limit=1&skip=0')
        .success(function(data, status, headers, config) {
            if (data.data && data.data.items && data.data.items.length > 0)
                $scope.cnyPrice = data.data.items[0].price.value;
            else
                $scope.cnyPrice = 0.0;
        });

    $scope.totalAssetBtc = 0;
    $scope.totalAssetCny = 0;

    $scope.updateAsset = function () {
        $http.get('/api/account/' + $scope.uid)
            .success(function (response, status, headers, config) {
                $scope.accounts = response.data.accounts;
                var amountMap = $scope.assets[$scope.assets.length - 1].amountMap;

                var priceMap = $scope.assets[$scope.assets.length - 1].priceMap;

                $scope.totalAssetBtc = 0.0;

                for (currency in $scope.accounts) {
                    var account = $scope.accounts[currency];
                    account.asset = amountMap[currency].display;
                    account.price = priceMap[currency].display;
                    $scope.totalAssetBtc += parseFloat(account.asset);
                }

                if ($scope.cnyPrice > 0) {
                    $scope.totalAssetCny = $scope.cnyPrice * $scope.totalAssetBtc;
                } else if ($scope.accounts['CNY']) {
                    $scope.totalAssetCny = $scope.accounts['CNY'].total.value;
                } else {
                    $scope.totalAssetCny = 0.0;
                }

                $scope.totalAssetBtc = $scope.totalAssetBtc.toFixed(4);
                if($scope.totalAssetCny.toString().length < 10)
                    $scope.totalAssetCny =  $scope.totalAssetCny.toFixed(2);
                else
                    $scope.totalAssetCny =  $scope.totalAssetCny.toFixed(0);
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

app.controller('UserOrderCtrl', ['$scope', '$http', '$location', function ($scope, $http, $location, $timeout) {
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
        $http.get('/trade/' + order.subject + '-' + order.currency + '/order/cancel/' + order.id)
            .success(function(data, status, headers, config) {
                if (data.success) {
                    $timeout($scope.reload, 1000);
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

app.controller('AccountProfilesCtrl', function ($scope, $window, $http) {
    var freeThreshold = 1000000000 + 1440;

    $('#nickname-setter').popover({
        html: true,
        trigger: 'manual'
    }).on('shown.bs.popover', function () {
        var $popup = $(this);
        $(this).next('.popover').find('button.cancel').click(function (e) {
            $popup.popover('hide');
        });
        $(this).next('.popover').find('button.save').click(function (e) {
            $popup.popover('hide');
            var nickname = $(".popover #nickname").val();
            console.debug("nickname: ", nickname);
            $scope.setNickName(nickname);
        });
    });

    $scope.showFree = function() {
        return ($scope.uid <= freeThreshold);
    };

    $scope.setNickName = function(name) {
        console.debug("usernickname: ", name);
        $http.post('/account/updatenickname', $.param({'nickname': name}))
            .success(function (data, status, headers, config) {
                $scope.showNicknameError = true;
                if (data.success) {
                    $window.location.href = '/account/profile';
                    $window.location.reload();
                } else {
                    var errorMsg = Messages.getMessage(data.code, data.message);
                    $scope.nicknameError = errorMsg;
                }
            });
    };

    $scope.apiToken = "";
    $scope.getApiToken = function() {
        $http.get('/generateapitoken')
            .success(function(data, status, headers, config) {
                //console.debug('data: ', data);
                if (data.success) {
                    $scope.apiToken = data.data;
                }
            });
    }

});

app.controller('AccountSettingsCtrl', function ($scope, $http, $interval, $window, $timeout) {
    $scope.showSettingsError = false;
    $scope.settingsErrorMessage = "";

    $scope.displaySettingsMessage = function(msg) {
        $scope.showSettingsError = true;
        $scope.settingsErrorMessage = msg;
        $scope.$apply();

        $timeout(function() {
            console.debug("fade error message");
            $scope.showSettingsError = false;
            $scope.settingsErrorMessage = "";
        }, 6000);
    }

    $('#pwd-update').popover({
        html: true,
        trigger: 'manual'
    }).on('shown.bs.popover', function () {
        var $popup = $(this);
        $(this).next('.popover').find('button.cancel').click(function (e) {
            $popup.popover('hide');
        });
        $(this).next('.popover').find('button.save').click(function (e) {
            var oldPwd = $(".popover #oldPwd").val();
            var newPwd = $(".popover #newPwd").val();
            var newPwdConfirm = $(".popover #newPwdConfirm").val();
            //console.debug("changepwd: ", oldPwd, newPwd, newPwdConfirm);
            if(newPwd != newPwdConfirm) {
                $scope.displaySettingsMessage("修改密码失败：确认密码和新密码不一致！");
            } else if(newPwd.length < 6 || newPwd.length > 60) {
                $scope.displaySettingsMessage("修改密码失败：密码长度必须在6和60之间！");
            } else {
                $scope.doChangePassword(oldPwd, newPwd);
            }
            $popup.popover('hide');
        });
    });

// ----------------------- changepwd ---------------------
    $scope.doChangePassword = function (oldPassword, newPassword) {
        var oldPwd = $.sha256b64(oldPassword);
        var newPwd = $.sha256b64(newPassword);
        $http.post('/account/dochangepwd', $.param({'oldPassword': oldPwd, 'newPassword': newPwd}))
            .success(function (data, status, headers, config) {
                //console.debug("changepwd result: ", data)
                if (data.success) {
                    $scope.displaySettingsMessage(Messages.account.changePwdSucceeded);
                } else {
                    var errorMsg = Messages.getMessage(data.code, data.message);
                    $scope.displaySettingsMessage(errorMsg);
                }
            });
    };

    $('#email-switch').popover({
        html: true,
        trigger: 'manual'
    }).on('shown.bs.popover', function () {
        var $popup = $(this);
        $scope.sendVerifyEmail();
        $(this).next('.popover').find('button.cancel').click(function (e) {
            $popup.popover('hide');
        });
        $(this).next('.popover').find('button.save').click(function (e) {
            var emailCode = $(".popover #email-verify-code").val();
            $scope.changeEmailSecPrefer(emailCode);
            $popup.popover('hide');
        });
    });

    $scope.verifyCodeUuidEmail = '';
    $scope.sendVerifyEmail = function () {
        $http.get('/emailverification')
            .success(function (data, status, headers, config) {
                //console.debug('data: ', data);
                if (data.success) {
                    $scope.verifyCodeUuidEmail = data.data;
                } else {
                    console.info("send verify email failed.");
                }
            });
    };

    $scope.changeEmailSecPrefer = function (verCode) {
        if (!$scope.mobileVerOn && !$scope.googleAuthOn && $scope.emailVerOn) {
            $scope.displaySettingsMessage(Messages.account.canNotDisableEmailVerify);
            return;
        } else {
            var emailStatus = "";
            //console.debug('params', $scope.verifyCodeUuidEmail, verCode);
            if ($scope.emailVerOn) emailStatus = "0"; else emailStatus = "1";

            $http.post('/preference/email',
                       $.param({'uuid': $scope.verifyCodeUuidEmail,
                                'emailcode': verCode,
                                'emailprefer': emailStatus}))
                .success(function (data, status, headers, config) {
                    $scope.showChangeEmailSecPreferError = true;
                    if (data.success) {
                        return $window.location.href = '/account/settingsPage';
                    } else {
                        var errorMsg = Messages.getMessage(data.code, data.message);
                        $scope.displaySettingsMessage(errorMsg);
                    }
                });
        }
    };

    var validateMobileNumber = function(location, number) {
        if (location && number) {
            var numberTrimed = number.trim();
            if (location === 'zh-CN') {
                return ((numberTrimed.indexOf("+86") == 0) && (numberTrimed.substring(3).trim().length == 11)) || ((numberTrimed.indexOf("0086") == 0) && (numberTrimed.substring(4).trim().length == 11)) || (numberTrimed.length == 11);
            } else if (location === 'USA') {
                return numberTrimed.length >= 9;
            } else if (location === 'other') {
                return numberTrimed.length >= 9;
            } else {
                return false;
            }
        } else {
            return false;
        }
    };

    var regularMobileNumber = function(location, number) {
        var numberTrimed = number.trim();
        if (location === 'zh-CN') {
            if (numberTrimed.indexOf("+86") == 0) {
                return "+86" + numberTrimed.substring(3).trim();
            } else if (numberTrimed.indexOf("0086") == 0) {
                return "+86" + numberTrimed.substring(4).trim();
            } else {
                return "+86" + numberTrimed;
            }
        } else {
            return numberTrimed;
        }
    };

    $scope.bindMobile = {};

    $('#mobile-bind').popover({
        html: true,
        trigger: 'manual'
    }).on('shown.bs.popover', function () {
        var $popup = $(this);
        $(".popover #mobile-bind-error-div").hide();
        //$scope.sendVerifyEmail();
        $(this).next('.popover').find('button.sendsms').click(function (e) {
            var location = $(".popover #mobile-region").val();
            var phoneNumber = $(".popover #mobile-number").val();
            var regularNumber = regularMobileNumber(location, phoneNumber);
            //console.debug("mobile bind: ", location, phoneNumber);
            if (validateMobileNumber(location, phoneNumber)) {
                $(".popover #bind-mobile-sendsms").attr("disabled", true);

                $http.post('/smsverification', $.param({phoneNumber: regularNumber}))
                    .success(function (data, status, headers, config) {
                        console.log("send sms result: ", data)
                        if (data.success) {
                            $scope.bindMobile.verifyCodeUuid = data.data;
                        } else {
                            var smsErrorMsg = Messages.getMessage(data.code, data.message);
                            $(".popover #mobile-bind-error-msg").text(smsErrorMsg);
                            $(".popover #mobile-bind-error-div").show();
                            $timeout(function() {
                                $(".popover #mobile-bind-error-div").hide();
                                $(".popover #bind-mobile-sendsms").attr("disabled", false);
                            }, 5000);
                        }
                    });

            } else {
                //alert("电话号码格式错误");
                $(".popover #mobile-bind-error-msg").text("电话号码格式错误");
                $(".popover #mobile-bind-error-div").show();
                $timeout(function() {
                    $(".popover #mobile-bind-error-div").hide();
                }, 5000);
            }
        });

        $(this).next('.popover').find('button.cancel').click(function (e) {
            $popup.popover('hide');
        });
        $(this).next('.popover').find('button.save').click(function (e) {
            //var emailCode = $(".popover #email-verify-code").val();
            //$scope.changeEmailSecPrefer(emailCode);
            var location = $(".popover #mobile-region").val();
            var phoneNumber = $(".popover #mobile-number").val();
            $scope.bindMobile.mobile = regularMobileNumber(location, phoneNumber);
            $scope.bindMobile.verifyCode = $(".popover #mobile-verify-code").val();

            $scope.doBindMobile();
            $popup.popover('hide');
        });
    });

    $('#mobile-switch').popover({
        html: true,
        trigger: 'manual'
    }).on('shown.bs.popover', function () {
        var $popup = $(this);
        $scope.sendVerifySms2();

        $(this).next('.popover').find('button.cancel').click(function (e) {
            $popup.popover('hide');
        });
        $(this).next('.popover').find('button.save').click(function (e) {
            var mobileCode = $(".popover #mobile-verify-code1").val();
            $scope.changeMobileSecPrefer(mobileCode);
            $popup.popover('hide');
        });
    });

    $scope.verifyCodeUuidMobile = '';

    $scope.sendVerifySms2 = function () {
        $scope.showBindMobileError = false;
        $http.get('/smsverification2')
            .success(function (data, status, headers, config) {
                console.log('data : ', data);
                if (data.success) {
                    $scope.verifyCodeUuidMobile = data.data;
                } else {
                    $scope.showBindMobileError = true;
                    $scope.bindMobileError = Messages.getMessage(data.code, data.message)
                }
            });
    };

    $scope.changeMobileSecPrefer = function(verifyCode) {
        if (!$scope.emailVerOn && !$scope.googleAuthOn && $scope.mobileVerOn) {
            $scope.displaySettingsMessage(Messages.account.canNotDisableMobileVerify);
            return;
        } else {
            var mobileStatus = '';
            if ($scope.mobileVerOn) mobileStatus = '0'; else mobileStatus = '1';
            $http.post('/preference/phone',
                       $.param({'uuid': $scope.verifyCodeUuidMobile,
                                'phonecode': verifyCode,
                                'phoneprefer': mobileStatus}))
                .success(function (data, status, headers, config) {
                    if (data.success) {
                        $scope.changeMobileSecPreferError = Messages.account.changMobileSecPreferSucceeded;
                        return $window.location.href = '/account/settingsPage';
                    } else {
                        var errorMsg = Messages.getMessage(data.code, data.message);
                        $scope.displaySettingsMessage(errorMsg);
                    }
                });
        }
    };

    //realname-verify
    $('#realname-verify').popover({
        container: 'body',
        html: true,
        trigger: 'manual'
    }).on('shown.bs.popover', function () {
        var $popup = $(this);
        $(this).next('.popover').find('button.cancel').click(function (e) {
            $popup.popover('hide');
        });
        $(this).next('.popover').find('button.save').click(function (e) {
            $scope.realNameVerify.realName = $(".popover #realname").val();
            $scope.realNameVerify.location = $(".popover #location").val();
            $scope.realNameVerify.identiType = $(".popover #identity-type").val();
            $scope.realNameVerify.idNumber = $(".popover #identity-code").val();
            $scope.doRealNameVerify();
            $popup.popover('hide');
        });
    });

    $scope.doBindMobile = function() {
        $scope.showBindMobileError = false;
        $http.post('/account/bindmobile', $.param($scope.bindMobile))
            .success(function (data, status, headers, config) {
                if (data.success) {
                    return $window.location.href = '/account/settingsPage';
                } else {
                    var errorMsg = Messages.getMessage(data.code, data.message);
                    $scope.displaySettingsMessage(errorMsg);
                }
            });
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

    $scope.realNameVerify = {};

    $scope.doRealNameVerify = function() {
        $http.post('/account/realnameverify', $.param($scope.realNameVerify))
            .success(function (data, status, headers, config) {
                console.debug("realnameverify result: ", data)
                if (data.success) {
                    return $window.location.href = '/account/settingsPage';
                } else {
                    $scope.showRealNameVerifyError = true;
                    var errorMsg = Messages.getMessage(data.code, data.message);
                    $scope.realNameVerifyErrorMessage = errorMsg;
                }
            });
    };

    $('#unbind-googleauth').popover({
        html: true,
        trigger: 'manual'
    }).on('shown.bs.popover', function () {
        var $popup = $(this);
        $(this).next('.popover').find('button.cancel').click(function (e) {
            $popup.popover('hide');
        });
        $(this).next('.popover').find('button.save').click(function (e) {
            var verifyCode = $(".popover #google-verifycode").val();
            $scope.unBindGoogleAuth(verifyCode);
            $popup.popover('hide');
        });
    });

    $scope.unBindGoogleAuth = function (verifycode) {
        $http.post('/googleauth/unbind/',
            $.param({googlecode: verifycode}))
            .success(function (data, status, headers, config) {
            if (data.success) {
                return $window.location.href = '/account/settingsPage';
            }
            else {
                alert(Messages.ErrorMessages['m' + data.code]);
            }
        });
    };

});

app.controller('GoogleAuthCtrl', function ($scope, $http, $location, $window) {

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
                    return $window.location.href = '/account/settingsPage';
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
                return $window.location.href = '/account/settingsPage';
                // $window.location.href = '/account#/accountsettings';
                // $window.location.reload();
            }
            else {
                alert(Messages.ErrorMessages['m' + data.code]);
            }
        });
    };

});
