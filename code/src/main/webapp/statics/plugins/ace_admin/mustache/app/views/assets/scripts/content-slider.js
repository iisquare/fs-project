jQuery(function($) {
	$('.modal.aside').ace_aside();
	
	$('#aside-inside-modal').addClass('aside').ace_aside({container: '#my-modal > .modal-dialog'});
	
	//$('#top-menu').modal('show')
	
	$(document).one('ajaxloadstart.page', function(e) {
		//in ajax mode, remove before leaving page
		$('.modal.aside').remove();
		$(window).off('.aside')
	});
	
	
	//make content sliders resizable using jQuery UI (you should include jquery ui files)
	//$('#right-menu > .modal-dialog').resizable({handles: "w", grid: [ 20, 0 ], minWidth: 200, maxWidth: 600});
})