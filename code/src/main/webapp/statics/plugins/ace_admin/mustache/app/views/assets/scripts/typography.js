jQuery(function($) {

	window.prettyPrint && prettyPrint();
	$('#id-check-horizontal').removeAttr('checked').on('click', function(){
		$('#dt-list-1').toggleClass('dl-horizontal').prev().html(this.checked ? '&lt;dl class="dl-horizontal"&gt;' : '&lt;dl&gt;');
	});

})