var fs = require('fs');

function aceRTL(compile_less) {
	var files = [
	 'bootstrap.css',

	 'ace.css',
	 'ace-skins.css'
	];

	var content = '';
	for(var f = 0; f < files.length; f++) {
		content += fs.readFileSync(__dirname+'/../assets/css/' + files[f] , 'utf-8');
	}

	var rtl_func_file = __dirname+'/files/ace-rtl.js';
	var code = fs.readFileSync(rtl_func_file , 'utf-8');
	var vm = require('vm');
	vm.runInThisContext(code, rtl_func_file);

	var rtl_output = makeRTL(content);


	if(compile_less === false) {
		fs.writeFileSync(__dirname+'/../assets/css/ace-rtl.css' , rtl_output , 'utf-8');
	}
	else {
		var arg = require('argh').argv;//read & parse arguements
		
		var less = require('less');
		var parser = new(less.Parser)({
		  paths: [__dirname+'/../assets/css/less'], // Specify search paths for @import directives
		  //filename: '../assets/css/less/ace-rtl2.less' // Specify a filename, for better error messages
		});

		parser.parse(fs.readFileSync(__dirname+'/../assets/css/less/ace-rtl.less' , 'utf-8'), function (e, tree) {
		  if(e) {
			console.log(e);
			return;
		  }
		  rtl_output = rtl_output + "\n" + tree.toCSS();
		  /**if(arg['strip-media']) {
			var strip_func_file = __dirname+'/files/css-strip-media.js';
			var code = fs.readFileSync(strip_func_file , 'utf-8');
			var vm = require('vm');
			vm.runInThisContext(code, strip_func_file);

			rtl_output = remove_media_queries(rtl_output, 900);//keep `min-width` media queries which are >= 900px
		  }*/
		  fs.writeFileSync(__dirname+'/../assets/css/ace-rtl.css' , rtl_output , 'utf-8');
		});
	}
}
if(process.argv[1].match(/rtl\.js$/)) aceRTL(true);//if "node rtl.js" invoked



//this is used for grunt
if(typeof module !== 'undefined') {
 module.exports = function generateRTL(grunt) {
  aceRTL(false);
  var rtl_output = 
	  fs.readFileSync(__dirname+'/../assets/css/ace-rtl.css' , 'utf-8') 
	  + "\n" 
	  + fs.readFileSync(__dirname+'/../assets/css/ace-rtl.less.css' , 'utf-8');//generated in grunt less
	  
  fs.writeFileSync(__dirname+'/../assets/css/ace-rtl.css' , rtl_output , 'utf-8');
  fs.unlinkSync(__dirname+'/../assets/css/ace-rtl.less.css');

  grunt.log.writeln('RTL File ace-rtl.css created.');
 }
}