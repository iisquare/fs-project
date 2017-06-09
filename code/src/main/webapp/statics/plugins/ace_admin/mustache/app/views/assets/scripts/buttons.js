jQuery(function($) {
	$('#loading-btn').on(ace.click_event, function () {
		var btn = $(this);
		btn.button('loading')
		setTimeout(function () {
			btn.button('reset')
		}, 2000)
	});

	$('#id-button-borders').attr('checked' , 'checked').on('click', function(){
		$('#default-buttons .btn').toggleClass('no-border');
	});
})