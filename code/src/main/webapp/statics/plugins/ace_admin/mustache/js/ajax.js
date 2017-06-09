var arg = require('argh').argv; //read & parse arguements
var engine_name = "hogan";//('engine' in arg && arg['engine'] == "mustache") ? "mustache" : "hogan";//hogan or mustache
var engine = require("hogan.js")//require(engine_name == "hogan" ? "hogan.js" : "mustache")
  , fs    = require('fs')
  , extend= require('xtend')
  , AutoLoader = require('./classes/autoload-'+engine_name+'.js');

var output_folder = 'output_folder' in arg ? arg['output_folder']+'/ajax' : 'output/ajax'

createFolder(output_folder+'/content');

function createFolder(path) {
 var path_parts = path.split(/[\\\/]/g);
 var new_folder = '';
 for(var p in path_parts) {
  var tmp = path_parts[p].replace(/^\s+/g, '').replace(/\s+$/g, '');
  new_folder = new_folder + tmp + "/";
  if( !fs.existsSync(__dirname+'/'+new_folder) ) fs.mkdir(__dirname+'/'+new_folder);
 }
}


var path = 
{
 data : __dirname + '/../app/data',
 views : __dirname + '/../app/views',
 base : '../..',
 assets : '../assets',
 components : '../components',
 images : '../../assets/images',
 minified: ''
}

for(var p in path) {
	if ('path_'+p in arg) path[p] = arg['path_'+p]
}

var site = JSON.parse(fs.readFileSync(path['data']+'/common/site.json' , 'utf-8'));//this site some basic site variables
site['protocol'] = 'http:'
//override config file with command line options
for(var k in site) {
	if (k in arg) site[k] = arg[k]
}
if(site['protocol'] == false) site['protocol'] = '';
site['ajax'] = true;


var Sidenav_Class = require('./classes/Sidenav')
var sidenav = new Sidenav_Class()

var Page_Class = require('./classes/Page')
var Indentation = require('./classes/Indent')
var autoload = new AutoLoader(engine , path);

if(site['development'] == true) {
 site['ace_scripts'] = [];
 var scripts = JSON.parse(fs.readFileSync(__dirname + '/../../assets/js/src/scripts.json' , 'utf-8'));
 scripts['ace.ajax-content.js'] = true;
 for(var name in scripts)
   if(scripts.hasOwnProperty(name) && scripts[name] == true) {
	 site['ace_scripts'].push(name);
   }
}



//iterate over all pages and generate the static html file
generate(null);
var page_views_folder = path["views"]+"/pages";
if(fs.existsSync(page_views_folder) && (stats = fs.statSync(page_views_folder)) && stats.isDirectory()) {
	var files = fs.readdirSync(page_views_folder)
	files.forEach(function (name) {
		var filename;//file name, which we use as the variable name
		if (! (filename = name.match(/(.+?)\.(mustache|html)$/)) ) return;
		var page_name = filename[1];
		
		generate(page_name);
	})
}


function generate(page_name) {
	if(page_name == null) {
		//the ajax layout page
		var layout_name = 'ajax-layout';
		var layout = new Page_Class( {'engine':engine, 'path':path, 'name':layout_name, 'type':'layout'} );
		layout.initiate();
		if(layout.get_var('sidebar_items'))
		{
			sidenav.set_items(layout.get_var('sidebar_items'));
			sidenav.mark_active_item(page_name);
		}
		

		var context = { "page":{} , "layout":layout.get_vars(), "path" : path , "site" : site }
		context['breadcrumbs'] = sidenav.get_breadcrumbs();

		context['createLinkFunction'] = function() {
			return function(text) {
				return '../'+text+'.html';
			}
		}
		context['createAjaxLinkFunction'] = function() {
			return function(text) {
				return '#page/'+text;
			}
		}

		autoload.set_params("" , layout_name);

		var rendered_output = engine_name == "hogan" ? layout.get_template().render(context) : (layout.get_template())(context)
		Indentation(rendered_output , site['onpage_help'], false, function(result) {
			var output_file = output_folder+'/index.html';
			fs.writeFileSync( __dirname + '/'+output_file , result, 'utf-8' );
			console.log(output_file);
		})
	
		return;
	}
	
	var items = sidenav.get_items();//list of available pages, some are not included such as "horizontal menu, etc ..."
	if( !(page_name in items) ) return;
	
	//other ajax content pages;
	var page = new Page_Class( {'engine':engine, 'path':path, 'name':page_name, 'type':'page'} );
	page.initiate(function() {
		var layout_name = 'ajax-content';
		var layout = new Page_Class( {'engine':engine, 'path':path, 'name':layout_name, 'type':'layout'} );
		layout.initiate();
		/**if(layout.get_var('sidebar_items'))
		{
			sidenav.set_items(layout.get_var('sidebar_items'));
			sidenav.mark_active_item(page_name);
		}*/


		var context = { "page":page.get_vars() , "layout":layout.get_vars(), "path" : path , "site" : site }
		//context['breadcrumbs'] = sidenav.get_breadcrumbs();
		context['createLinkFunction'] = function() {
			return function(text) {
				return '../'+text+'.html';
			}
		}
		context['createAjaxLinkFunction'] = function() {
			return function(text) {
				return '#page/'+text;
			}
		}


		autoload.set_params(page.get_name() , layout_name);

		var rendered_output = layout.get_template().render(context);//engine_name == "hogan" ? layout.get_template().render(context) : (layout.get_template())(context)
		Indentation(rendered_output , site['onpage_help'], true, function(result) {
			var output_file = output_folder+'/content/'+page_name+'.html';
			fs.writeFileSync( __dirname + '/'+output_file , result, 'utf-8' );
			console.log(output_file);
		})
	})
}
