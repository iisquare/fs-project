/**
 This is a quick and dirty script using "htmlparser2"
 to add some indentation to html outputs of JS mustache compiler.
 I haven't put any thought into it. It's only quick fixes and hacks for Ace html preview files only,
 so it's not supposed to be a general purpose solution and may not produce exactly the original html view.
*/

var htmlparser = require("htmlparser2")
var keep_help = false;

module.exports = function(html_input , keep_help_comments, is_ajax_content, callback) {
	keep_help = keep_help_comments || false;
	var handler = new htmlparser.DomHandler(function (error, dom) {
		if (error) { }
		else {
			var result = (!is_ajax_content ? "<!DOCTYPE html>" : "")+pretty_print(dom , 0);
			callback.call(null, result)
		}
	});
	var parser = new htmlparser.Parser(handler);
	parser.write(html_input);
	parser.done();
}




var emptyTags = {
	__proto__: null,
	area: true,
	base: true,
	basefont: true,
	br: true,
	col: true,
	frame: true,
	hr: true,
	img: true,
	input: true,
	isindex: true,
	link: true,
	meta: true,
	param: true,
	embed: true
};
function trim(str) {
	return str.replace(/^\s+/, '').replace(/\s+$/, '');
}
var blockTags = {
	__proto__: null,
	div: true,
	p: true,
	ul: true,
	ol: true,
	blockquote:true
};
function hasBlockElements(elements) {
	for(var e in elements) if(elements.hasOwnProperty(e)) {
		if(elements[e].name in blockTags) return true;
	}
	return false;
}





function pretty_print(dom , level) {
 var output = "";
 for(var i = 0 ; i < dom.length ; i++) {
	var e = dom[i];
	if(e.type == 'tag' || e.type == 'script' || e.type == 'style') {
		
		output += "\r\n";
		
		for(var t = 0 ; t < level ; t++) output += "\t";
		output += "<"+e.name;
		var pWithBlocks = e.name == "p" && hasBlockElements(e.children);//the p element cannot contain "block" levels, so don't add closing tag, to validate!

		if("attribs" in e) {
			for(var name in e.attribs) if(e.attribs.hasOwnProperty(name)) {
				var attribWrap = '"';
				if(e.attribs[name].indexOf('"') >= 0) {
					attribWrap = "'";//if attribute value contains " then use ' to wrap it
				}
				output += " "+name+"="+attribWrap+(trim(e.attribs[name]).replace(/\s/g, ' '))+attribWrap;
			}
		}


		if(e.name == "pre") {
			var text = e.children[0].data;
			var lines = text.split("\r\n");
			var new_lines = '';
			for(var l = 0 ; l < lines.length ; l++) new_lines += (lines[l].replace(/^\s+/, '')) + "\r\n";
			
			e.children[0].data = new_lines;
		}
		else if( ((e.type == "script" && "attribs" in e && !("src" in e.attribs)) || e.type == "style") 
			) {
			var text = e.children[0].data;
			var lines = text.split("\r\n");
			var new_lines = '';
			var tabs = ""; for(var t = 0 ; t < level+1 ; t++) tabs += "\t";
			for(var l = 0 ; l < lines.length ; l++) new_lines += tabs + lines[l] + "\r\n";
			
			e.children[0].data = new_lines;
		}
		
		if(e.name in emptyTags) {
			output += " />";
			
			if((i < dom.length - 1 && dom[i+1].type == 'tag') || (i < dom.length - 2 && dom[i+1].type == 'text' && dom[i+2].type == 'tag'));
			else output += "\r\n";
		}
		else {
			output += ">";
			if(pWithBlocks) output += "</"+e.name+">";//close the p tag here cause that's enough (only good for ACE!)
			
			var children_output = '';
			
			if(e.children && e.children.length > 0){
				if(e.children.length == 1 && (e.children[0].type == 'text' || e.children[0].type == 'comment')) {
					if(e.children[0].type == 'text') {
						var txt = e.children[0].data;
						if(e.name == "pre")	output += trim(txt);
						else if(txt.length < 40) {
							if(txt.match(/^\s/)) output += ' ';
							output += trim(txt);
							if(txt.match(/\s$/)) output += ' ';
						}
						else {
							if(txt.match(/^\s/)) {//if the text already has spaces before it, then ok, add tabs, etc
								output += "\r\n";
								for(var t = 0 ; t < level+1 ; t++) output += "\t";
							}
							output += trim(txt);
							if(txt.match(/\s$/)) {//if the text already has spaces after it, then ok, add tabs, etc
								output += "\r\n";
								for(var t = 0 ; t < level ; t++) output += "\t";
							}
						}
					}
					else {
						if( keep_help || e.children[0].data.indexOf('section:') == -1 ) {
						    output += "<!-- "+trim(e.children[0].data)+" -->";
						}
					}
				}
				else {
					children_output = pretty_print(e.children , !pWithBlocks ? level + 1 : level , e , i);

					output += children_output;
					for(var t = 0 ; t < level ; t++) output += "\t";
				}
			}

			if(!pWithBlocks) output += "</"+e.name+">";
			
			if(e.type == 'script' || e.type == 'style') {
				if( (i < dom.length - 1 && dom[i+1].type == e.type)
					|| (i < dom.length - 2 && dom[i+1].type == 'text' && dom[i+2].type == e.type) );
				else output += "\r\n";
			}
			else {
				if( i < dom.length - 1 && (dom[i+1].type == 'comment') );//don't add new line if adjacent next is a comment, we add new line after comment
				else if( i < dom.length - 1 && dom[i+1].type == 'text' && dom[i+1].data.match(/^\S/))
				{
					//console.log("here");
				}
				
				else if( i < dom.length - 2 && dom[i+1].type == 'text' && dom[i+2].type == 'tag'
					&& (!dom[i+2].children || (dom[i+2].children.length == 1 && dom[i+2].children[0].type == 'text')) )
				{
					//console.log("here");
				}
				
				else output += "\r\n";
			}
		}

		
	}
	else if(e.type == 'text' || e.type == 'comment') {
		var text = trim(e.data);
		if(text.length > 0) {
			if(e.type == 'comment' && i > 0 && dom[i-1].type == 'tag') {			
				if( keep_help || text.indexOf('section:') == -1 ) {
					output += "<!-- "+text+" -->\r\n";
				}
			}
			else {
				if(e.type == 'comment' && ( !(keep_help || text.indexOf('section:') == -1) )) {
					//ignore if #section comment and help not needed
					continue;
				}
			
				if(e.type == 'text' && i == dom.length - 1 && i >= 1);
				else output += "\r\n";

				if(e.type == 'text' && e.data.match(/^\S/) && i > 0 && dom[i-1].type == 'tag');//don't add tabs if text doesn't start with "SPACE"
				else for(var t = 0 ; t < level ; t++) output += "\t";
				
				if(e.type == 'text') output += text;
				else {

					if(text.indexOf('[if !IE]') >= 0) {
						output += '<!--[if !IE]> -->';
					}
					
					else if(text.indexOf('[if ') >= 0) {
						output += "<!--"+text+"-->";
					}
						
					else if(text.indexOf('<![endif]') >= 0) {
						output += '<!-- <![endif]-->';
					}
					
					else {
						output += "<!-- "+text+" -->";
					}
				}
				
				if(e.type == 'text' && i < dom.length - 1 && dom[i+1].type == 'tag');
				else if(e.type == 'comment' && i < dom.length - 2 && (dom[i+2].type == 'tag' || dom[i+2].type == 'script' || dom[i+2].type == 'style'));
				else output += "\r\n";
			}
		}

	}
 }

 return output;
}