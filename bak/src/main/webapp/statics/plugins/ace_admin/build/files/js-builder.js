jQuery(function($) {
 var winURL = (window.URL || window.webkitURL);
 var objectURL1 = null, objectURL2 = null;
 
 var script_names = {}, script_contents = {};
 var script_list = [];
 $.ajax({url: "../assets/js/src/scripts.json", cache:false, dataType: 'json'}).done(function(list) {
	for(var name in list) script_list.push(name);
	get_file(0);
 });

 function get_file(index) {
	if(index >= script_list.length) {
		show_list();
		return;
	}

	var script_file = script_list[index];
	$.ajax({url: "../assets/js/src/" + script_file, cache: false, dataType: 'text'})
	.done(function(content) {
		script_names[script_file] = [script_file];
		script_contents[script_file] = content;

		//if the file has an alternative, e.g. "ace.submenu-1.js" has an alternative "ace.submenu-2.js"
		if(script_file.match(/\-1\.js$/)) {
			var alt_script = script_file.replace(/\-(1)\.js$/, "-2.js");
			$.ajax({url: "../assets/js/src/" + alt_script, cache: false, dataType: 'text'})
			.done(function(content) {
				script_names[script_file].push(alt_script);
				script_contents[alt_script] = content;
			})
			.complete(function() {
				get_file(index + 1);//get next file
			});
		}
		else get_file(index + 1);//get next file
	})

 }


 function show_list() {
	var list = $('<div class="list-group" />').appendTo('#page-content');
	var hr_break = false;
	list.append('<hr /><h3 class="text-primary"><div class="checkbox"><label><input type="checkbox" class="section" data-type="elements" /> Ace Elements</label></div></h3>');
	var script_type = 'elements';
	
	for(var name in script_names) if( script_names.hasOwnProperty(name) ){
		//add a separating line between "elements" and "functions" sections
		if(!hr_break && !name.match(/^elements/)) {
			list.append('<hr /><h3 class="text-primary"><div class="checkbox"><label><input type="checkbox" class="section" data-type="functions" /> Ace Functions</label></div></h3>');
			script_type = 'functions';
			hr_break = true;
		}

		var script_file = script_names[name];
		var input_type = script_file.length == 1 ? 'checkbox' : 'radio';

		var container = $('<div class="list-group-container" data-type="'+script_type+'" />').appendTo(list);

		//sometimes there are more that one scripts under a name (e.g. the alternatives)
		for(var i = 0; i < script_file.length; i++) {
			var comment = script_contents[script_file[i]].match(/^\/\*\*([^]+?)\*\//)
			if(comment) {
				//http://phpjs.org/functions/nl2br/
				var item = $('<div class="list-group-item" />').appendTo(container);
				comment = $.trim(comment[1]).replace(/([^>\r\n]?)(\r\n|\n\r|\r|\n)/g, '$1' + '<br />' + '$2')
				item.append('<div class="'+input_type+'"><label><input class="script" type="'+input_type+'" name="'+name+'" data-name="'+script_file[i]+'" value="'+(i)+'" />'+comment+'</label></div>');

				//enable & hide the required items
				if( input_type == 'checkbox' && comment.match(/^required/i) ) {
					container.addClass('hide required').find('input:checkbox').get(0).checked = true;
				}
			}
		}
	}
	
	$(document).on('click', 'input[type=checkbox],input[type=radio]', function() {
		if($(this).attr('data-type')) {
			var checked = this.checked;
			var type = $(this).attr('data-type');
			$('.list-group-container[data-type='+type+']:not(.required)').each(function() {
				var inp = $(this).find('input').eq(0);

				inp.get(0).checked = checked;

				if(checked) $(inp).closest('.list-group-item').addClass('active');
				else $(inp).closest('.list-group-item').removeClass('active');
			});				
			return;
		}
	
		if(this.checked) {
			if(this.type == 'radio') $(this).closest('.list-group-container').find('.list-group-item').removeClass('active');
			$(this).closest('.list-group-item').addClass('active');
		}
		else $(this).closest('.list-group-item').removeClass('active');
	});
 }



 $('#btn-build-js').on('click', function() {
	releaseUrlObjects();
	function showModal() {
		$('#btn-build-js').button('reset').removeClass('disabled');
		$('#btn-launch-modal').removeClass('hide');
		$('#modal-save-dialog').modal('show');
	}

	$(this).button('loading').text('Please wait...').addClass('disabled');

	setTimeout(function() {
		build_js();
	} , 100);

	function build_js() {
		var compress = $("#id-compress-js").get(0).checked;
		var js_output = {}
		$(['functions', 'elements']).each(function(i, script_type) {
			if( !(script_type in js_output) ) js_output[script_type] = '';
			$('.list-group-container[data-type='+script_type+'] input.script').each(function() {
				if(this.checked) {
					var script_content = script_contents[$(this).data('name')];
					js_output[script_type] += script_content
										+ (!script_content.match(/;(?:\s*)$/) ? ";" : "")
										+ "\n\n";
				}
			})

			if(compress) {
				var ast = UglifyJS.parse(js_output[script_type]);

				ast.figure_out_scope();
				var compressor = UglifyJS.Compressor()
				ast = ast.transform(compressor);

				// need to figure out scope again so mangler works optimally			
				ast.figure_out_scope();
				ast.compute_char_frequency();
				ast.mangle_names();
				js_output[script_type] = ast.print_to_string();
			}
			
		});


		var merge = $("#id-merge-js").get(0).checked;
		if(merge) {
			$('#btn-save-js-elements,#btn-save-js-functions').addClass('hide');

			js_output = js_output['elements']+';'+js_output['functions'];
			objectURL1 = js_output.length <= 1 ? 'javascript:void(0)' : winURL.createObjectURL(new Blob([js_output], {type : 'text/javascript'}))

			$('#btn-save-js-merged').removeClass('hide')
			.attr({'download' : 'ace'+(compress ? '.min':'')+'.js'})
			.find('.filesize').remove().end()
			.append('<span class="filesize"> ('+readableSize(js_output.length)+')</span>')
			.get(0).href = objectURL1;
		} else {
			$('#btn-save-js-merged').addClass('hide');

			objectURL1 = js_output['elements'].length == 0  ? 'javascript:void(0)'  : winURL.createObjectURL(new Blob([js_output['elements']], {type : 'text/javascript'}))
			objectURL2 = js_output['functions'].length == 0 ? 'javascript:void(0)' : winURL.createObjectURL(new Blob([js_output['functions']], {type : 'text/javascript'}))
			
			
			$('#btn-save-js-elements').removeClass('hide')
			.attr({'download' : 'ace-elements'+(compress ? '.min':'')+'.js'})
			.find('.filesize').remove().end()
			.append('<span class="filesize"> ('+readableSize(js_output['elements'].length)+')</span>')
			.get(0).href = objectURL1;

			$('#btn-save-js-functions').removeClass('hide')
			.attr({'download' : 'ace'+(compress ? '.min':'')+'.js'})
			.find('.filesize').remove().end()
			.append('<span class="filesize"> ('+readableSize(js_output['functions'].length)+')</span>')
			.get(0).href = objectURL2;
		}
		
		

		var options = [];
		if(compress) options.push('minified');
		if(merge) options.push('merged');
		$('.modal-title').find('.text-muted').remove().end().append(options.length == 0 ? '' : '<span class="text-muted"> ('+options.join('/')+')</span>');

		showModal();
	}
 });

 $(window).on('unload' , function() {
	releaseUrlObjects();
 });

 function releaseUrlObjects() {
	if(objectURL1 && typeof objectURL1 === "object") winURL.revokeObjectURL(objectURL1);
	if(objectURL2 && typeof objectURL1 === "object") winURL.revokeObjectURL(objectURL2);
	objectURL1 = objectURL2 = null;
 }
 
 function readableSize(size) {
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

 var storage_name = 'ace.js-selection';
 function save_selection() {
	var selection = {}
	$('input.script').each(function() {
		var name = $(this).attr('data-name');
		selection[name] = this.checked ? 1 : 0;
	});
	selection['option-compress'] = $('#id-compress-js').get(0).checked ? 1 : 0;
	
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
				$('input.script').removeAttr('checked').closest('.list-group-item').removeClass('active');
				
				var selection = selections[s]['selection'];	
				for(var i in selection) if(selection.hasOwnProperty(i) && parseInt(selection[i]) == 1) {
					$('input.script[data-name="'+i+'"]')
					.each(function() {this.checked = true})
					.closest('.list-group-item').addClass('active');
				}
				break;
			}
		}
		
		$('#id-compress-js').get(0).checked = selection['option-compress'] == 1 ? true : false;
		
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