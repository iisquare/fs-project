jQuery(function($) {
	$('[data-toggle="buttons"] .btn').on('click', function(e){
		var target = $(this).find('input[type=radio]');
		var which = parseInt(target.val());
		$('[id*="timeline-"]').addClass('hide');
		$('#timeline-'+which).removeClass('hide');
	});
});
