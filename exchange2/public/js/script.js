// Popover 

$('[rel="popover"],[data-rel="popover"]').popover({
  trigger: 'click',
  html: true,
  placement: 'top'		
});


$(".qrcode a").popover({ 
 trigger: "click",
 html:true 
 }); 
 
 
 
// Slider	
$(document).ready(function(){
  $('.slider').bxSlider({
    minSlides: 2,
    maxSlides: 3,
    startSlide: 0,
    slideMargin: 10
  });
  $('.slider2').bxSlider({
    startSlide: 0,
  });
});


// Toggle Left Menu
$(document).ready(function(){
	jQuery('.has-submenu > a').click(function() {
		var parent = jQuery(this).parent();
		var sub = parent.find('> ul');
		if (!jQuery('body').hasClass('sidebar')) {
			if (sub.is(':visible')) {
				sub.slideUp(200,
				function() {
					parent.removeClass('active');
					jQuery('#container').css({
						height: ''
					});
					adjustmainpanelheight();
				});
			} else {
				closeVisibleSubMenu();
				parent.addClass('active');
				sub.slideDown(200,
				function() {
					adjustmainpanelheight();
				});
			}
		}
		return false;
	});
});

function closeVisibleSubMenu() {
	jQuery('.has-submenu').each(function() {
		var t = jQuery(this);
		if (t.hasClass('active')) {
			t.find('> ul').slideUp(200,
			function() {
				t.removeClass('active');
			});
		}
	});
}

function adjustmainpanelheight() {
	var docHeight = jQuery(document).height();
	if (docHeight > jQuery('#main-container').height()) jQuery('#main-container').height(docHeight);
}