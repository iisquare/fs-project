jQuery(function($) {
   $('#sidebar2').insertBefore('.page-content');
   
   $('.navbar-toggle[data-target="#sidebar2"]').insertAfter('#menu-toggler');
   
   
   $(document).on('settings.ace.two_menu', function(e, event_name, event_val) {
	 if(event_name == 'sidebar_fixed') {
		 if( $('#sidebar').hasClass('sidebar-fixed') ) {
			$('#sidebar2').addClass('sidebar-fixed');
			$('#navbar').addClass('h-navbar');
		 }
		 else {
			$('#sidebar2').removeClass('sidebar-fixed')
			$('#navbar').removeClass('h-navbar');
		 }
	 }
   }).triggerHandler('settings.ace.two_menu', ['sidebar_fixed' ,$('#sidebar').hasClass('sidebar-fixed')]);
})