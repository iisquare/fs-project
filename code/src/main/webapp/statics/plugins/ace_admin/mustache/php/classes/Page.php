<?php
//this is basic class for loading page/layout data/templates from json or csv files
//usually you have a Controller which grabs needed data from database using models, etc and passes it onto the view/template files
class Page {

 private $type;
 private $name;
 private $vars; //holds the variables and data that will be accessed in mustache via page.variableName or layout.variableName

 private $data_dir;
 private $views_dir;

 public function __construct(array $options = array())
 {
	$this->type = isset($options['type']) ? $options['type'] : 'page'; //page or layout?
	$this->name = $options["name"]; //page or layout name
  
	$this->data_dir = $options['path']['data'];
	$filename = "{$this->data_dir}/{$this->type}s/{$this->name}.json";
	$data = file_get_contents($filename);
	$this->vars = json_decode($data , TRUE);

	if(isset($this->vars['alias'])) {
		$this->name = $this->vars['alias'];
		$filename = "{$this->data_dir}/{$this->type}s/{$this->name}.json";
		$data = file_get_contents($filename);
		$this->vars = array_merge(json_decode($data , TRUE) , $this->vars);
	}

	if($this->type != "layout" && !isset($this->vars["layout"]))//if no default layout for this page, then consider it to be "default"
		$this->vars["layout"] = "default";

	$this->views_dir = $options['path']['views'];
	if($this->type == 'page') {
		if(is_file("{$this->views_dir}/assets/scripts/{$this->name}.js")) {
			//replace path.assets etc in scripts with their value
			$this->vars['inline_scripts'] = str_ireplace( array_map(function($key) {return '{{{path.'.$key.'}}}';}, array_keys($options['path'])), array_values($options['path']), file_get_contents("{$this->views_dir}/assets/scripts/{$this->name}.js") );
		}
		if(is_file("{$this->views_dir}/assets/styles/{$this->name}.css")) {
			$this->vars['inline_styles'] = file_get_contents("{$this->views_dir}/assets/styles/{$this->name}.css");
		}
	}


	$this->map_script_names();

	//now load all data relating to this page
	$this->load_data($this->data_dir);
 }


 /**
  This functions loads the data related to each page or layout
  for a page we load all files in the related directory
  for a layout we check the "datas-sources" attribute of our layout_file.json and load them
  In your real world application you can load data only when it is needed according to each page inside controllers
 */
 private function load_data($data_dir) {

	$data_files = array();
	$format = "/([0-9a-z\\-_]+)\\.(json|csv)$/i";

	//see if there's a folder for partial data relating to this page or layout
	//if so iterate all json/csv files and load the data
	$partial_data_folder = "{$data_dir}/{$this->type}s/partials/{$this->name}";
	$stats = null;
	if(is_dir($partial_data_folder)) {
		$files = scandir($partial_data_folder);
		foreach($files as $name) {
			if($name == '.' OR $name == '..')continue;

			if (! preg_match($format, $name , $filename) ) continue;
			$data_files[$filename[1]] = "{$partial_data_folder}/{$name}";
		}
	}


	foreach($data_files as $var_name => $var_value) {
			$new_data = '';
			if(preg_match("/\.json$/i" , $data_files[$var_name])) {
				$new_data = json_decode(file_get_contents($data_files[$var_name]) , TRUE);
			} else if(preg_match("/\.csv$/i" , $data_files[$var_name])) {
				//load csv data into an array
				$new_data = CSV::to_array($data_files[$var_name]);
				foreach($new_data as &$data) if(isset($data['status'])) $data[$data['status']] = true;
			}

			$this->vars[str_replace('.' , '_', $var_name)] = $new_data;
			//here we replace '.' with '_' in variable names, so template compiler can recognize it as a variable not an object
			//for example change sidebar.navList to sidebar_navList , because sidebar is not an object
	}

	return true;
 }

 private function map_script_names() {
	$vars = &$this->vars;
	
	//in our page's data file, we save short script/style names, which we must map to original file names
	//this way we can easily only the script-mapping file and changes will be reflected
	$mappables = array("script" , "style");
	foreach($mappables as $which) {
	  if(isset($vars[$which.'s'])) {//if we have $vars["scripts"] or $vars["styles"]
		$page_scripts = $vars[$which.'s'];
		$map_json = json_decode(file_get_contents("{$this->data_dir}/common/{$which}-mapping.json") , TRUE);
		$mapped_scripts = array();
		foreach($page_scripts as $i => $script) {
			if(isset($map_json[$page_scripts[$i]])) {
				if(is_string($map_json[$page_scripts[$i]])) $mapped_scripts[] = $map_json[$page_scripts[$i]];
				else //if(is_object($map_json[$page_scripts[$i]])) //two or more scripts should be included for this to work, like dataTables, jQuery UI, etc ...
				{
					foreach($map_json[$page_scripts[$i]] as $script_item)
						$mapped_scripts[] = $script_item;
				}
			}
		}
		$vars[$which.'s'] = $mapped_scripts;
		
		if(isset($vars['ie_'.$which.'s'])) {
			$ie_scripts = $vars['ie_'.$which.'s'];//ie_scripts || ie_styles
			$ie_mapped_scripts = array();
			foreach($ie_scripts as $script)
				if(isset($map_json[$script['name']]))
					$ie_mapped_scripts[] = array("version" => $script['version'] , "file_name" => $map_json[$script['name']]);

			$vars['ie_'.$which.'s'] = $ie_mapped_scripts;
		}
	  }
	}
 }
 


 


 public function get_template() {
	return file_get_contents("{$this->views_dir}/{$this->type}s/{$this->name}.mustache");
 }
 public function get_vars() {
	return $this->vars;
 }
 public function &get_var($name) {
	if(!isset($this->vars[$name])) {
		$result = null;
		return $result;
	}
	return $this->vars[$name];
 }
 public function get_name() {
	return $this->name;
 }

}
