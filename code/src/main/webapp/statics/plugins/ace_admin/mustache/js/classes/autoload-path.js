var fs = require('fs');
module.exports = function(partial_name , names , paths) {
	var parts = partial_name.split('.');
	var type = parts[0];
	if(type != 'page' && type != 'layout') return '';

	var format = new RegExp("^"+type+"\\.");
	var file = partial_name.replace(format , '').replace(/\./g , '/');//for example layout.sidebar.items becomes sidebar/items
	var $path = '';
	if(type == 'page' && file == 'content') {
		$path = paths['views']+"/"+type+"s/"+names['page_name']+".mustache";
		if(!fs.existsSync($path)) return '';
	}
	else {
		var item_name = names[type+'_name'];//page_name or layout_name , name of current page or layout

		//look in the partials for this page or layout
		$path = paths['views']+"/"+type+"s/partials/"+item_name+"/"+file+".mustache";
		if(!fs.existsSync($path)) {
			//look in the upper folder, partials for all pages or layouts
			$path = paths['views']+"/"+type+"s/partials/_shared/"+file+".mustache";
			if(!fs.existsSync($path)) return '';
		}
	}

	return $path;
}