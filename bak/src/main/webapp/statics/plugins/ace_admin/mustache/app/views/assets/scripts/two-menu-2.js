jQuery(function($) {
   $('#sidebar2').insertBefore('.page-content');
   $('#navbar').addClass('h-navbar');
   $('.footer').insertAfter('.page-content');
   
   $('.page-content').addClass('main-content');
   
   $('.menu-toggler[data-target="#sidebar2"]').insertBefore('.navbar-brand');
   
   
   $(document).on('settings.ace.two_menu', function(e, event_name, event_val) {
	 if(event_name == 'sidebar_fixed') {
		 if( $('#sidebar').hasClass('sidebar-fixed') ) $('#sidebar2').addClass('sidebar-fixed')
		 else $('#sidebar2').removeClass('sidebar-fixed')
	 }
   }).triggerHandler('settings.ace.two_menu', ['sidebar_fixed' ,$('#sidebar').hasClass('sidebar-fixed')]);
   
   $('#sidebar2[data-sidebar-hover=true]').ace_sidebar_hover('reset');
   $('#sidebar2[data-sidebar-scroll=true]').ace_sidebar_scroll('reset', true);
})