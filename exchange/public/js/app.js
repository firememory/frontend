// Declare app level module which depends on filters, and services
var coinportApp = angular.module('coinport.app', []);
// Filters
coinportApp.filter('orderTypeText', function() {
    return function(input) {
        if(! input) return '';
        var input = input.toLowerCase();
        if(input == 'buy')
            return Messages.buy;
        if(input == 'sell')
            return Messages.sell;
        return Messages.unknown;
    }
});

coinportApp.filter('orderTypeClass', function() {
    return function(input) {
        return input.toLowerCase();
    }
});

coinportApp.filter('orderRoleClass', function() {
    return function(input) {
        return input ? 'sell' : 'buy';
    }
});

coinportApp.filter('txTypeClass', function() {
    return function(input) {
        return input ? 'sell' : 'buy';
    }
});

coinportApp.filter('orderRoleClass', function() {
    return function(input) {
        return input ? 'fa fa-btc' : 'fa fa-cny';
    }
});

coinportApp.filter('txTypeText', function() {
    return function(input) {
        return input ?  Messages.sell : Messages.buy;
    }
});

coinportApp.filter('txTypeIcon', function() {
    return function(input) {
        return input ?  'fa-arrow-right' : 'fa-arrow-left';
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
            return Messages.orderStatus.finished;
        if(input == 0)
            return Messages.orderStatus.pending;
        if(input == 1)
            return Messages.orderStatus.open;
        if(input == 3)
            return Messages.orderStatus.cancelled;
        if(input == 4)
            return Messages.orderStatus.cancelled;
        if(input == 5)
            return Messages.orderStatus.open;
        return Messages.unknown + input;
    }
    return filter;
});

coinportApp.filter('transferStatusText', function() {
    return function(input) {
        return Messages.transfer.status[input];
    };
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

coinportApp.filter('price', function() {
    return function(input) {
        return input ? input.toFixed(4) : '0';
    };
});

coinportApp.filter('coin', function() {
    return function(input) {
        return input ? input.toFixed(4) : '0';
    };
});

coinportApp.filter('UID', function() {
    return function(input) {
        return parseInt(input).toString(35).toUpperCase().replace('-','Z');
    }
});

coinportApp.filter('dwText', function() {
    return function(input) {
        return input == 0 ? Messages.transfer.deposit : Messages.transfer.withdrawal;
    }
});

coinportApp.filter('dwClass', function() {
    return function(input) {
        return input == 0 ? 'green' : 'red';
    }
});

coinportApp.filter('dwIcon', function() {
    return function(input) {
        return input == 0 ? 'fa-sign-out fa-rotate-270' : 'fa-sign-in fa-rotate-90';
    }
});


coinportApp.filter('networkStatusClass', function() {
    return function(input) {
        if (input < 30 * 60 * 1000)
            return 'success';
        else if (input < 60 * 60 * 1000)
            return 'warning';
        else
            return 'danger';
    }
});

coinportApp.filter('networkStatusText', function() {
    return function(input) {
        if (input < 30 * 60 * 1000)
            return Messages.connectivity.status.normal;
        else if (input < 60 * 60 * 1000)
            return Messages.connectivity.status.delayed;
        else
            return Messages.connectivity.status.blocked;
    }
});

// Directives
// nav bar
coinportApp.directive('cpNav', function($window) {
 'use strict';
 return {
   restrict: 'A',
   link: function postLink(scope, element, attrs, controller) {
     // Watch for the $window
     scope.$watch(function() {
       return $window.location.hash;
     }, function(newValue, oldValue) {

       $('li[route]', element).each(function(k, li) {
         var $li = angular.element(li),
           pattern = $li.attr('route'),
           regexp = new RegExp('^' + pattern + '$', ['i']);
         if(regexp.test(newValue)) {
           $li.addClass('active');
         } else {
           $li.removeClass('active');
         }
       });
     });
   }
 };
});

// form validations
//var FLOAT_REGEXP = /^\-?\d+((\.|\,)\d+)?$/;
//coinportApp.directive('cpPrice', function() {
//    return {
//        require : 'ngModel',
//        link : function(scope, elm, attrs, ctrl) {
//            ctrl.$parsers.unshift(function(viewValue) {
//                if (FLOAT_REGEXP.test(viewValue)) {
//                    ctrl.$setValidity('price', true);
//                    return parseFloat(viewValue.replace(',','.'));
//                } else {
//                    ctrl.$setValidity('price', false);
//                    return undefined;
//                }
//            });
//        }
//    };
//});
