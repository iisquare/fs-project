jQuery(function($) {
	$('#id-change-style').on(ace.click_event, function() {
		var toggler = $('#menu-toggler');
		var fixed = toggler.hasClass('fixed');
		var display = toggler.hasClass('display');
		
		if(toggler.closest('.navbar').length == 1) {
			$('#menu-toggler').remove();
			toggler = $('#sidebar').before('<a id="menu-toggler" data-target="#sidebar" class="menu-toggler" href="#">\
				<span class="sr-only">Toggle sidebar</span>\
				<span class="toggler-text"></span>\
			 </a>').prev();

			 var ace_sidebar = $('#sidebar').ace_sidebar('ref');
			 ace_sidebar.set('mobile_style', 2);

			 var icon = $(this).children().detach();
			 $(this).text('Hide older Ace toggle button').prepend(icon);
			 
			 $('#id-push-content').closest('div').hide();
			 $('#id-push-content').removeAttr('checked');
			 $('.sidebar').removeClass('push_away');
		 } else {
			$('#menu-toggler').remove();
			toggler = $('.navbar-brand').before('<button data-target="#sidebar" id="menu-toggler" class="three-bars pull-left menu-toggler navbar-toggle" type="button">\
				<span class="sr-only">Toggle sidebar</span><span class="icon-bar"></span><span class="icon-bar"></span><span class="icon-bar"></span>\
			</button>').prev();
			
			 var ace_sidebar = $('#sidebar').ace_sidebar('ref');
			 ace_sidebar.set('mobile_style', 1);
			
			var icon = $(this).children().detach();
			$(this).text('Show older Ace toggle button').prepend(icon);
			
			$('#id-push-content').closest('div').show();
		 }

		 if(fixed) toggler.addClass('fixed');
		 if(display) toggler.addClass('display');
		 
		 $('.sidebar[data-sidebar-hover=true]').ace_sidebar_hover('reset');
		 $('.sidebar[data-sidebar-scroll=true]').ace_sidebar_scroll('reset');

		 return false;
	});
	
	$('#id-push-content').removeAttr('checked').on('click', function() {
		$('.sidebar').toggleClass('push_away');
	});
});