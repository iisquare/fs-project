if( typeof window.getMatchedCSSRules !== 'function' ) {
	alert('Please note that, this converter does not work in your browser.\nIt uses "getMatchedCSSRules" function which is available only on webkit browsers such as Chrome or Safari.');
}

//initiliaze codemirror (syntax highlighting)
window.onload = function() {
  var email_code = document.getElementById("email-code");

  window.editor_html = CodeMirror.fromTextArea(email_code, {
    mode: "text/html",
    lineNumbers: true,
    lineWrapping: true,
    extraKeys: {
		"Ctrl-Q": function(cm){ cm.foldCode(cm.getCursor()); },
		"Ctrl-J": "toMatchingTag",
		"F11": function(cm) {
          cm.setOption("fullScreen", !cm.getOption("fullScreen"));
        },
        "Esc": function(cm) {
          if (cm.getOption("fullScreen")) cm.setOption("fullScreen", false);
        }
	},
    foldGutter: true,
    gutters: ["CodeMirror-linenumbers", "CodeMirror-foldgutter"],
	matchTags: {bothTags: true}
  });
};



jQuery(function($) {
  var objectURL1 = null;

  $('#id-btn-preview').on('click', function() {
	  var options = getOptions();
	  updateDirection(options['rtl'])
	  updateStyles(options)

	  var raw_html = window.editor_html.getValue();
	  
	  var convertFrame = $('#convert-frame').get(0);
	  var frameWin = convertFrame.contentWindow || convertFrame
	  var output_html = frameWin.convert_email(raw_html, options);

	  $('#preview-dialog').modal('show');
  
	  var previewFrame = $('#preview-frame').get(0);
	  var preview =  previewFrame.contentDocument ||  previewFrame.contentWindow.document;
	  preview.open();
	  preview.write(output_html);
	  preview.close();

  });
  
  var winURL = (window.URL || window.webkitURL);
  $('#id-btn-save').on('click', function() {
	  var options = getOptions();
	  updateDirection(options['rtl'])
	  updateStyles(options)
	  
	  var raw_html = window.editor_html.getValue();
	  
	  var convertFrame = $('#convert-frame').get(0);
	  var frameWin = convertFrame.contentWindow || convertFrame
	  var output_html = frameWin.convert_email(raw_html, options);

	  try {
		if(objectURL1) winURL.releaseObjectURL(objectURL1);
	  } catch(e) {}
 	  objectURL1 = winURL.createObjectURL(new Blob([output_html], {type : 'text/html'}))
	 
	  $('#btn-save-html')
	  .attr({'download' : 'output-email.html'})
	  .get(0).href = objectURL1;
	  
	  $('#btn-save-html')[0].click();
  });
  
  $('.option-group .list-group-item input[type=text]').on('focus', function() {
	$(this).addClass('active');
  }).on('blur', function() {
	$(this).removeClass('active');
  }).parent().tooltip({placement: 'right', container: 'body'});
  
  function getOptions() {
	  var options = {}
	  $('.option-group .list-group-item input[type=text]').each(function() {
		var name = $(this).attr('name').match(/option\[([\w\d\-]+)\]/i)
		if(name) {
			var val = $(this).val();
			options[name[1]] = val;
		}
	  });
	  
	  options['base-width'] = options['base-width'] || 600;
	  options['body-background'] = options['body-background'] || '#E4E6E9';
	  options['content-background'] = options['content-background'] || '#FFFFFF';
	  options['text-color'] = options['text-color'] || '#444444';
	  options['hr-background'] = options['hr-background'] || '#E8E8E8';
	  
	  options['font-size'] = options['font-size'] || 13;
	  options['font-family'] = options['font-family'] || 'Arial, sans-serif';
	  
	  options['wrap-size'] = parseInt(options['wrap-size']) || 0;
	  
	  options['rtl'] = $('.option-group .list-group-item input[name="option[rtl]"]')[0].checked

	  return options;
  }

  function updateStyles(options) {
	  var convertFrame = $('#convert-frame').get(0);
	  var frameWin = convertFrame.contentWindow || convertFrame
	   
	  //also update the convert-html's font size and color according to options
	  var doc = (convertFrame.contentDocument || convertFrame.contentWindow.document)
	  if (doc.styleSheets) {
		var theRules = null;
		if (doc.styleSheets[2].cssRules)
			theRules = doc.styleSheets[2].cssRules
		else if (doc.styleSheets[2].rules)
			theRules = document.styleSheets[2].rules
		else theRules = null;
			
		if(theRules) {
			theRules[0].style.fontSize = options['font-size']+'px';
			theRules[0].style.color = options['text-color'];
			theRules[0].style.fontFamily = options['font-family'];
		}
	  }
  }
  
  function updateDirection(rtl) {
	  var convertFrame = $('#convert-frame').get(0);
	  var frameWin = convertFrame.contentWindow || convertFrame
	  var doc = (convertFrame.contentDocument || convertFrame.contentWindow.document)
	  if(rtl) $(doc.body).addClass('rtl');
	  else $(doc.body).removeClass('rtl');
  }



  $('#id-btn-later').on('click', function() {
	saveTemplate();
  });
  function saveTemplate() {
	var name = prompt("Please enter a name for this email:");
	if(!name || $.trim(name).length == 0) return;
	
	var content = window.editor_html.getValue();
	if(!content || content.length == 0) return;
	var title = $.trim( $('#email-title').val() );
	if(!title || title.length == 0) return;

	var $name = $.trim( name.replace(/[^\w\d\-\.]/g , '').toLowerCase() );
	if($name.length == 0) return;
	
	var emails = ace.storage.get('ace.saved-emails');
	if(!emails) emails = {}
	else if(typeof emails === 'string') emails = JSON.parse(emails);
	
	emails['saved-'+$name] = {name: $.trim(name), options: getOptions(), content: content , title: title}
	ace.storage.set('ace.saved-emails', JSON.stringify(emails));
	
	var opt = $('<option />').appendTo('#saved-templates');
	opt.attr('value', 'saved-'+$name).text( emails['saved-'+$name].name );
  }
  $('#saved-templates').on('change', function() {
	var self = this;
    var name = this.value;
	$('#btn-delete-email').addClass('hidden');
	
	if( name.match(/^saved\-/i) ) {
		var emails = ace.storage.get('ace.saved-emails');
		if( !emails ) return;
		if(typeof emails === 'string') emails = JSON.parse(emails);
		if(typeof emails[name] !== 'object') return;
		
		$('#btn-delete-email').removeClass('hidden');
		loadTemplate(emails[name]);
	}
	else if( name.match(/^demo\-/i) ) {
		$.ajax({url: 'demo/'+name+'.html', cache: false})
		.done(function(res) {
			var email = {}
			email.content = res;
			email.title = self.options[self.selectedIndex].innerHTML;
			email.options = {}
			
			if(name == 'demo-contrast') {
				email.options['body-background'] = '#113D68';
				email.options['base-width'] = 580;
				email.options['wrap-size'] = 20;
			}
			else if(name == 'demo-confirmation')	email.options['base-width'] = 450;
			else {
			  email.options['body-background'] = '#E4E6E9';
			  email.options['base-width'] = 600;
			 email.options['wrap-size'] = 8;
			}
			
			loadTemplate(email);
		});
	}
  })
  
  function loadTemplate(email) {
	window.editor_html.setValue(email.content);
	$('#email-title').val(email.title);
	var options = email.options;
	for(var o in options) if(options.hasOwnProperty(o)) {
		var inp = $('.option-group .list-group-item input[name="option['+o+']"]');
		if(inp[0].type == 'text') inp.val(options[o]);
		else if(inp[0].type == 'checkbox') inp[0].checked = options[o];
	}
  }

  function loadTemplates() {
	var emails = ace.storage.get('ace.saved-emails');
	if(!emails) return;
	
	if(typeof emails === 'string') emails = JSON.parse(emails);

	for(var e in emails) if(emails.hasOwnProperty(e)) {
		var opt = $('<option />').appendTo('#saved-templates');
		opt.attr('value', e).text( emails[e].name );
	}
  }
  loadTemplates();
  
  
  $('#btn-delete-email').on('click', function(e) {
	e.preventDefault();

	var name = $('#saved-templates').val();
	if( name.match(/^saved\-/i) ) {
		$('#saved-templates option:selected').remove();
		var emails = ace.storage.get('ace.saved-emails');
		if(!emails) return;
	
		if(typeof emails === 'string') emails = JSON.parse(emails);
		delete emails[name];
		ace.storage.set('ace.saved-emails', JSON.stringify(emails));
		
		$('#btn-delete-email').addClass('hidden');
	}
  });
});