jQuery(function($) {
	/**
	$('#myTab a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
	  //console.log(e.target.getAttribute("href"));
	})
		
	$('#accordion').on('shown.bs.collapse', function (e) {
		//console.log($(e.target).is('#collapseTwo'))
	});
	*/
	
	$('#myTab a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
		//if($(e.target).attr('href') == "#home") doSomethingNow();
	})

	
	/**
		//go to next tab, without user clicking
		$('#myTab > .active').next().find('> a').trigger('click');
	*/


	$('#accordion-style').on('click', function(ev){
		var target = $('input', ev.target);
		var which = parseInt(target.val());
		if(which == 2) $('#accordion').addClass('accordion-style2');
		 else $('#accordion').removeClass('accordion-style2');
	});
	
	//$('[href="#collapseTwo"]').trigger('click');


	$('.easy-pie-chart.percentage').each(function(){
		$(this).easyPieChart({
			barColor: $(this).data('color'),
			trackColor: '#EEEEEE',
			scaleColor: false,
			lineCap: 'butt',
			lineWidth: 8,
			animate: ace.vars['old_ie'] ? false : 1000,
			size:75
		}).css('color', $(this).data('color'));
	});

	$('[data-rel=tooltip]').tooltip();
	$('[data-rel=popover]').popover({html:true});


	$('#gritter-regular').on(ace.click_event, function(){
		$.gritter.add({
			title: 'This is a regular notice!',
			text: 'This will fade out after a certain amount of time. Vivamus eget tincidunt velit. Cum sociis natoque penatibus et <a href="#" class="blue">magnis dis parturient</a> montes, nascetur ridiculus mus.',
			image: '{{{path.assets}}}/avatars/avatar1.png', //in Ace demo {{{path.assets}}} will be replaced by correct assets path
			sticky: false,
			time: '',
			class_name: (!$('#gritter-light').get(0).checked ? 'gritter-light' : '')
		});

		return false;
	});

	$('#gritter-sticky').on(ace.click_event, function(){
		var unique_id = $.gritter.add({
			title: 'This is a sticky notice!',
			text: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus eget tincidunt velit. Cum sociis natoque penatibus et <a href="#" class="red">magnis dis parturient</a> montes, nascetur ridiculus mus.',
			image: '{{{path.assets}}}/avatars/avatar.png',
			sticky: true,
			time: '',
			class_name: 'gritter-info' + (!$('#gritter-light').get(0).checked ? ' gritter-light' : '')
		});

		return false;
	});


	$('#gritter-without-image').on(ace.click_event, function(){
		$.gritter.add({
			// (string | mandatory) the heading of the notification
			title: 'This is a notice without an image!',
			// (string | mandatory) the text inside the notification
			text: 'This will fade out after a certain amount of time. Vivamus eget tincidunt velit. Cum sociis natoque penatibus et <a href="#" class="orange">magnis dis parturient</a> montes, nascetur ridiculus mus.',
			class_name: 'gritter-success' + (!$('#gritter-light').get(0).checked ? ' gritter-light' : '')
		});

		return false;
	});


	$('#gritter-max3').on(ace.click_event, function(){
		$.gritter.add({
			title: 'This is a notice with a max of 3 on screen at one time!',
			text: 'This will fade out after a certain amount of time. Vivamus eget tincidunt velit. Cum sociis natoque penatibus et <a href="#" class="green">magnis dis parturient</a> montes, nascetur ridiculus mus.',
			image: '{{{path.assets}}}/avatars/avatar3.png', //in Ace demo {{{path.assets}}} will be replaced by correct assets path
			sticky: false,
			before_open: function(){
				if($('.gritter-item-wrapper').length >= 3)
				{
					return false;
				}
			},
			class_name: 'gritter-warning' + (!$('#gritter-light').get(0).checked ? ' gritter-light' : '')
		});

		return false;
	});


	$('#gritter-center').on(ace.click_event, function(){
		$.gritter.add({
			title: 'This is a centered notification',
			text: 'Just add a "gritter-center" class_name to your $.gritter.add or globally to $.gritter.options.class_name',
			class_name: 'gritter-info gritter-center' + (!$('#gritter-light').get(0).checked ? ' gritter-light' : '')
		});

		return false;
	});
	
	$('#gritter-error').on(ace.click_event, function(){
		$.gritter.add({
			title: 'This is a warning notification',
			text: 'Just add a "gritter-light" class_name to your $.gritter.add or globally to $.gritter.options.class_name',
			class_name: 'gritter-error' + (!$('#gritter-light').get(0).checked ? ' gritter-light' : '')
		});

		return false;
	});
		

	$("#gritter-remove").on(ace.click_event, function(){
		$.gritter.removeAll();
		return false;
	});
		

	///////


	$("#bootbox-regular").on(ace.click_event, function() {
		bootbox.prompt("What is your name?", function(result) {
			if (result === null) {
				
			} else {
				
			}
		});
	});
		
	$("#bootbox-confirm").on(ace.click_event, function() {
		bootbox.confirm("Are you sure?", function(result) {
			if(result) {
				//
			}
		});
	});
	
/**
	$("#bootbox-confirm").on(ace.click_event, function() {
		bootbox.confirm({
			message: "Are you sure?",
			buttons: {
			  confirm: {
				 label: "OK",
				 className: "btn-primary btn-sm",
			  },
			  cancel: {
				 label: "Cancel",
				 className: "btn-sm",
			  }
			},
			callback: function(result) {
				if(result) alert(1)
			}
		  }
		);
	});
**/
	

	$("#bootbox-options").on(ace.click_event, function() {
		bootbox.dialog({
			message: "<span class='bigger-110'>I am a custom dialog with smaller buttons</span>",
			buttons:
			{
				"success" :
				 {
					"label" : "<i class='ace-icon fa fa-check'></i> Success!",
					"className" : "btn-sm btn-success",
					"callback": function() {
						//Example.show("great success");
					}
				},
				"danger" :
				{
					"label" : "Danger!",
					"className" : "btn-sm btn-danger",
					"callback": function() {
						//Example.show("uh oh, look out!");
					}
				}, 
				"click" :
				{
					"label" : "Click ME!",
					"className" : "btn-sm btn-primary",
					"callback": function() {
						//Example.show("Primary button");
					}
				}, 
				"button" :
				{
					"label" : "Just a button...",
					"className" : "btn-sm"
				}
			}
		});
	});



	$('#spinner-opts small').css({display:'inline-block', width:'60px'})

	var slide_styles = ['', 'green','red','purple','orange', 'dark'];
	var ii = 0;
	$("#spinner-opts input[type=text]").each(function() {
		var $this = $(this);
		$this.hide().after('<span />');
		$this.next().addClass('ui-slider-small').
		addClass("inline ui-slider-"+slide_styles[ii++ % slide_styles.length]).
		css('width','125px').slider({
			value:parseInt($this.val()),
			range: "min",
			animate:true,
			min: parseInt($this.attr('data-min')),
			max: parseInt($this.attr('data-max')),
			step: parseFloat($this.attr('data-step')) || 1,
			slide: function( event, ui ) {
				$this.val(ui.value);
				spinner_update();
			}
		});
	});



	//CSS3 spinner
	$.fn.spin = function(opts) {
		this.each(function() {
		  var $this = $(this),
			  data = $this.data();

		  if (data.spinner) {
			data.spinner.stop();
			delete data.spinner;
		  }
		  if (opts !== false) {
			data.spinner = new Spinner($.extend({color: $this.css('color')}, opts)).spin(this);
		  }
		});
		return this;
	};

	function spinner_update() {
		var opts = {};
		$('#spinner-opts input[type=text]').each(function() {
			opts[this.name] = parseFloat(this.value);
		});
		opts['left'] = 'auto';
		$('#spinner-preview').spin(opts);
	}



	$('#id-pills-stacked').removeAttr('checked').on('click', function(){
		$('.nav-pills').toggleClass('nav-stacked');
	});

	
	
	
	
	
	///////////
	$(document).one('ajaxloadstart.page', function(e) {
		$.gritter.removeAll();
		$('.modal').modal('hide');
	});

});