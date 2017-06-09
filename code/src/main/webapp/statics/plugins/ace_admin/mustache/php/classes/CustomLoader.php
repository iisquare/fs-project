<?php
/**
* Mustache Template Custom Loader.
*
* A CustomLoader extends the normal FilesystemLoader with the ability to find templates inside specific layout and page directories.
* @extends Mustache_Loader_FilesystemLoader
*/
class CustomLoader extends Mustache_Loader_FilesystemLoader
{
private $page;
private $layout;
private $baseDir;

public function __construct($baseDir, array $options = array())
{
 $this->page_name = isset($options['page']) ? $options['page'] : 'index';
 $this->layout_name = isset($options['layout']) ? $options['layout'] : 'default';
 $this->baseDir = $baseDir;

 parent::__construct($baseDir, $options);
}


public function load($name)
{
 //the template name is something like "page.index" or "layout.sidenav.items"
 //we check whether it's a layout or page , and depending on $this->page_name OR $this->layout_name we find the appropriate partial file
 $parts = explode('.', $name, 2);
 $type = $parts[0];
 if($type != 'page' AND $type != 'layout') return '';

 $file = str_replace('.' , '/' , $parts[1]);
 if($type == 'page' AND $file == 'content') return parent::load("/{$type}s/{$this->page_name}.mustache");

 $item_name = $type.'_name';
 $item_name = $this->$item_name;//page_name OR layout_name , value of current page or layout e.g. default, login | index, widgets, etc ...

 //look in the partials for this page or layout
 $path = "/{$type}s/partials/{$item_name}/{$file}.mustache";
 if(!is_file($this->baseDir.$path)) {
	//look in the upper folder, which contains partials for all pages or layouts
	$path = "/{$type}s/partials/_shared/{$file}.mustache";
	if(!is_file($this->baseDir.$path)) return '';
 }

 return parent::load($path);//we don't use $this->baseDir.$path , because baseDir has already been passed onto parent(Mustache_Loader_FilesystemLoader)
}



}