<?php
//this defines a simple model class for loading basic page data which is read from a json file
class Sidenav {
 private $navList = null;
 private $active_page = null;
 private $breadcrumbs;


 public function mark_active_item($active_page) {
	$this->active_page = $active_page;
	$this->breadcrumbs = array();
	if($this->navList) {
		$this->mark_items($this->navList , 1);
		if( isset($this->breadcrumbs['links']) && !empty($this->breadcrumbs['links']) )
			$this->breadcrumbs['links'] = array_reverse($this->breadcrumbs['links']);
	}
 }
 public function set_items(&$navList) {
	$this->navList = &$navList;
 }


 private function mark_items(&$navList, $level) {
	$this->reset_items($navList);

	$ret = false;
	foreach($navList as &$item)
	{
	 $item['icon'] = isset($item['icon']) ? $item['icon'] : false;
	 //if there is no icon for this item, we set it as false
	 //otherwise in recursive modes it will lookup the parent's scope and use that one's icon (Mustache only)
	 
	 $item['class'] = isset($item['class']) ? $item['class'] : false;//same as above
	 $item['submenu?'] = isset($item['submenu']);//same as above
	 $item['label'] = isset($item['label']) ? $item['label'] : false;
	 $item['badge'] = isset($item['badge']) ? $item['badge'] : false;
	 
	 $item['level-'.$level] = true;//in printing out menu items, we use a partial recursively, but we have different needs for level-1 & level-2, etc items ... so we specify this data with each item
	 for($l = $level - 1 ; $l > 0 ; $l--) $item['level-'.$l] = false;
	 //why? because when we have "level-2"=true  then parent's "level-1" will also be true,
	 //and mustache looks up the parent context's value as well, so the part between {{#level-1}}{{/level-1}} will be
	 //executed even when we are printing level-2 menu items (i.e submenu) recursively
	 //see views/layouts/partials/default/sidenav/items.mustache
	 //maybe using something like handlebars with a little bit of logic is better here
	 //or perhaps not using recursive partials, and a different partial for each other

	 if($this->active_page != null && $this->active_page == $item['link'])
	 {
		$item['class'] = 'active';
		if($item['submenu?']) $item['class'] .= ' no-active-child';//rare case, it means that this item has been directly selected, not a child of it, so it's not opened and take care of the caret using "no-active-child" class

		$this->breadcrumbs['title'] = $item['title'];//add the active page's title to breadcrumb object
		$ret = true;//this item is active so we return true
	 }//if current page

	 if($item['submenu?'])
	 {
		$isOpen = $this->mark_items($item['submenu'] , $level+1);
		if($isOpen)
		{
			$item['class'] = 'active open';//an item in the submenu of this item is active, so set this as "active" & "open"

			//create the array if it doesn't exist
			if(!isset($this->breadcrumbs['links'])) $this->breadcrumbs['links'] = array();
			//add the parent of this active submenu item to the breadcrumbs list
			$this->breadcrumbs['links'][] = array('link' => $item['link'] ? '?page='.$item['link'] : '#' , 'title'=>$item['title']);
			
			$ret = true;
		}
	 }//it has submenu
	}//for
	
	return $ret;
 }


 //this navList is used multiple times to produce multiple pages in one place
 //therefore we need to reset the classes we assigned to previously "active" or "open" items and recalculate
 private function reset_items(&$navList) {
	foreach($navList as &$item)
	{
		$item['class'] = false;
		if(isset($item['submenu']))
		{
			$this->reset_items($item['submenu']);
		}
	}
 }



 public function get_breadcrumbs() {
	return $this->breadcrumbs;
 }

}