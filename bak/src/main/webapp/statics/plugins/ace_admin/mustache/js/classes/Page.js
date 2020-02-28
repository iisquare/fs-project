//this is basic class for loading page/layout data from json or csv files

module.exports = function(params) {
 require('require-csv')//lightweight class for parsing possible csv data
 var fs = require("fs"),
	 engine = params["engine"]



 var $type = params["type"] || "page" //page or layout?
 var $name = params["name"] //page or layout name
 var $main_template = null
 var $partials = {} //will hold all compiled partials that will be passed onto the base Mustache template for further processing
 var $vars = {} //holds the variables and data that will be accessed in mustache via page.variableName or layout.variableName

 var $data_dir = ''
 var $views_dir = ''

 this.initiate = function(callback) {//reading and initiate page data from json file
    $data_dir = params.path['data'];
	var filename = $data_dir +"/"+$type+"s/"+ $name + '.json';
	var data = fs.readFileSync(filename, 'utf-8')
	$vars = JSON.parse(data);
	
	if('alias' in $vars) {
		$name = $vars['alias'];
		filename = $data_dir +"/"+$type+"s/"+ $name + '.json';
		data = fs.readFileSync(filename, 'utf-8')
		$vars = extend(JSON.parse(data), $vars)
	}

	
	if($type != "layout" && !("layout" in $vars)) $vars["layout"] = "default";

	map_script_names();
	//now load all data relating to this page
	load_data($data_dir)

	var wait_for_compression = false;

	//load inline scripts and styles
	$views_dir = params.path['views'];
	if($type == 'page') {
		if(fs.existsSync($views_dir + '/assets/scripts/' + $name + '.js')) {
			/**
			if(params.compressor) {
				wait_for_compression = true;
				params.compressor.compress($views_dir + '/assets/scripts/' + $name + '.js', {
					charset: 'utf8',
					type: 'js'
				}, function(err, data, extra) {
					if(err) console.log(err);
					$vars['inline_scripts'] = data;
					callback.call();
				});
			}
			else
			*/
			$vars['inline_scripts'] = (fs.readFileSync($views_dir + '/assets/scripts/' + $name + '.js' , 'utf-8')).replace(/\{\{\{path\.(\w+)\}\}\}/ig , function(text, a1){ return params.path[a1]; });
		}
		if(fs.existsSync($views_dir + '/assets/styles/' + $name + '.css')) {
			$vars['inline_styles'] = fs.readFileSync($views_dir + '/assets/styles/' + $name + '.css' , 'utf-8')
		}
	}

	if( !wait_for_compression && typeof callback === "function" ) callback.call();

 }


 /**
  This functions loads the data related to each page or layout
  for a page we load all files in the related directory
  for a layout we check the "datas-sources" attribute of our layout_file.json and load them
  In your real world application you can load data only when it is needed according to each page inside controllers
 */
 var load_data = function(data_dir) {
	var data_files = {}

	var format = new RegExp("([0-9a-z\\-_]+)\\.(json|csv)$" , "i");

	//see if there's a folder for partial data relating to this page or layout
	//if so iterate all json/csv files and load the data
	var partial_data_folder = data_dir + "/"+$type +"s/partials/" + $name;
	var stats;
	if(fs.existsSync(partial_data_folder) && (stats = fs.statSync(partial_data_folder)) && stats.isDirectory()) {
		var files = fs.readdirSync(partial_data_folder)
		files.forEach(function (name) {
			var filename;//file name, which we use as the variable name
			if (! (filename = name.match(format)) ) return;
			data_files[filename[1]] = partial_data_folder + "/" + name;
		})
	}


	for(var var_name in data_files) if(data_files.hasOwnProperty(var_name)) {

		var new_data
		try {
			if(data_files[var_name].match(/\.json$/i)) {
				new_data = fs.readFileSync(data_files[var_name] , 'utf-8');
				new_data = JSON.parse(new_data);
			} else if(data_files[var_name].match(/\.csv$/i)) {
				//load csv data into an array
				var csv_data = require(data_files[var_name]);
				
				//now convert it to json
				var csv_header = csv_data[0];
				var length = csv_data.length;

				var json_data = []
				for(var i = 1 ; i < length; i++) {
					var csv_row = csv_data[i];
					var json_row = {}
					for(var j = 0; j < csv_row.length; j++) {
						json_row[csv_header[j]] = csv_row[j];
					}
					//for example if we have "status":"pending" , add this to it as well : "pending":true
					if("status" in json_row) json_row[json_row["status"].toLowerCase()] = true;
					json_data.push(json_row);
				}
				
				new_data = json_data
			}
		} catch(e) {
			console.log("Invalid JSON Data File : " + data_files[var_name]);
			continue;
		}
		
		$vars[var_name.replace(/\./g , '_')] = new_data
		//here we replace '.' with '_' in variable names, so template compiler can recognize it as a variable not an object
		//for example change sidebar.navList to sidebar_navList , because sidebar is not an object

	}


  return true;
 }


 //in our page's data file, we save short script/style names, which we must map to original file names
 //this way we can easily only change the script-mapping file and changes will be reflected
 var map_script_names = function() {
	var mappables = ["script" , "style"]
	for(var m in mappables) {
	  var which = mappables[m]
	  if($vars[which+'s']) {//if we have $vars["scripts"] or $vars["styles"]
		var page_scripts = $vars[which+'s'];
		var map_json = JSON.parse(fs.readFileSync($data_dir + "/common/"+which+"-mapping"+(params.path['minified'] ? '.min' : '')+".json"));
		var mapped_scripts = [];
		for(var i in page_scripts) {
			if(page_scripts[i] in map_json) {
				if(typeof map_json[page_scripts[i]] == "string") mapped_scripts.push(map_json[page_scripts[i]])
				else if(typeof map_json[page_scripts[i]] == "object") //two or more scripts should be included for this to work, like dataTables, jQuery UI, etc ...
				{
					for(var m in map_json[page_scripts[i]])
						mapped_scripts.push(map_json[page_scripts[i]][m])
				}
			}
		}
		$vars[which+'s'] = mapped_scripts;

		if(('ie_'+which+'s') in $vars) {
			var ie_scripts = $vars['ie_'+which+'s'];//ie_scripts || ie_styles
			var ie_mapped_scripts = [];
			for(var i in ie_scripts) if(map_json[ie_scripts[i].name])
			{
				ie_mapped_scripts.push({"version" : ie_scripts[i].version , "file_name" : map_json[ie_scripts[i].name]})
			}

			$vars['ie_'+which+'s'] = ie_mapped_scripts
		}
	  }
	}
 }
 






 this.get_template = function() {
	var template_file = $views_dir+"/"+$type+"s/"+$name + '.mustache';
	if(fs.existsSync(template_file)) {
		return params.engine.compile(fs.readFileSync(template_file , 'utf-8'));
	} 
	return null;
 }
 this.get_vars = function() {
	return $vars;
 }
 this.get_var = function(name , undefined) {
	return name in $vars ? $vars[name] : undefined;
 }
 this.get_name= function() {
	return $name;
 }

 
}
