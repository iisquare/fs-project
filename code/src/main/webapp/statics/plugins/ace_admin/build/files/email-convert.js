var defaultTextStyles = {}
defaultTextStyles['font-family'] = 'AceFont1';
defaultTextStyles['font-size'] = '13';
defaultTextStyles['font-weight'] = 'normal';
defaultTextStyles['font-style'] = 'normal';

var base_padding = 24;
var extra_padding = 36;

function addZero(n, reslen) {
	var reslen = reslen || 2;
    var res = n + '';
    while (res.length < reslen) res = '0' + res;
    return res;
}
function rgbToHex(str) {
	var m
	if( (m = str.match(/(rgb\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*(\d+)\s*)?\))/ )) ) {
		var color = '#' 
		+ addZero(parseInt(m[2]).toString(16))
		+ addZero(parseInt(m[3]).toString(16))
		+ addZero(parseInt(m[4]).toString(16))
		
		str = str.replace(m[1], color)
	}
	return str;
}
function rgbToHexAll(str) {
	return str.replace(/(?:rgb\s*\(\s*(?:\d+)\s*,\s*(?:\d+)\s*,\s*(?:\d+)\s*(?:,\s*(\d+)\s*)?\))/ig , function(a,b,c) {
		var hex = rgbToHex(a);
		return hex;
	}).replace(/rgba\(\s*0\s*,\s*0\s*,\s*0\s*,\s*0\s*\)/ig , 'transparent')
}



function convert(content, options) {
	defaultTextStyles['font-size'] = options['font-size'];

	var unique_id = 1;

	var div = $('<div style="display: none;" data-uid="1" />').appendTo('body');
	div[0].innerHTML = content.replace(/(\&)(?:[\w\d]{2,10}\;)/g , function(a){//hide unicode &xxx; chars
		return a.replace(/&/g , '###amp;')
	});

	var res_div = $('<div><div style="display: none;" data-uid="1"></div></div>').appendTo('body');

	traverse(div[0]);


	res_div.find('[data-uid]').removeAttr('data-uid');
	res_div.find('table').each(function() {
		var $this = $(this);
		if( !$this.attr('cellspacing') ) $this.attr('cellspacing', '0');
		if( !$this.attr('cellpadding') ) $this.attr('cellpadding', '0');
		if( !$this.attr('border') ) $this.attr('border', '0');
		
		if( !this.style.tableLayout && ($(this).closest('.table-space').length == 0) ) this.style.tableLayout = 'fixed';
	});
	res_div.find('td').each(function() {
		var $this = $(this);
		if( !$this.attr('valign') ) $this.attr('valign', 'top');
		if( !$this.attr('align') ) $this.attr('align', options['rtl'] ? 'right' : 'left');
	});
	res_div.find('img').each(function() {
		var $this = $(this);
		if( !$this.attr('hspace') ) $this.attr('hspace', '0');
		if( !$this.attr('vspace') ) $this.attr('vspace', '0');
		if( !$this.attr('border') ) $this.attr('border', '0');
		
		if( !this.style.display && $this.siblings('img').length == 0 ) this.style.display = 'block';//for older outlooks?
		if( !this.style.paddingBottom && $this.closest('.table-col-td').length != 0 && !$this.next().is('br') ) this.style.paddingBottom = '9px';
	});

	res_div.find(' > div').children().unwrap();
	
	var output =
	res_div.html()
	.replace(/###amp;/g, '&')
	//.replace(/AceFont1/ig, options['font-family']).replace(/AceFont2/ig, options['font-family']);
	
	output = rgbToHexAll(output);
	output = output.replace(/line-height\:\s*(\d+\.\d+)px/ig, function(a, b, c) {
	  return a.replace(b, parseInt(Math.round(b)));
	});
	

	//wrap contents inside a slightly bigger td
	var wrap_size = parseInt(options['wrap-size'])
	if(wrap_size > 0) {
		var wrap_width = parseInt(options['base-width']) + wrap_size;
		if( div.find('.navbar').length == 0 )
			output = '<table><tr><td class="table-td-wrap" align="center" width="'+wrap_width+'">'+output+'</td></tr></table>';
	}

	div.remove();
	res_div.remove();

	return output;


  function traverse(input_parent) {
	var child = input_parent.firstChild;
	while(child != null) {
	 //if child is text append to parent! continue;//
	 
	 if( child.nodeType == 1 && child.getAttribute('data-uid') == null ) {
		 var $elem = $(child);
		 var $class = $elem.attr('class');
		 
		 unique_id++;
		 $elem.attr('data-uid', unique_id);

		 if( $elem.hasClass('row') ) {
			var uid = $elem.parent().attr('data-uid');
			var parent_td = res_div.find('[data-uid='+uid+']');
		 
			var table = $('<table class="table-row"><tr><td class="table-row-td"></td></tr></table>')
			.appendTo(parent_td);
			
			table.attr('width', options['base-width']);
			var td = table.find('td');
			td.attr('data-uid', unique_id);
			
			if( $elem.closest('.navbar').length == 0 ) {
				var bg = $elem[0].style.backgroundColor || options['content-background'];
				table.attr('bgcolor', bg).css('background-color', bg);
			}
		 }

		 else if( $elem.is('[class*=col]') && $elem.parent().hasClass('row') ) {
			var uid = $elem.parent().attr('data-uid');
			var parent_td = res_div.find('[data-uid='+uid+']');
			
			var class_name = $elem.parent().hasClass('sm-border') ? 'table-col-border': 'table-col';
			
			var new_table = 
			$('<table class="'+class_name+'"><tr><td class="table-col-td"></td></tr></table>')
			.appendTo(parent_td);
			new_table.attr('align', options['rtl'] ? 'right' : 'left');
			
			var siblings = $elem.siblings('[class*=col]').length;
			var col_count = siblings + 1;


			//calculat .row padding and column margins
			var pclass = $elem.parent().attr('class');
			var match_padding = null;
			if( pclass && (match_padding = pclass.match(/padding\-(\d+)/i)) ) {
				match_padding = parseInt(match_padding[1]);
			}
			var row_padding = typeof match_padding === 'number' ? match_padding : extra_padding;
			if( typeof match_padding === 'number' ) {
				parent_td.closest('table').attr('class', 'table-row-fixed');
				parent_td.closest('.table-row-td').attr('class', 'table-row-fixed-td');
			}

			var margins = {'1': 0, '2': 18, '3': 16, '4': 16, '6': 9}
			var match_margin = null;
			if( pclass && (match_margin = pclass.match(/margin\-(\d+)/i)) ) {
				match_margin = parseInt(match_margin[1]);
			}			
			var margin = typeof match_margin === 'number' ? match_margin : margins[col_count];
			//
			

			
			var grid_size = ((options['base-width'] - (2 * row_padding)) - (siblings * margin)) / 12;

			var is_last_child = ($elem.nextAll('[class*=col]').length == 0) 

			var col_margin = is_last_child ? 0 : margin;
			var width = 0;
			var clen = $elem.attr('class').match(/col-(?:xs|sm|md|lg)-(\d+)/i);
			if(clen && clen[1]) {
				var clen = parseInt(clen[1]);
				
				width = parseInt(grid_size * clen)
				new_table.attr('width', width + col_margin);
				
				if(col_margin > 0) new_table.css('padding-' + (options['rtl'] ? 'left' : 'right'), col_margin)
				//columns with extra padding
				new_table.closest('td[class*="table-row-"]').css({'padding-left': row_padding, 'padding-right': row_padding});
			} else {
				//single .col only
				row_padding = typeof match_padding === 'number' ? match_padding : base_padding;
				width = (options['base-width'] - (row_padding * 2));
				new_table.attr('width', width);
				new_table.closest('td[class*="table-row-"]').css({'padding-left': row_padding, 'padding-right': row_padding});
			}

			var td = new_table.find('td');
			td.attr('data-uid', unique_id);
			td.attr('width', width);
			td.addClass('table-col-td');
		 }

		 else if( $elem.hasClass('clearfix') ) {
			var uid = $elem.parent().attr('data-uid');
			var parent_td = res_div.find('[data-uid='+uid+']');
			var new_table = $('<table class="table-row" style="table-layout: auto;" />').appendTo(parent_td);
			
			
			if( $elem.closest('.navbar').length == 0 ) {
				var bg = $elem[0].style.backgroundColor || options['content-background'];
				new_table.attr('bgcolor', bg).css('background-color', bg);
			}

			var tmp = $elem;
			while( true ) {
				var new_tr = $('<tr />').appendTo(new_table);
				new_tr.attr('data-uid', unique_id);
				
				tmp = tmp.next();
				if( tmp.hasClass('clearfix') ) {
					unique_id++;
					tmp.attr('data-uid', unique_id);
				}
				else break;
			}
			
			var pclass = $elem.attr('class');
			var match_padding = null;
			if( pclass && (match_padding = pclass.match(/padding\-(\d+)/i)) ) {
				match_padding = parseInt(match_padding[1]);
			}
			var padding = typeof match_padding === 'number' ? match_padding : base_padding;
			if( typeof match_padding === 'number' ) parent_td.closest('table').attr('class', 'table-row-fixed');

			
			new_table.css('padding-right', padding);
			new_table.css('padding-left', padding);
			var parent_width = new_table.closest('[width]').attr('width') || options['base-width'];
			new_table.attr('width', parent_width).css('width', parent_width);
		 }
		 else if( $elem.is('[class*=pull-]') && $elem.parent().hasClass('clearfix') ) {
			var parent = $elem.parent();
			var uid = parent.attr('data-uid');
			var parent_tr = res_div.find('[data-uid='+uid+']');
			//append now
			var new_td = $('<td class="table-row-td" />').appendTo(parent_tr);
			if(parent[0].style.height) new_td.css('height', parent[0].style.height);
			new_td.attr('data-uid', unique_id);
			if( new_td.closest('table').hasClass('table-row-fixed') ) new_td.attr('class', 'table-row-fixed-td');
			
			//add padding between tds
			if( $elem.nextAll('[class*=pull-]').length > 0 ) {
				var padding = parseInt($elem.css('padding-'+(options['rtl'] ? 'left' : 'right'))) || 16;
				new_td.css('padding-'+(options['rtl'] ? 'left' : 'right'), padding);
			}
			if( $elem.parent().next().hasClass('clearfix') ) {
				var padding_bottom = parseInt($elem.css('padding-bottom')) || 12;
				new_td.css('padding-bottom', padding_bottom);
			}

			//var line_height = parseInt($elem.css('line-height')) || 18;
			//new_td.css('line-height', line_height+'px');
		 }

		 else if( $elem.attr('class') && $elem.attr('class').match(/(?:space|hr|break)-(\d+)/i) ) {
			var clen = $elem.attr('class').match(/(?:space|hr|break)-(\d+)/i);
			var h = 16;
			if(clen && clen[1]) {
				h = parseInt(clen[1]);// * 2;
			}
			
			var uid = $elem.parent().attr('data-uid');
			var parent = res_div.find('[data-uid='+uid+']');
			
			var table = $('<table class="table-space" height="'+h+'" style="height:'+h+'px"><tr><td class="table-space-td" valign="middle" height="'+h+'" style="height:'+h+'px" style="font-size:0;">&nbsp;</td></tr></table>').appendTo(parent);
			var w = table.closest('[width]').attr('width') || options['base-width'];
			w = parseInt(w);
			
			var padding = parseInt(base_padding * (w / options['base-width']));
			table.attr('width', w).css({'font-size': 0, 'line-height': 0, 'width':w})
			.find('td').attr('width', w).css('width', w);
			
			if($elem.is('[class*=space]')) {
				var bg = $elem[0].style.backgroundColor || options['content-background'];
				if($elem.hasClass('transparent') || $elem.closest('.well,.alert,.navbar').length > 0) bg = 'transparent';

				table.attr('bgcolor', bg).css('background-color', bg)
				.find('td').attr('bgcolor', bg).css('background-color', bg)
			}
			else if($elem.is('[class*=break]')) {
				var bg = $elem[0].style.backgroundColor || options['body-background'];
				table.attr('bgcolor', bg).css('background-color', bg)
				.find('td').attr('bgcolor', bg).css('background-color', bg)
			}
			else if($elem.is('[class*=hr]')) {
				var bg = $elem[0].style.backgroundColor || options['content-background'];
				if($elem.hasClass('transparent') || $elem.closest('.well,.alert,.navbar').length > 0) bg = 'transparent';
				
				table.attr('bgcolor', bg).css('background-color', bg)
				.find('td')
				.attr({'bgcolor': bg, 'align': 'center'})
				.css('background-color', bg)
			
			
				var hr_padding = null;
				if($elem.hasClass('compact')) hr_padding = padding;
				else if( (hr_padding = $elem.attr('class').match(/padding\-(\d+)/i)) ) {
					hr_padding = parseInt(hr_padding[1]);
				}
				
				var parent_padding = typeof hr_padding === 'number' ? hr_padding : parseInt(extra_padding / 2);
				
				bg = $elem[0].style.color || options['hr-background'];
				var parent_td = table.find('td');
				var td = $('<table bgcolor="'+bg+'" height="0" width="100%"><tr><td bgcolor="'+bg+'" height="1" width="100%" style="height: 1px; font-size:0;">&nbsp;</td></tr></table>')
				.appendTo(parent_td);
				parent_td.css({'padding-left': parent_padding, 'padding-right': parent_padding})
			}
		 }
		 
		 else if( $elem.hasClass('navbar') ) {
			var uid = $elem.parent().attr('data-uid');
			var parent = res_div.find('[data-uid='+uid+']');
			var table = $('<table style="table-layout: auto;"><tr><td></td></tr></table>').appendTo(parent);
			table.attr('width', '100%').css('width', '100%').find('td').attr({'width' : '100%', 'align': 'center'}).css('width', '100%');
			
			var bg = $elem.css('background-color');

			table.attr('bgcolor', bg).css('background-color', bg).find('td').attr('bgcolor', bg).css('background-color', bg);
			$elem.attr('data-skipstyle', true);
			
			var navbar_h = $elem[0].style.height || 50;
			table = $('<table class="navbar-row" height="'+navbar_h+'" width="'+options['base-width']+'"><tr><td class="navbar-row-td" valign="middle" height="'+navbar_h+'" width="'+options['base-width']+'"></td></tr></table>').appendTo(table.find('td'))
			table.find('td').attr('data-uid', unique_id);
		 }

		 else if( $elem.is('h1,h2,h3,h4,h5,h6') ) {
			var uid = $elem.parent().attr('data-uid');
			var parent = res_div.find('[data-uid='+uid+']');
			var table = $('<table class="header-row"><tr><td class="header-row-td"></td></tr></table>').appendTo(parent);
			table.find('td').attr('data-uid', unique_id);

			var w = table.closest('[width]').attr('width') || options['base-width'];
			table.attr('width', w).find('td').attr('width', w);
		 }
		 

		 else {
			var uid = $elem.parent().attr('data-uid');
			var parent = res_div.find('[data-uid='+uid+']');
			var $tag = $elem[0].tagName.toLowerCase();

			if( ($tag == 'div' || $tag == 'p') && 
				 (parseInt($elem.css('padding-left')) > 0 || parseInt($elem.css('padding-right')) > 0
					|| parseInt($elem.css('padding-top')) > 0 || parseInt($elem.css('padding-bottom')) > 0) ) 
			{
				//because outlook doesn't allow padding on DIV
				//we convert it to TD
				var element = $('<table width="100%"><tr><td width="100%"></td></tr></table>').appendTo(parent);
				element.find('td').attr('data-uid', unique_id);
			}
			
			else {
				var element = $('<'+$tag+' />').appendTo(parent);
				element.attr('data-uid', unique_id);
				
				if( $tag == 'hr' ) {
					$elem.attr('data-skipstyle', true);
					element.css({'background-color': $elem[0].style.backgroundColor || options['hr-background'], 'border-width': 0, 'height': '1px'})
				}
			}
		 }

	 }

	 else if(child.nodeType == 3) {
		var puid = $(child).parent().attr('data-uid');
		var parent = res_div.find('[data-uid='+puid+']');
		parent.append(child.textContent.replace(/\"/g, '&quot;').replace(/\</g, '&lt;').replace(/\>/g, '&gt;'));
	 }
	 /**
	 else if(child.nodeType == 8) {
	 }
	 */
	 
	 
	 if(child.nodeType == 1) {
		var mirror = res_div.find('[data-uid='+child.getAttribute('data-uid')+']')
		if(mirror.length == 1) {

		var attributes = child.attributes;
		for (var i = 0; i < attributes.length; i++){
			var attr = attributes.item(i);
			
			var attr_name = attr.nodeName.toLowerCase();
			if( attr_name == 'style' || attr_name.indexOf('on') == 0 ) continue;//ignore onclick, etc
			if( attr_name == 'class' && child.tagName.toLowerCase() != 'img') continue;//allow class on image only
			if( !mirror.attr(attr.nodeName) ) mirror.attr(attr.nodeName, attr.nodeValue);
		}


		
		var applied_styles = {}
		var elem = child;
		var $elem = $(elem);
		var rules = $elem.attr('data-skipstyle') ? null : window.getMatchedCSSRules(elem);
		if( (rules && rules.length > 0) || (elem.style.length > 0) ) {
			if(rules) for(var i = 0; i < rules.length; i++) {
				var styles = rules[i]['style'];
				for(var k = 0 ; k < styles.length; k++) {
					applied_styles[styles[k]] = true;
				}
			}
			for(var k = 0 ; k < elem.style.length; k++) {
				applied_styles[elem.style[k]] = true;
			}

			var styles = {}

			var computedStyle = window.getComputedStyle(elem, null);
			for(var k in applied_styles) {
				k = $.trim(k.toLowerCase());
				var m
				//exclude margin for now, not supported by outlook
				if((m = k.match(/^(margin|padding)(?:-(left|right|top|bottom))?/))) {
					if( $elem.hasClass('row') || $elem.is('[class*=col]') || $elem.hasClass('clearfix') ) continue;
					var name = m[1];
					if( !(name in styles) ) styles[name] = {}
					if( typeof m[2] !== 'undefined' ) styles[name][m[2]] = computedStyle[name+'-'+m[2]];
					else {
						var val = computedStyle[name];
						var part = val.split(/\s+/);
						styles[name]['top'] = part[0];
						styles[name]['right'] = part[1] || part[0];
						styles[name]['bottom'] = part[2] || part[0];
						styles[name]['left'] = part[3] || part[1] || part[0];
					}
				}
				else if(k.match(/border/)) {//only supports border/border-top/border-bottom/border-left/border-right
					if( (m = k.match(/^(border)(?:-(left|right|top|bottom))?(?:-(width|color|style))?$/)) ) {
						if( typeof m[2] !== 'undefined' && typeof m[3] !== 'undefined' ) {
							if( !('border' in styles) ) styles['border'] = {}
								styles['border'][m[2]] = rgbToHex(computedStyle['border-'+m[2]]);
						}
					}
				}
				
				else if( k.match(/^(?:background-)?color$/) ) {
					var color = rgbToHex(computedStyle[k]);
					styles[k] = color;
					if( k.match(/^background-color$/) && (mirror.is('td') || mirror.is('table')) ) mirror.attr('bgcolor', color);
				}
				else if( k.match(/^(?:width|height)$/) ) {
					var attr = mirror.attr(k);
					if( !attr && ('style' in mirror[0] || !mirror[0].style[k])  ) {
						styles[k] = computedStyle[k];
						var val = parseInt(computedStyle[k]);
						if( !isNaN(val) && (mirror.is('td') || mirror.is('table') || mirror.is('tr') || mirror.is('img')) ) {
							mirror.attr(k, styles[k]);
						}
					}
					if( attr && !mirror[0].style[k] ) styles[k] = attr;
				}
				else if(k.match(/text-align/)) {
					if( mirror.is('td') ){
						var v = computedStyle[k];
						mirror.attr('align', v);
					}
					styles[k] = computedStyle[k];
				}
				else if(k.match(/vertical-align/)) {
					if( mirror.is('td') ){
						var v = computedStyle[k];
						if(v == 'top' || v == 'bottom' || v == 'middle') {
							mirror.attr('valign', v);
						} else mirror.attr('valign', 'top');
					}
					styles[k] = computedStyle[k];
				}
				
				else if(k.match(/^(font-family|font-style|font-size|font-weight)$/)) {
					if( computedStyle[k] != defaultTextStyles[k] || mirror.is('td') ) styles[k] = computedStyle[k];
					var tag = elem.tagName.toUpperCase();
					if( k.match(/^(font-weight)$/) && (tag == 'B' || tag == 'STRONG') ) delete styles[k];
					else if( k.match(/^(font-style)$/) && (tag == 'I' || tag == 'EM') ) delete styles[k];
				}

				else if(k.match(/^(direction|letter-spacing|line-height|text-decoration|list-style-type|font-variant)$/)) {
					//valid styles only, just copy them over
					styles[k] = computedStyle[k];
				}
			}
			
			
			for(var name in styles) {
				var val = styles[name];
				if( name.match(/padding|margin|border/i) ) {
					if(val.left === val.right && val.left === val.top && val.top === val.bottom) {
						if( name.match(/margin/i) && parseInt(val.left) !== 0 ) continue;//don't allow non-zero margin on any element (outlook doesn't support it)
						mirror.css(name, val.left);
					}
					else {
						for(var dir in val) if(val.hasOwnProperty(dir)) {
							if( name.match(/margin/i) && parseInt(val[dir]) !== 0 ) continue;//don't allow non-zero margin on any element (outlook doesn't support it)
							mirror.css(name+'-'+dir , val[dir]);
						}
					}
				}
				else {
					mirror.css(name , val);
				}
			}
		}

	   }
	 }


	 if(child.nodeType == 1) traverse(child);
	 child = child.nextSibling;
	}//while


  }//traverse


}//convert


function get_output(input_str, options) {
 options['base-width'] = parseInt(options['base-width']);
 
 var wrap_width = options['base-width'] + parseInt(options['wrap-size']);
 
 var str = convert(input_str, options);

 str = '<!doctype html>\n\
 <html xmlns="http://www.w3.org/1999/xhtml">\n\
 <head>\n\
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />\n\
  <meta name="viewport" content="initial-scale=1.0" />\n\
  <meta name="format-detection" content="telephone=no" />\n\
  <title></title>\n\
  <style type="text/css">\n\
 	body {\n\
		width: 100%;\n\
		margin: 0;\n\
		padding: 0;\n\
		-webkit-font-smoothing: antialiased;\n\
	}\n\
	@media only screen and (max-width: 600px) {\n\
		table[class="table-row"] {\n\
			float: none !important;\n\
			width: 98% !important;\n\
			padding-left: 20px !important;\n\
			padding-right: 20px !important;\n\
		}\n\
		table[class="table-row-fixed"] {\n\
			float: none !important;\n\
			width: 98% !important;\n\
		}\n\
		table[class="table-col"], table[class="table-col-border"] {\n\
			float: none !important;\n\
			width: 100% !important;\n\
			padding-left: 0 !important;\n\
			padding-right: 0 !important;\n\
			table-layout: fixed;\n\
		}\n\
		td[class="table-col-td"] {\n\
			width: 100% !important;\n\
		}\n\
		table[class="table-col-border"] + table[class="table-col-border"] {\n\
			padding-top: 12px;\n\
			margin-top: 12px;\n\
			border-top: 1px solid '+(options['hr-background'] || '#E8E8E8')+';\n\
		}\n\
		table[class="table-col"] + table[class="table-col"] {\n\
			margin-top: 15px;\n\
		}\n\
		td[class="table-row-td"] {\n\
			padding-left: 0 !important;\n\
			padding-right: 0 !important;\n\
		}\n\
		table[class="navbar-row"] , td[class="navbar-row-td"] {\n\
			width: 100% !important;\n\
		}\n\
		img {\n\
			max-width: 100% !important;\n\
			display: inline !important;\n\
		}\n\
		img[class="pull-right"] {\n\
			float: right;\n\
			margin-left: 11px;\n\
            max-width: 125px !important;\n\
			padding-bottom: 0 !important;\n\
		}\n\
		img[class="pull-left"] {\n\
			float: left;\n\
			margin-right: 11px;\n\
			max-width: 125px !important;\n\
			padding-bottom: 0 !important;\n\
		}\n\
		table[class="table-space"], table[class="header-row"] {\n\
			float: none !important;\n\
			width: 98% !important;\n\
		}\n\
		td[class="header-row-td"] {\n\
			width: 100% !important;\n\
		}\n\
	}\n\
	@media only screen and (max-width: 480px) {\n\
		table[class="table-row"] {\n\
			padding-left: 16px !important;\n\
			padding-right: 16px !important;\n\
		}\n\
	}\n\
	@media only screen and (max-width: 320px) {\n\
		table[class="table-row"] {\n\
			padding-left: 12px !important;\n\
			padding-right: 12px !important;\n\
		}\n\
	}\n\
	@media only screen and (max-width: '+wrap_width+'px) {\n\
		td[class="table-td-wrap"] {\n\
			width: 100% !important;\n\
		}\n\
	}\n\
  </style>\n\
 </head>\n\
 <body '+(options['rtl'] ? 'dir="rtl" ' : '')+'style="font-family: '+options['font-family']+'; font-size:'+options['font-size']+'px; color: '+options['text-color']+'; min-height: 200px;" bgcolor="'+options['body-background']+'" leftmargin="0" topmargin="0" marginheight="0" marginwidth="0">\n\
 <table width="100%" height="100%" bgcolor="'+options['body-background']+'" cellspacing="0" cellpadding="0" border="0">\n\
 <tr><td '+(options['rtl'] ? 'dir="rtl" ' : '')+'width="100%" align="center" valign="top" bgcolor="'+options['body-background']+'" style="background-color:'+options['body-background']+'; min-height: 200px;'+(options['rtl'] ? 'direction:rtl;' : '')+'">\n'
 +
 str
 +
 '\n</td></tr>\n\
 </table>\n\
 </body>\n\
 </html>';
 
 var title = options['title'] || '';
 return str.replace(/\<title\>\<\/title\>/i, '<title>'+title+'</title>');
}