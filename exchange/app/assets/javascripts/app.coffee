coinportApp = angular.module('coinport.app', [])

coinportApp.filter 'orderTypeText', () -> (input) ->
	return '' if !input
	input = input.toLowerCase()
	return Messages.buy if input == 'buy'
	return Messages.sell if input == 'sell'
	return Messages.unknown

coinportApp.filter 'orderTypeClass', () -> (input) ->
	input.toLowerCase()

coinportApp.filter 'orderRoleClass', () -> (input) ->
	if input then 'sell' else 'buy'

coinportApp.filter 'txTypeClass', () -> (input) ->
	if input then 'sell' else 'buy'

# TODO(chunming): what if for other currency?
coinportApp.filter 'orderRoleClass', () -> (input) ->
	if input then 'fa fa-btc' else 'fa fa-cny'

coinportApp.filter 'txTypeText', () -> (input) ->
	if input then  Messages.sell else Messages.buy

coinportApp.filter 'txTypeIcon', () -> (input) ->
	if input then  'fa-arrow-right' else 'fa-arrow-left'

coinportApp.filter 'orderStatusClass', () -> (input) ->
	switch(input)
		when 0 then 'info'
		when 1 then 'warning'
		when 2 then 'success'
		when 3 then 'danger'
		else ''

coinportApp.filter 'orderStatusText', () -> (input) ->
	switch input
		when 0 then Messages.orderStatus.pending
		when 1 then Messages.orderStatus.open
		when 2 then Messages.orderStatus.finished
		when 3 then Messages.orderStatus.cancelled
		when 4 then Messages.orderStatus.cancelled
		when 5 then Messages.orderStatus.open
		else Messages.unknown + input

coinportApp.filter 'transferStatusText', () -> (input) ->
	Messages.transfer.status[input]

coinportApp.filter 'currency', () -> (input) ->
	if input then input.toFixed(2) else '0'

coinportApp.filter 'quantity', () -> (input) ->
	if input then input.toFixed(3) else '0'

coinportApp.filter 'price', () -> (input) ->
	if input
	    s = input.toPrecision(8)
	    `for (i = s.length; i >= 0 && s.charAt(i - 1) == '0'; i--)`
	    return s.slice(0, i + 1)
	else
	    return '0'

coinportApp.filter 'coin', () -> (input) ->
	if input then input.toFixed(4) else '0'

coinportApp.filter 'UID', () -> (input) ->
	parseInt(input).toString(35).toUpperCase().replace('-','Z')

coinportApp.filter 'dwText', () -> (input) ->
	if input == 0 then Messages.transfer.deposit else Messages.transfer.withdrawal

coinportApp.filter 'dwClass', () -> (input) ->
	if input == 0 then 'green' else 'red'

coinportApp.filter 'dwIcon', () -> (input) ->
	if input == 0 then 'fa-sign-out fa-rotate-270' else 'fa-sign-in fa-rotate-90'


coinportApp.filter 'networkStatusClass', () -> (input) ->
	return 'success' if (input < 30 * 60 * 1000)
	return 'warning' if (input < 60 * 60 * 1000)
	return 'danger'

coinportApp.filter 'networkStatusText', () -> (input) ->
	return Messages.connectivity.status.normal if (input < 30 * 60 * 1000)
	return Messages.connectivity.status.delayed if (input < 60 * 60 * 1000)
	return Messages.connectivity.status.blocked

# NavBar
coinportApp.directive 'cpNav', ($window) ->
	'use strict'

	restrict: 'A',
	link: (scope, element, attrs, controller) ->
		# Watch for the $window
		scope.$watch () ->
			$window.location.hash
		, (newValue, oldValue) ->
			$('li[route]', element).each (k, li) ->
				$li = angular.element(li)
				pattern = $li.attr('route')
				regexp = new RegExp('^' + pattern + '$', ['i'])

				if regexp.test(newValue)
					$li.addClass('active')
				else
					$li.removeClass('active')

