//override the function that loads a new template, to make it load partials dynamically based on their path
var autoload = require('./autoload-path');
var fs = require('fs');

module.exports = function(hogan, paths) {
 var $page_name , $layout_name;
 this.set_params = function(page_name , layout_name) {
	$page_name = page_name || 'index';
	$layout_name = layout_name || 'default';
 }

 hogan.Template.prototype.rp = function(symbol, context, partials, indent) {
	var partial = this.ep(symbol, partials);
	
	if (!partial) {
	  var partial_name = symbol.replace(/^([^\w\d])/ , '').replace(/(\d)?(\d{1,})$/, '$1');
	  //convert something like: "<page.style_32" to "page.style_3"

	  var partial_path = autoload(partial_name , {'page_name' : $page_name , 'layout_name' : $layout_name} , paths);
	  if(partial_path != '') partial = fs.readFileSync(partial_path, 'utf-8');
	}
	if (!partial) {
        return '';
    }
	
	if (this.c && typeof partial == 'string') {
		partial = this.c.compile(partial, this.options);
    }
	
    return partial.ri(context, partials, indent);
 }
}