//Usage: node strip-media.js --in="ace.min.css" --out="ace.min.css"
//strip media queries, i.e. disable responsiveness

var fs = require('fs');
var vm = require('vm');
var arg = require('argh').argv;//read & parse arguements

try {
	var strip_func_file = 'files/css-strip-media.js';
	var code = fs.readFileSync(strip_func_file , 'utf-8');
	vm.runInThisContext(code, strip_func_file);
	
	var min_width = parseInt(arg['min']) || 900;//keep `min-width` media queries which are >= 900px

	var content = fs.readFileSync(arg['in'] , 'utf-8');
	var css_output = remove_media_queries(content, min_width);
	fs.writeFileSync(arg['out'], css_output, 'utf-8');
} catch(e) {
	console.log('Error!');
	console.log(e);
}
