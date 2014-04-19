// Declare app level module which depends on filters, and services
var coinportApp = angular.module('coinport.app', []);
coinportApp.filter('orderTypeText', function() {
    return function(input) {
        if(! input) return '';
        var input = input.toLowerCase();
        if(input == 'buy')
            return '买入';
        if(input == 'sell')
            return '卖出';
        return '未知';
    }
});

coinportApp.filter('orderTypeClass', function() {
    return function(input) {
        return input.toLowerCase();
    }
});

coinportApp.filter('txTypeClass', function() {
    return function(input) {
        return input ? 'sell' : 'buy';
    }
});

coinportApp.filter('txTypeText', function() {
    return function(input) {
        return input ?  '卖出' : '买入';
    }
});

coinportApp.filter('txTypeIcon', function() {
    return function(input) {
        return input ?  'fa-angle-double-right' : 'fa-angle-double-left';
    }
});

coinportApp.filter('orderStatusClass', function() {
    var filter = function(input) {
        if(input == 2)
            return 'success';
        if(input == 0)
            return 'info';
        if(input == 1)
            return 'warning';
        if(input == 3)
            return 'danger';
        return '';
    }
    return filter;
});

coinportApp.filter('orderStatusText', function() {
    var filter = function(input) {
        if(input == 2)
            return '全部成交';
        if(input == 0)
            return '正在挂单';
        if(input == 1)
            return '部分成交';
        if(input == 3)
            return '已经撤销';
        if(input == 4)
            return '未能成交';
        if(input == 5)
            return '部分成交';
        if(input == -1)
            return '等待处理';
        return '未知状态:'+input;
    }
    return filter;
});

coinportApp.filter('depositStatusText', function() {
    var filter = function(input) {
        if(input == 0)
            return '等待处理';
        if(input == 1)
            return '充值成功';
        if(input == 2)
            return '充值失败';
        return '未知状态:'+input;
    }
    return filter;
});

coinportApp.filter('withdrawalStatusText', function() {
    var filter = function(input) {
        if(input == 0)
            return '等待处理';
        if(input == 1)
            return '提现成功';
        if(input == 2)
            return '提现失败';
        return '未知状态:'+input;
    }
    return filter;
});

coinportApp.filter('currency', function() {
    var filter = function(input) {
        return input ? input.toFixed(2) : '0';
    }
    return filter;
});

coinportApp.filter('quantity', function() {
    var filter = function(input) {
        return input ? input.toFixed(3) : '0';
    }
    return filter;
});

coinportApp.filter('UID', function() {
    return function(input) {
        return parseInt(input).toString(35).toUpperCase().replace('-','Z');
    }
});

coinportApp.filter('dwText', function() {
    return function(input) {
        return input == 0 ? '充值' : '提现';
    }
});

coinportApp.filter('dwClass', function() {
    return function(input) {
        return input == 0 ? 'green' : 'red';
    }
});

coinportApp.filter('dwIcon', function() {
    return function(input) {
        return input == 0 ? 'fa-sign-in' : 'fa-sign-out';
    }
});