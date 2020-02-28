jQuery(function($){

	$('.dd').nestable();

	$('.dd-handle a').on('mousedown', function(e){
		e.stopPropagation();
	});
	
	$('[data-rel="tooltip"]').tooltip();

});