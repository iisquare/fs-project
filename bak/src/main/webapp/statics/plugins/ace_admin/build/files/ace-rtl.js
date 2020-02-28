//simple script to convert styles to RTL
//it's not bullet proof or anything and some parts need to be manually treated
//it's because it doesn't convert all rules to RTL, only the parts that are needed will be put in a separate file
//ace-rtl.css and overriden


function makeRTL(css_input) {

	function trim(str) {
		return str.replace(/^\s+/, '').replace(/\s+$/, '');
	}

	var defaultify = {
		'left': 'auto',
		'right': 'auto',

		'padding-right': 0,
		'padding-left': 0,

		'margin-right': 'auto',
		'margin-left': 'auto',

		'border-left-color': 'transparent',
		'border-right-color': 'transparent',
		
		'border-left-width': 'transparent',
		'border-right-width': 'transparent',
		
		'border-left-style': 'none',
		'border-right-style': 'none',

		'border-left': 'none',
		'border-right': 'none',
		
		'-moz-border-left-colors': 'none',
		'-moz-border-right-colors': 'none',
		
		'border-bottom-right-radius': 0,
		'border-bottom-left-radius': 0,
		'border-top-right-radius': 0,
		'border-top-left-radius': 0
	};

	function splitted_values(rule, value) {
		value = value.replace(/\!important/g, '');
		if(rule.match(/border-color/i) && value.match(/rgb/i)) {
			//remove extra spaces in border-color
			value = trim(value).replace(/\s\s+/g, ' ').replace(/\s*([\(,])\s*/g, '$1')
		}
		var part = trim(value).split(/\s+/);
		if(part.length >= 4) {
			//if(part[3] == part[1]) return null;
			//swap left & right value		
			if(rule.match(/border-radius/i)) return [part[1], part[0], part[3], part[2]].join(' ');
			return [part[0], part[3], part[2], part[1]].join(' ');
		}
		//if(rule.match(/margin/i)) return part.join(' ');//if we return null, cases like [.rtl ul]'s margin will override that of [.nav-list], so we should have [.rtl .nav-list]'s margin again for safe measures
		return null;
	}


	//first remove all comments
	var selector_regex = /((?:\s*@media\s*[^\{\}]*)?(?:\s*@(?:-(?:moz|webkit|ms|o|khtml)-)?keyframes\s*[^\{\}]*)?(?:\s*[^\{\}]*))(\{)/ig

	var lastCloseBracket = 100000000, media_wrap = false, media_selector = '', pre_media_length = 0;

	var rtl_output = "";
	var content = css_input.replace(/\/\*[^*]*\*+([^/*][^*]*\*+)*\//ig , '');//remove comments


	var result;
	while ((result = selector_regex.exec(content)) !== null) {
	  //we optimistically suppose that no '}' character is inside CSS values such as content:"}" or url('}.jpg')

	  var index = result.index;
	  if(media_wrap && index > lastCloseBracket) {
		//we have reached end of a @media or @keyframe
		if(rtl_output.length > pre_media_length)
			rtl_output = rtl_output.substr(0, pre_media_length) + "\n" + trim(media_selector) + " {\n" + rtl_output.substr(pre_media_length) + "}\n";

		media_selector = '';
		media_wrap = false;
	  }

	  var end = content.indexOf('}', selector_regex.lastIndex);
	  lastCloseBracket = content.indexOf('}', end + 1);

	  var selector = result[1];
	  if(selector.match(/@media/i) || selector.match(/@(?:-(?:moz|webkit|ms|o|khtml)-)?keyframes/i)) {
		media_selector = selector;
		media_wrap = true;

		pre_media_length = rtl_output.length;
	  } else {
			var selector_parts = selector.split(/\,+/);
			var valid_parts = [];
			for(var c = 0 ; c < selector_parts.length; c++) {
				//ignore selectors that have "-left or -right", because they are already directional
				if(selector_parts[c].match(/(?:(?:[_\-\.])(?:left|right))|(?:\.arrowed)|(?:\.input-icon)|(\.rtl)|(\-rtl)|(\.dropdown-menu[^\:])|(\.scroll-)|(\.ace-switch)|(\.lbl)|(\.popover)|(\.table)|(\.ui-slider)|(\.nav-tabs)|(blockquote)|(\.datepicker)|(\.daterangepicker)|(\.dropdown-menu-right)|(\.footer-)|(\.chosen-)|(\.aside)/i)) {
					continue;
				}
				var sel = trim(selector_parts[c]);
				if(sel.match(/(?:\.no\-skin)|(?:\.skin\-\d)/)) valid_parts.push('.rtl'+sel);//.rtl.skin-1
				else valid_parts.push('.rtl '+sel);//.rtl .selector
			}
			if(valid_parts.length == 0) continue;

			var override_rules = {};

			var rules_text = content.substring(selector_regex.lastIndex , end);
			var rule_list = rules_text.match(/(?:([\w\*\-%]+)\s*\:\s*([^\;\}]+))/ig);

			if(rule_list && rule_list.length > 0) {
				for(var r = 0; r < rule_list.length; r++) {
					var $rules = rule_list[r].match(/(?:([\w\*\-%]+)\s*\:\s*([^\;\}]+))/i);

					var rule = $rules[1].toLowerCase();
					var value = $rules[2];

					if(rule.match(/(left|right)/i)) {
						override_rules[rule] = {type: 1, value: value};

					} else if(rule.match(/^(margin|padding|border-color|border-width|border-style|border-radius)$/i)) {
						override_rules[rule] = {type: 2, value: value};
					} else if(rule.match(/(box-shadow)/) && !value.match(/none/)) {
						if(value.match(/^\s*(\d)/)) {
							value = value.replace(/^\s*(\d)/ , '-$1');
							override_rules[rule] = {type: 0, value: value};
						}
						else if(value.match(/^\s*(\-)(\d)/)) {
							value = value.replace(/^\s*(\-)(\d)/ , '$2');
							override_rules[rule] = {type: 0, value: value};
						}
					}


					if(value.match(/(right|left)/i)) {
						if(value.match(/right/i)) value = value.replace(/right/i, 'left');
						 else value = value.replace(/left/i, 'right');
						override_rules[rule] = {type: 0, value: value};
					}
					else if(value.match(/center/i) && rule.match(/text\-align/i)) {
						override_rules[rule] = {type: 0, value: value};
					}
					/**else if(value.match(/none/i) && rule.match(/float/i)) {
						override_rules[rule] = {type: 0, value: value};
					}*/
					else if(value.match(/(rtl|ltr)/i)) {
						if(value.match(/rtl/i)) value = value.replace(/rtl/i, 'ltr');
						 else value = value.replace(/ltr/i, 'rtl');
						override_rules[rule] = {type: 0, value: value};
					}
					else if(value.match(/translateX\(/i)) {
						if(value.match(/translateX\(\-(.*)\)/i))
							value = value.replace(/translateX\(\-(.*)\)/ig , 'translateX($1)');
						else value = value.replace(/translateX\(([^\-]*)\)/ig , 'translateX(-$1)');
						override_rules[rule] = {type: 0, value: value};
					}
					/*else if(value.match(/rotate\(/i)) {
						var val = value.replace(/rotate\(.*\)/i , function(text, found) {
							console.log(text);
						});
					}*/
				}


				for(var rule in override_rules) if(override_rules.hasOwnProperty(rule)) {
					var cur_rule = override_rules[rule];
					if(cur_rule.type == 1) {
						var flip_rule;
						if(rule.match(/right/i)) flip_rule = rule.replace(/right/i, 'left');
						else flip_rule = rule.replace(/left/i, 'right');
					
						if(!(flip_rule in override_rules)) {
							//add a nullify/defaultify value for the opposite side
							override_rules[flip_rule] = {type: 0, value: cur_rule.value};
							
							/**if(rule.match(/border-(left|right)$/i)) {
								console.log(rule);
								delete override_rules[rule];
								rule += '-width';//change border-left to border-left-width
								if(!(rule in override_rules)) override_rules[rule] = {}
								override_rules[rule].value = defaultify[rule] + (cur_rule.value.match(/!important/) ? ' !important' : '');
								override_rules[rule].type = 0;
							}
							else*/ {
								override_rules[rule].value = defaultify[rule] + (cur_rule.value.match(/!important/) ? ' !important' : '');
								override_rules[rule].type = 0;
							}
						} else {
							//flip_rule is already a rule itself
							
							//swap left & right values unless they are equal, in that case we ignore it!
							var tmp1 = override_rules[rule].value;
							var tmp2 = override_rules[flip_rule].value;
							if(tmp1 === tmp2) {
								delete override_rules[rule];
								delete override_rules[flip_rule];
								continue;
							}
							var important = tmp2.match(/!important/) || tmp1.match(/!important/);
							override_rules[rule].value = tmp2 + (important && !tmp2.match(/!important/) ? ' !important' : '');
							override_rules[flip_rule].value = tmp1 + (important && !tmp1.match(/!important/) ? ' !important' : '');
							
							override_rules[flip_rule].type = override_rules[rule].type = 0;
						}
					} else if(cur_rule.type == 2) {
						var new_value = splitted_values(rule, cur_rule.value);
						if(new_value != null) {
							override_rules[rule].value = new_value + (cur_rule.value.match(/!important/) ? ' !important' : '');
							override_rules[rule].type = 0;
						}
					}
				}


				var new_rules_text = '';
				for(var rule in override_rules) if(override_rules.hasOwnProperty(rule)) {
					var new_rule = override_rules[rule];
					if(new_rule.type == 0) {
						new_rules_text += "\t"+ rule+": "+ new_rule.value +";\n";
					}
				}

				if(new_rules_text.length > 0) {
					rtl_output += trim(valid_parts.join(','))+" {\n";
					rtl_output += new_rules_text;
					rtl_output += "}\n";
				}
			}
	  }
	}

	//if there's a remaining open media query, close it
	if(media_wrap)  {
		if(rtl_output.length > pre_media_length)
			rtl_output = rtl_output.substr(0, pre_media_length) + "\n" + trim(media_selector) + " {\n" + rtl_output.substr(pre_media_length) + "}\n";
	}


	return rtl_output;

}