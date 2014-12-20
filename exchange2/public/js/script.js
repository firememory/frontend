

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
    startSlide: 0,
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

// Switch
var elems = Array.prototype.slice.call(document.querySelectorAll('.js-switch'));
elems.forEach(function(html) {
  var switchery = new Switchery(html);
});
defaults = {
    color :'#8bc34a',
    secondaryColor : '#dfdfdf',
    jackColor: '#fff',
    className: 'switchery',
    disabled: false,
    disabledOpacity: 0.5,
    speed: '0.1s'
}
