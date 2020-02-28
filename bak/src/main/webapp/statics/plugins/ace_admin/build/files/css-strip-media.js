function trim(str) {
	return str.replace(/^\s+/, '').replace(/\s+$/, '');
}
function remove_media_queries(content, disable_less_than) {
	var disable_less_than = disable_less_than || 900;
	var css_output = "";
	content = content.replace(/\/\*[^*]*\*+([^/*][^*]*\*+)*\//ig , '');//remove comments

	var disable_less_than = 900;
	//first remove all comments
	var selector_regex = /((?:\s*@media\s*[^\{\}]*)?(?:\s*@(?:-(?:moz|webkit|ms|o|khtml)-)?keyframes\s*[^\{\}]*)?(?:\s*[^\{\}]*))(\{)/ig

	var lastCloseBracket = 100000000, media_wrap = false, media_selector = '', pre_media_length = 0;
	var media_ignore = false;

	var result;
	while ((result = selector_regex.exec(content)) !== null) {
	  //we optimistically suppose that no '}' character is inside CSS values such as content:"}" or url('}.jpg')

	  var index = result.index;
	  if(media_wrap && index > lastCloseBracket) {
		//we have reached end of a @media or @keyframe
		if(!media_ignore && css_output.length > pre_media_length)
			css_output = css_output.substr(0, pre_media_length) + "\n" + trim(media_selector) + " {\n" + css_output.substr(pre_media_length) + "}\n";

		media_selector = '';
		media_wrap = false;
		media_ignore = false;
	  }

	  var end = content.indexOf('}', selector_regex.lastIndex);
	  lastCloseBracket = content.indexOf('}', end + 1);

	  var selector = result[1];

	  
	  if(selector.match(/@media/i) || selector.match(/@(?:-(?:moz|webkit|ms|o|khtml)-)?keyframes/i)) {
		media_selector = selector;
		media_wrap = true;

		if( selector.match(/@media/i) && selector.match(/(min|max)\-width/i) ) {
			var min_width = 0;
			if ( (min_width = selector.match(/min\-width:\s*(\d+)/i)) ) {
				min_width = parseInt(min_width[1]);
			}
			if( !min_width || min_width < disable_less_than ) media_ignore = true;
		}

		pre_media_length = css_output.length;
	  } else {
			var selector_parts = selector.split(/\,+/);
			var valid_parts = [];
			for(var c = 0 ; c < selector_parts.length; c++) {
				var sel = trim(selector_parts[c]);
				valid_parts.push(sel);
			}
			if(valid_parts.length == 0) continue;


			var rules_text = content.substring(selector_regex.lastIndex , end);
			var rule_list = rules_text.match(/(?:([\w\*\-%]+)\s*\:\s*([^\;\}]+))/ig);
			
			if(rule_list && rule_list.length > 0) {
				var override_rules = {};
				for(var r = 0; r < rule_list.length; r++) {
					var $rules = rule_list[r].match(/(?:([\w\*\-%]+)\s*\:\s*([^\;\}]+))/i);

					var rule = $rules[1].toLowerCase();
					var value = $rules[2];

					override_rules[rule] = {type: 1, value: value};
				}
				

				var new_rules_text = '';
				for(var rule in override_rules) if(override_rules.hasOwnProperty(rule)) {
					var new_rule = override_rules[rule];
					//if(new_rule.type == 0) {
						new_rules_text += "\t"+ rule+": "+ new_rule.value +";\n";
					//}
				}
				
				
				if( !media_ignore && new_rules_text.length > 0 ) {
					css_output += trim(valid_parts.join(','))+" {\n";
					css_output += new_rules_text;
					css_output += "}\n";
				}
			}
	  }
	}

	//if there's a remaining open media query, close it
	if(media_wrap && !media_ignore)  {
		if(css_output.length > pre_media_length)
			css_output = css_output.substr(0, pre_media_length) + "\n" + trim(media_selector) + " {\n" + css_output.substr(pre_media_length) + "}\n";
	}

	return css_output;
}