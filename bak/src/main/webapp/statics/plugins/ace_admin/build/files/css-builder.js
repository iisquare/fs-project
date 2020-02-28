jQuery(function($) {
 var winURL = (window.URL || window.webkitURL);
 var objectURLs = {};
 var saved_results = {};
 var currentLessVars = {};
 

 $.ajax({url: "../assets/css/less/ace-features.less"}).done(function(less_file_data) {
	var sections = []
	var regex = /\/\/~([\w\- \&]+)/gi;
	var matches;
	while ((matches = regex.exec(less_file_data)) !== null)
	{
		sections.push({'title' : matches[1] , 'index': matches.index})
	}
	sections.push({'title' : 'EOF' , 'index': less_file_data.length})
	
			
	var depend_on = {}
	var depend_off = {}
	
	$(sections).each(function(i, info) {
		if(info.title == 'EOF') return;
		
		var next_info = sections[i + 1];
		var sub = less_file_data.substring(info.index + info.title.length + 4 , next_info.index - 1);
		sub = $.trim(sub);
		
		if(sub.length == 0) return;

		var vars = [];
		var regex = /@([\w\-]+)\:\s+([\w\d\'\"\@\-]+);(?:\/\/(.*))?/gi;
		var matches;
		while ((matches = regex.exec(sub)) !== null) {
			vars.push({'name': matches[1], 'value': matches[2], 'description': matches[3]})
		}

		var section_name = info.title.toLowerCase().replace(/\W/g, '');
		$('<hr /><h4 class="text-primary"><div class="checkbox"><label><input checked type="checkbox" class="section" name="'+section_name+'" /> '+info.title+'</label></div></h4>').appendTo('#page-content');
		var group = $('<div class="list-group" />').appendTo('#page-content');

		
		$(vars).each(function(i, lessvar) {
			//lessvar.name
			//lessvar.value
			//lessvar.description
			var description = lessvar.description || '';
			if(description == '!ignore') return;
			
			var label = $('<label />').appendTo(group);
			label.wrap('<div class="checkbox list-group-item clearfix" />');
				
			if(lessvar.value.match(/true|false/i)) {
				var check = $('<input type="checkbox" name="less-var" data-section="'+section_name+'" />').prependTo(label);
				check.attr({'id' : 'id-'+lessvar.name , 'data-varname' : lessvar.name})
				check.get(0).checked = lessvar.value == 'true';
				if(lessvar.value == 'true') label.parent().addClass('active');
			}
			else {
				var opts = description.match(/\{(?:[^\}]+)\}/);
				if(opts) {
					description = description.replace(/\{(?:[^\}]+)\}/ , '');
					
					try {
						var select = $(' <select class="pull-right form-control"  style="width: auto; " name="less-var" data-section="'+section_name+'" /> ').appendTo(label);
						select.attr({'id' : 'id-'+lessvar.name , 'data-varname' : lessvar.name})
						opts = JSON.parse(opts[0]);
						for(var o in opts) if(opts.hasOwnProperty(o)) {
							var opt  = $('<option />').appendTo(select);
							opt.attr('value', o).text(opts[o]);
						}
						select.val(lessvar.value);
					}
					catch(e) {}
				}
				
				else {
					var input = $(' <input type="text" class="pull-right form-control"  style="width: auto; " name="less-var" data-section="'+section_name+'" /> ').appendTo(label);
					input.attr({'id' : 'id-'+lessvar.name , 'data-varname' : lessvar.name, value: lessvar.value})
				}
			}

			
			var match
			if(description.length > 0 && (match = description.match(/\+@enable\-([\w\-]+)/))) {
				var depends = match[1];
				
				//check if a variable (feature) depends on another one
				//we should automatically enable that if so
				if(!(lessvar.name in depend_on)) depend_on[lessvar.name] = []
				depend_on[lessvar.name].push('enable-'+depends);//enable "'enable-'+depends" when lessvar.name is enabled
				
				if(!('enable-'+depends in depend_off)) depend_off['enable-'+depends] = []
				depend_off['enable-'+depends].push(lessvar.name);//disable "lessvar.name" when "'enable-'+depends" is disabled
				//for example auto-disable switch elements when checkboxes are disabled/unselected
				//or auto-enable checkboxes when switch elements are enabled/selected

				description = description.replace(match[0], '');
			}
			
			label.append(description);
		});	
	});


	$(document).on('click', 'input[type=checkbox][name=less-var]', function(e){
		$(this).closest('.list-group-item').toggleClass('active');
		
		var varname = $(this).data('varname');
		if(this.checked && varname in depend_on) {
			//we need another part to be enabled as well
			enable_depends(depend_on[varname]);				
		}		
		else if(!this.checked && varname in depend_off) {
			//we need to disable dependent parts as well
			//but we should do it recursively as `depend_off[varname]` is an array
			disable_depends(depend_off[varname]);
		}
	});
	
	
	$(document).on('click', 'input[type=checkbox].section', function(){
		var name = this.name;
		var checked = this.checked;
		$('input[type=checkbox][name=less-var][data-section="'+name+'"]').each(function() {
			this.checked = checked;
			if(checked) $(this).closest('.list-group-item').addClass('active');
			else $(this).closest('.list-group-item').removeClass('active');
		});
	});
	
	
	//for example when we disable "checkboxes" , "switch elements" are disabled automatically
	//and switch styles 4,5,6,7 are also dependent on "switch elements" so they get disabled as well
	function disable_depends(depend_offs) {
		$(depend_offs).each(function(i, varname) {
			$('#id-'+varname).get(0).checked = false;
			$('#id-'+varname).closest('.list-group-item').removeClass('active');
			if(varname in depend_off) {
				//if it contains another list of dependents
				disable_depends(depend_off[varname]);
			}
		});
	}
	
	function enable_depends(depend_ons) {
		$(depend_ons).each(function(i, varname) {
			$('#id-'+varname).get(0).checked = true;
			$('#id-'+varname).closest('.list-group-item').addClass('active');
			if(varname in depend_on) {
				//if it contains another list of dependents
				enable_depends(depend_on[varname]);
			}
		});
	}


});



function showModal() {
	$('#btn-build-css').button('reset').removeClass('disabled');
	$('#btn-launch-modal').removeClass('hide');
	$('#modal-save-dialog').modal('show');
}


function renderLess(options) {
	var updateVars = options.less_vars;
	releaseUrlObjects(options.name);

	function showResult() {
		if(options.download === true) {
			$('#btn-save-css').get(0).click();
			return;
		}
		else if(typeof options.callback === 'function' && objectURLs[options.name] != null) {
			options.callback.call(null);
			return;
		}
	}
	

	setTimeout(function() {
		//less_parser.parse(options.less_input, process_output, {modifyVars: updateVars});
		less.render(options.less_input, {modifyVars: updateVars}, process_output);
	} , 100);

	function process_output (e, tree) {
		if(e) {
			console.log(e);
			return;
		}
		
	
		var css_output = tree.css;
		////////
		var is_responsive = true;
		if(updateVars['enable-no-responsive'] === 'true') {
			try {
				css_output = remove_media_queries(css_output, grid_break == 991 ? 900 : 700);
				is_responsive = false;
				//keep `min-width` media queries which are >= 900px
			} catch(e){}
		}
		
		
		
		////////

		$('#save-buttons .output-size').remove();
		saveObjectUrl(options.name, css_output);

		
		if(options.ie_fix !== true) {
			showResult();
			return;
		}
		
		$('#modal-ie-alert').addClass('hide');
		
		
		//Here we try to count the output CSS's selector count for IE's limitations
		//I'm not familiar with inner workings of LESS parser
		//so to get a tree of the output CSS, let's parse it again!
		//It may not be the correct approach though!
		breakCSS(less, css_output, function(css_part2){
			if(typeof css_part2 == 'string' && css_part2.length > 10) {
				$('#id-selector-limit').hide()//.html('Maximum CSS selectors allowed: <b class="text-info">4096</b>, Current CSS selectors: <b class="text-warning">' + num + '</b>');
				$('.ie-alert-min').text('');
				
				$('#modal-ie-alert').removeClass('hide');
				
				saveObjectUrl(options.name+'-part2', css_part2);
			}
			showResult();
		});
	}
}


function updateGridVars(updateVars) {
	//grid breakpoint
	var grid_break = parseInt($('select[name=less-var][data-varname="enable-mobile-width"]').val()) || 991;
	updateVars['grid-float-breakpoint'] = (grid_break + 1)+'px';
	updateVars['grid-float-breakpoint-max'] = (grid_break)+'px';

	//gutter width
	updateVars['grid-gutter-width'] = (parseInt($('select[name=less-var][data-varname="enable-gutter-width"]').val()) || 24)+'px';
}
 
 ////
$('#btn-build-css').on('click', function() {
	$('#btn-build-css').button('loading').text('Please wait...').addClass('disabled');

	$('a[id*="btn-save-"]').parent().addClass('hide disabled').find('.filesize').remove();
	$('a[data-id*="btn-save-"]').parent().addClass('hide disabled').find('.filesize').remove();

	var updateVars = {}
	$('input[type=checkbox][name=less-var]').each(function() {
		var varname = $(this).attr('data-varname');
		updateVars[varname] = this.checked ? 'true' : 'false';
	});
	
	if(updateVars['enable-checkbox-asp'] === 'true') {
		updateVars['lbl_selector'] = '~ .lbl';//asp.net friendly selector
	}

	//selected skin
	var skin_less = '';	
	var skin = $('select[name=less-var][data-varname="enable-selected-skin"]').val();
	
	updateVars['selected-skin-1'] = '~"skins/empty.less"';
	if(skin == 'skin-3') {
		//skin-3 requires no-skin as well
		updateVars['selected-skin-1'] = '~"skins/no-skin.less"';
	}
	else {
		updateVars['selected-skin-2'] = '~"skins/'+skin+'.less"';
	}


	updateGridVars(updateVars);
	
	currentLessVars = updateVars;


	var ace_less = 
	'@selected-skin-1: "skins/empty.less";\
	 @selected-skin-2: "skins/no-skin.less";\
	 @import "../assets/css/less/ace.less";';
	
	
	//compile Main file
	renderLess({less_input: ace_less, name: 'ace', less_vars: updateVars, ie_fix: true, callback: function() {
	  
	  
	  var gridVars = {};
	  updateGridVars(gridVars);
	  //compile Bootstrap
	  renderLess({less_input: ' @import "../assets/css/less/bootstrap/bootstrap.less"; ', name: 'bootstrap', less_vars: gridVars, callback: function() {
		
		if(updateVars['enable-skin-file'] === 'true') {
		  renderLess({less_input: ' @import "../assets/css/less/skins/skins.less"; ', name: 'ace-skins', less_vars: updateVars, callback: function() {
			
			if(updateVars['enable-rtl-file'] === 'true') {
			  renderRTL({less_input: ' @import "../assets/css/less/ace-rtl.less"; ', name: 'ace-rtl', less_vars: updateVars}, function() {
				showModal()
			  })
			}//compile RTL file
			else {
				showModal()
			}
			
		  }})
		}//compile skin file
		
		else {
			if(updateVars['enable-rtl-file'] === 'true') {
			  renderRTL({less_input: ' @import "../assets/css/less/ace-rtl.less"; ', name: 'ace-rtl', less_vars: updateVars}, function() {
				showModal()
			  })
			}//compile RTL file
			else {
				showModal()
			}
		}

	  }})


	}})//renderLess

 })




 function renderRTL(options, callback) {

	renderLess({less_input: ' @import "../assets/css/less/ace-rtl.less"; ', name: 'ace-rtl', less_vars: options.less_vars,  callback: function() {
		var content	= saved_results['bootstrap'] || '';
		content += saved_results['ace'] || '';

		if(currentLessVars['enable-skin-file'] === 'true') 
				content += saved_results['ace-skins'] || '';

				

		var rtl_output = makeRTL(content);
		
		var grid_break = parseInt($('select[name=less-var][data-varname="enable-mobile-width"]').val()) || 991;
		if(currentLessVars['enable-no-responsive'] === 'true') rtl_output = remove_media_queries(rtl_output, grid_break == 991 ? 900 : 700);//keep `min-width` media queries which are >= 900px

		rtl_output = saved_results['ace-rtl'] = rtl_output + "\n" + saved_results['ace-rtl'];
		saveObjectUrl('ace-rtl', rtl_output);
		callback.call(null);

	  }//function
	})//renderLess
 }//renderRTL



 $(window).on('unload' , function() {
	releaseUrlObjects();
	saved_result = {}
 });

 function saveObjectUrl(name, css_output) {
	releaseUrlObjects(name);

	objectURLs[name] = css_output.length == 0 ? 'javascript:void(0)' : winURL.createObjectURL(new Blob([css_output], {type : 'text/css'}))
	saved_results[name] =  css_output;

	$('#btn-save-'+name).parent().removeClass('disabled hide')

	$('#btn-save-'+name)
	.attr({'download' : name+'.css'})
	.find('.filesize').remove().end()
	.append('<span class="filesize"> ('+readableSize(css_output.length)+')</span>')
	.get(0).href = objectURLs[name];
	
	
	
	releaseUrlObjects(name+'.min');
	
	var minimized = new CleanCSS().minify(css_output).styles;
	objectURLs[name+'.min'] = minimized.length == 0 ? 'javascript:void(0)' : winURL.createObjectURL(new Blob([minimized], {type : 'text/css'}))

	$('a[data-id="btn-save-'+name+'"]').parent().removeClass('disabled hide')
	$('a[data-id="btn-save-'+name+'"]')
	.attr({'download' : name+'.min.css'})
	.find('.filesize').remove().end()
	.append('<span class="filesize"> ('+readableSize(minimized.length)+')</span>')
	.get(0).href = objectURLs[name+'.min'];
 }
 
 function releaseUrlObjects(name) {
	for(var o in objectURLs) if(objectURLs.hasOwnProperty(o)) {
		if(name && o !== name) continue;

		if(objectURLs[o] !== null && typeof objectURLs[o] === "object") winURL.revokeObjectURL(objectURLs[o]);
		objectURLs[o] = null;
		
		var o2 = o+'-part2';
		if(objectURLs[o2] !== null && typeof objectURLs[o2] === "object") winURL.revokeObjectURL(objectURLs[o2]);
	}
 }

 function readableSize(size) {
	console.log(size);
	var val = size, index = 0;
	while(val > 1024) {
	 val = val / 1024;
	 index++;
	}
	var format = ['bytes', 'KB', 'MB', 'GB'];
	val = val.toFixed(1).replace(/(\.0+)$/g, '');
	//if(parseInt(val) > 100) val = parseInt(Math.round(val));
	return val + ' ' + format[index];
 }


 
 $('#btn-save-selection')
 .off('click')
 .on('click', function() {
	save_selection();
 });


 var storage_name = 'ace.css-selection';
 function save_selection() {
	var selection = {}
	$('input[type=checkbox][name=less-var]').each(function() {
		var name = $(this).attr('data-varname').replace(/^enable\-/, '');
		selection[name] = this.checked ? 1 : 0;
	});
	$('input[type=text][name=less-var],select[name=less-var]').each(function() {
		var name = $(this).attr('data-varname').replace(/^enable\-/, '');
		selection[name] = this.value;
	});
	
	
	var title = $.trim($('#selection-name').val());
	if(title.length == 0) return;

	var selections = ace.storage.get(storage_name);
	if(!selections) selections = {}
	else if(typeof selections === 'string') selections = JSON.parse(selections);
	
	var name = title.replace(/[^\d\w]/g , '-').replace(/\-\-/g, '-').toLowerCase();
	selections[name] = {name: name, title: title, selection: selection};
	ace.storage.set(storage_name, JSON.stringify(selections));
	
	var opt = $('<option />').appendTo('#selection-list');
	opt.attr('value', name);
	opt.text(title);
 }

 function display_selections() {
	var selections = ace.storage.get(storage_name);
	if(!selections) {
		$('#selection-list').parent().addClass('hide');
		return;
	}
	else if(typeof selections === 'string') selections = JSON.parse(selections);
	

	var count = 0;
	for(var s in selections) if(selections.hasOwnProperty(s)) {
		count++;
		var opt = $('<option />').appendTo('#selection-list');
		opt.attr('value', selections[s].name);
		opt.text(selections[s].title);
	}
	/**
	if(count == 0) {
		$('#selection-list').parent().addClass('hide');
		return;
	}
	*/


	$('#selection-list')
	.off('change.selection')
	.on('change.selection', function() {
		if(this.value == '') {
			$(this).next().addClass('hide');//the remove button
			return;
		}

		var selections = ace.storage.get(storage_name);
		if(!selections) return;
		else if(typeof selections === 'string') selections = JSON.parse(selections);
		
		for(var s in selections) if(selections.hasOwnProperty(s)) {
			if(selections[s].name == this.value) {
				$('input[type=checkbox][name=less-var]').removeAttr('checked').closest('.list-group-item').removeClass('active');
				
				var selection = selections[s]['selection'];	
				for(var i in selection) if(selection.hasOwnProperty(i)) {
					var element = $('[name=less-var][data-varname="enable-'+i+'"]');
					
					if( element.is('input[type=checkbox]') && parseInt(selection[i]) == 1) {
						element.prop('checked', true).closest('.list-group-item').addClass('active');
					}
					else if( element.is('select') || element.is('input[type=text]') ){
						element.val(selection[i]);
					}
				}
				break;
			}
		}
		
		
		$(this).next().removeClass('hide');//the remove button
	})
	.next().off('click').on('click', function() {
		var sel = $('#selection-list');
		if(sel.val() == '') return;
		var val = sel.val();
		
		var selections = ace.storage.get(storage_name);
		if(!selections) return;
		else if(typeof selections === 'string') selections = JSON.parse(selections);
		for(var s in selections) if(selections.hasOwnProperty(s)) {
			if(selections[s].name == val) {
				delete selections[s];
				ace.storage.set(storage_name, JSON.stringify(selections));
				
				sel.find('option[value='+val+']').remove();
				break;
			}
		}
	})
	.parent().removeClass('hide');
 }
 
 display_selections();

});