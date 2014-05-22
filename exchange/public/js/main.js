
function main_ini() {
	function is_touch_device() {
		return !!('ontouchstart' in window) || !!('msmaxtouchpoints' in window.navigator);
	};

	$('#home #main-logo, #home #main-title').hide();
	$('#about .mini-statistics').hide();
	$('#more-info .mini-statistics-2').hide();
	var get_goDown_el = $('#home article .read-on a .goDown');
	get_goDown_el.removeClass('goDown');
	$('#about #what-i-do #expertise-group').removeClass('animation-ready');
	$('#work .hero').css({'opacity' : '0'});

	function list_shuffle_texts() {
		var count_shuffle_texts = 0;
		var current_shuffle_text = new Array();
		$('.shuffle-texts span').each(function(){
			var current_shuffle_element = $(this);
			count_shuffle_texts++;
			current_shuffle_text[count_shuffle_texts] = current_shuffle_element.html();
		});
		$('.shuffle-texts').before('<p class="shuffle-texts-container"></p>');
		$('.shuffle-texts').hide();
		var current_shuffle_no = 0;
		$('.shuffle-texts-container').hide();
		function show_shuffle_texts() {
			current_shuffle_no++;
			if (current_shuffle_no > count_shuffle_texts) current_shuffle_no = 1;
			$('.shuffle-texts-container').fadeIn(500).shuffleLetters({
				'text' : current_shuffle_text[current_shuffle_no]
			});
			setTimeout(function() {
				$('.shuffle-texts-container').fadeOut(500, show_shuffle_texts);
			}, 4000);
		}
		show_shuffle_texts();
	}

	function list_fading_texts() {
		var count_fading_texts = 0;
		var current_fading_text = new Array();
		$('.fading-texts span').each(function(){
			var current_fading_element = $(this);
			count_fading_texts++;
			current_fading_text[count_fading_texts] = current_fading_element.html();
		});
		$('.fading-texts').before('<p class="fading-texts-container"></p>');
		$('.fading-texts').hide();
		var current_fading_no = 0;
		$('.fading-texts-container').css({'opacity' : '0'});
		function show_fading_texts() {
			current_fading_no++;
			if (current_fading_no > count_fading_texts) current_fading_no = 1;
			$('.fading-texts-container').html(current_fading_text[current_fading_no]).animate({opacity: 1.0}, 500);
			setTimeout(function() {
				$('.fading-texts-container').animate({opacity: 0}, 500, show_fading_texts);
			}, 5000);
		}
		show_fading_texts();
	}

	function setup_mini_statistics() {
		if (!$('#about .mini-statistics').is(':visible')) {
			$('#about .mini-statistics .chart').easyPieChart({
				barColor: '#1bd3df',
				trackColor: '#f3f3f3',
				scaleColor: false,
				lineCap: 'butt',
				lineWidth: 2,
				size: 170,
				animate: 5000
			});

			$('#about .mini-statistics .go-count').countTo();

			$('#about .mini-statistics').fadeIn('slow');
		}
	}

	function setup_mini_statistics_2() {
		if (!$('#more-info .mini-statistics-2').is(':visible')) {
			$('#more-info .mini-statistics-2 .chart').easyPieChart({
				barColor: '#1bd3df',
				trackColor: '#494949',
				scaleColor: false,
				lineCap: 'butt',
				lineWidth: 5,
				size: 30,
				animate: 5000
			});

			$('#more-info .mini-statistics-2 .go-count').countTo();

			$('#more-info .mini-statistics-2').fadeIn('slow');
		}
	}

	function update_scroll_spy() {
		$('[data-spy="scroll"]').each(function () {
			var $spy = $(this).scrollspy('refresh')
		});
	}

	function update_expertise_group() {
		if (Modernizr.mq('(max-width: 767px)')) {
			$('#about #what-i-do #expertise-group').animate({"height" : "120px"}, 500, 'easeOutCirc', function() {
				if (!$('#about #what-i-do .expertise').is(':visible')) $('#about #what-i-do .expertise').fadeIn('slow');

				update_scroll_spy();
			});
		} else if (Modernizr.mq('(max-width: 991px)')) {
			$('#about #what-i-do #expertise-group').animate({"height" : "80px"}, 500, 'easeOutCirc', function() {
				if (!$('#about #what-i-do .expertise').is(':visible')) $('#about #what-i-do .expertise').fadeIn('slow');

				update_scroll_spy();
			});
		} else if (Modernizr.mq('(min-width: 992px)')) {
			$('#about #what-i-do #expertise-group').animate({"height" : "40px"}, 500, 'easeOutCirc', function() {
				if (!$('#about #what-i-do .expertise').is(':visible')) $('#about #what-i-do .expertise').fadeIn('slow');

				update_scroll_spy();
			});
		}
	}



$('#map').after('<div id="showwork-background"></div><div id="showwork"><a id="showwork-close" href="#"><i class="fa fa-times-circle"></i></a><div id="showwork-loader"><img src="image/homepage/indicator.gif" alt="One moment please..." class="go-retina" width="24px" height="24px"></div><div id="showwork-content"></div></div>');
$('#showwork-background,#showwork,#showwork-close,#showwork-loader,#showwork-content').hide();



	if (Modernizr.mq('(min-device-width: 768px)')) {
		$('#global-nav').addClass('navbar-fixed-top');
	}
	if (Modernizr.mq('(min-device-width: 992px)')) {
		$('#global-nav').css({'top' : '-' + $('#global-nav').outerHeight() + 'px'});
		$('#home').css({'position' : 'fixed', 'z-index' : '10'});
		$('#main-content').css({'position' : 'absolute', 'top' : '100%', 'z-index' : '30'});
		$('#about #what-i-do .expertise').hide();
		$('#home #main-logo').addClass('goZoom');
		$('#home #main-title').addClass('goZoom-2');
		$('#map').css({'bottom' : '0', 'position' : 'fixed', 'z-index' : '20'}).hide();
		$('#main-content .wrapper').after('<div id="map-placeholder"></div>');

		var show_global_nav = 0;
		function scroll_resize_do() {
			if ($(window).scrollTop() < $(window).height()) $('#home').css({'height' : $(window).height()-($(window).scrollTop()/4)});
			if ($(window).scrollTop() < ($(window).height()-$('#global-nav').outerHeight())) {
				if (show_global_nav == 1) {
					$('#global-nav').stop().animate({top: '-' + $('#global-nav').outerHeight() + 'px'}, 500, 'easeInExpo');
					show_global_nav = 0;
				}
			} else {
				if (show_global_nav == 0) {
					$('#global-nav').stop().animate({top: '0'}, 500, 'easeOutBounce');
					show_global_nav = 1;
				}
			}
		}
		if($(window).scrollTop() >= ($(window).height()-$('#global-nav').outerHeight())) {
			if (show_global_nav == 0) {
				$('#global-nav').stop().animate({top: '0'}, 500, 'easeOutBounce');
				show_global_nav = 1;
			}
		}
		$(window).scroll(function() {
			scroll_resize_do();
		});
		$(window).on('debouncedresize', function( event ) {
			scroll_resize_do();
			update_expertise_group();
		});

		var page_jump = 0;
		$(window).bind('mousewheel', function(event, delta) {
			if ($(window).scrollTop() < $(window).height() && delta < 0) {
				event.preventDefault();
				if (page_jump == 0) {
					page_jump = 1;
					$.scrollTo( '#main-content', 1000, {'axis':'y', easing:'easeInOutExpo', onAfter:function(){
						page_jump = 0;
					}} );
				}
			}
			if ($('#showwork').is(':visible')) event.preventDefault();
		});

		$(document).keydown(function(e){
			if ($(window).scrollTop() < $(window).height() && e.keyCode == 40) {
				e.preventDefault();
				if (page_jump == 0) {
					page_jump = 1;
					$.scrollTo( '#main-content', 1000, {'axis':'y', easing:'easeInOutExpo', onAfter:function(){
						page_jump = 0;
					}} );
				}
			}
		});

		$('#about #mini-statistics-group').one('inview', function(event, isInView, visiblePartX, visiblePartY) {
			var iv_elem_1 = $(this);

			if (iv_elem_1.data('inviewtimer')) {
				clearTimeout(iv_elem_1.data('inviewtimer'));
				iv_elem_1.removeData('inviewtimer');
			}

			if (isInView) {
				iv_elem_1.data('inviewtimer', setTimeout(function() {
					setup_mini_statistics();
				}, 500));
			}
		});

		$('#more-info #mini-statistics-group-2').one('inview', function(event, isInView, visiblePartX, visiblePartY) {
			var iv_elem_4 = $(this);

			if (iv_elem_4.data('inviewtimer')) {
				clearTimeout(iv_elem_4.data('inviewtimer'));
				iv_elem_4.removeData('inviewtimer');
			}

			if (isInView) {
				iv_elem_4.data('inviewtimer', setTimeout(function() {
					setup_mini_statistics_2();
				}, 100));
			}
		});

		$('#about #what-i-do #expertise-group').one('inview', function(event, isInView, visiblePartX, visiblePartY) {
			var iv_elem_2 = $(this);

			if (iv_elem_2.data('inviewtimer')) {
				clearTimeout(iv_elem_2.data('inviewtimer'));
				iv_elem_2.removeData('inviewtimer');
			}

			if (isInView) {
				iv_elem_2.data('inviewtimer', setTimeout(function() {
					if(Modernizr.cssanimations) {
						iv_elem_2.addClass('animation-ready');
						update_expertise_group();
					}
				}, 500));
			}
		});

		$('#contact').bind('inview', function(event, isInView, visiblePartX, visiblePartY) {
			if (isInView) {
				$('#map').show();
			} else {
				$('#map').hide();
			}
		});

		$('#work .showcase li').css({'opacity' : '0'});
		$('#work .showcase li').each(function() {
			var iv_elem_3 = $(this);
			iv_elem_3.one('inview', function(event, isInView, visiblePartX, visiblePartY) {

				if (iv_elem_3.data('inviewtimer')) {
					clearTimeout(iv_elem_3.data('inviewtimer'));
					iv_elem_3.removeData('inviewtimer');
				}

				if (isInView) {
					iv_elem_3.data('inviewtimer', setTimeout(function() {
						iv_elem_3.animate({opacity: 1.0}, 1000);
					}, 100));
				}
			});
		});
	} else {
		if (!$('#global-nav').hasClass('navbar-fixed-top')) {
			$('#home').css({'height' : $(window).height() - $('#global-nav .navbar-header').outerHeight() + 'px'});
			$(window).on('orientationchange', function( event ) {
				$('#home').css({'height' : $(window).height() - $('#global-nav .navbar-header').outerHeight() + 'px'});
			});
		}
		setup_mini_statistics();
		setup_mini_statistics_2();
		$('#map').append('<div class="arrow"></div>');
		if (Modernizr.mq('(max-width: 767px)')) $('#map').css({'height' : '200px'});
	}

	$('#main-nav a').click(function(){
		var thisLinkItem = $(this);
		var goto = thisLinkItem.attr('href');
		if (goto == "http://suj.co") {
			$.scrollTo( 0, 1000, {'axis':'y', easing:'easeInOutExpo'} );
		} else {
			$.scrollTo( goto, 1000, {'axis':'y', easing:'easeInOutExpo'} );
		}
		return false;
	});

	$('#home article .read-on a#read-on-link').click(function(){
		$.scrollTo( '#main-content', 1000, {'axis':'y', easing:'easeInOutExpo'} );
		return false;
	});



	var showwork_status = 0;
	$('#work li a:not(.no-popup)').click(function(){
		showwork_open(this);
		return false;
	});

	function showwork_open(work_content) {
		if (showwork_status == 0) {
			$('#showwork-background,#showwork-loader').show();
			$('#showwork').fadeIn('slow', function() {
				$('<img src="' + work_content + '" alt="Showcase">').load(function () {
					$('#showwork-content').empty().append($(this));
					$('#showwork-loader').fadeOut('slow', function() {
						$('#showwork-close, #showwork-content').fadeIn('slow');
						if (Modernizr.mq('(max-device-width: 767px)')) {
							$(document).on('touchmove',function(e){
								if ($('#showwork-content').is(':visible')) {
									e.preventDefault();
								}
							});

							var scrolling = false;

							$('body').on('touchstart','#showwork-content',function(e) {
								if (!scrolling) {
									scrolling = true;
									if (e.currentTarget.scrollTop === 0) {
										e.currentTarget.scrollTop = 1;
									} else if (e.currentTarget.scrollHeight === e.currentTarget.scrollTop + e.currentTarget.offsetHeight) {
										e.currentTarget.scrollTop -= 1;
									}
									scrolling = false;
								}
							});

							$('body').on('touchmove','#showwork-content',function(e) {
								e.stopPropagation();
							});

							$('#showwork-close, #showwork-background, #showwork').click(function(){
								showwork_close();
								return false;
							});
						} else {
							$('#showwork-content').mCustomScrollbar({
								scrollButtons:{
									enable:false
								}
							});
							$('#showwork-close, #showwork-background, #showwork .mCSB_container').click(function(){
								showwork_close();
								return false;
							});
						}
					showwork_status = 1;
					});
				});
			});
		}
		return false;
	}

	function showwork_close() {
		if (showwork_status == 1) {
			$('#showwork-close, #showwork-content').hide();
			$('#showwork-background, #showwork').fadeOut('slow', function() {
				showwork_status = 0;
			});
		}
		return false;
	}



	if (window.devicePixelRatio > 1) {
		$('.go-retina').each(function(i) {
			var lowres = $(this).attr('src');
			var highres = lowres.replace('.', '@2x.');
			$(this).attr('src', highres);
		});
	}

	$(window).load(function() {
	  list_fading_texts();
		list_shuffle_texts();
		$('.flexslider').flexslider({
			animation: 'slide',
			slideshow: false,
			slideshowSpeed: 3000,
			prevText: '',
			nextText: '',
			start: function(slider) {
				$('#work .hero').one('inview', function(event, isInView, visiblePartX, visiblePartY) {
					if (isInView) {
						slider.play();
						$(this).animate({opacity: 1.0}, 1000);
					}
				});
			}
		});

		setTimeout(function () {
			$('#home #main-logo, #home #main-title').show();
			get_goDown_el.addClass('goDown');
		}, 0);
		$('#global-loader').fadeOut('slow', function() {
			update_scroll_spy();
		});
	});
}

$(document).ready(main_ini);
