//this defines a simple model class for loading basic page data which is read from a json file
var fs = require('fs');
module.exports = function() {
 var $navList = null;
 var $active_page = null;
 var $breadcrumbs = {};
 var nav_items = {}


 this.mark_active_item = function(active_page) {
	$active_page = active_page;
	$breadcrumbs = {};
	if($navList) {
		this.mark_items($navList , 1);
		if('links' in $breadcrumbs && 'reverse' in $breadcrumbs['links']) $breadcrumbs['links'].reverse();
	}
 }
 this.set_items = function(navList) {
	$navList = navList;
 }
 this.get_items = function() {
	return nav_items;
 }
 

 this.mark_items = function(navList , level) {
	this.reset_items(navList);

	var ret = false;
	for(var i = 0 ; i < navList.length; i++)
	{
	 var item = navList[i];


	 item['icon'] = item['icon'] || false;
	 //if there is no icon for this item, we set it as false
	 //otherwise in recursive modes it will lookup the parent's scope and use that one's icon (Mustache only)

	 item['class'] = item['class'] || false;//same as above
	 item['submenu?'] = ('submenu' in item);//same as above
	 item['badge'] = item['badge'] || false;//same as above
	 item['label'] = item['label'] || false;//same as above
	 
	 item['level-'+level] = true;//in printing out menu items, we use a partial recursively, but we have different needs for level-1 & level-2, etc items ... so we should know on what level we are
	 for(var l = level - 1 ; l > 0 ; l--) item['level-'+l] = false;
	 //why? because when we have "level-2"=true  then parent's "level-1" will also be true,
	 //and mustache looks up the parent context's value as well, so the part between {{#level-1}}{{/level-1}} will be
	 //executed even when we are printing level-2 menu items (i.e submenu) recursively
	 //see views/layouts/partials/default/sidenav/items.mustache
	 //maybe using something like handlebars with a little bit of logic is better here
	 //or perhaps not using recursive partials, and a different partial for each submenu level

	 if($active_page != null && $active_page == item['link'])
	 {
		item['class'] = 'active';
		if(item['submenu?']) item['class'] += ' no-active-child';//rare case, it means that this item has been directly selected, not a child of it, so it's not opened and take care of the caret using "no-active-child" class
		
		$breadcrumbs['title'] = item['title'];//add the active page's title to breadcrumb object
		ret = true;//this item is active so we return true
	 }//if current page
	 
	 if(item['submenu?'])
	 {
		isOpen = this.mark_items(item['submenu'] , level+1);
		if(isOpen)
		{
			item['class'] = 'active open';//an item in the submenu of this item is active, so set this as "active" & "open"
			
			//create the array if it doesn't exist
			$breadcrumbs['links'] = $breadcrumbs['links'] || [];
			//add the parent of this active submenu item to the breadcrumbs list
			$breadcrumbs['links'].push({
				'link': item['link'] || '#',
				'title': item['title']
			});

			ret = true;
		}
	 }//it has submenu
	 
	 if(item['link'] != null) nav_items[item['link']] = true;
	}//for
	
	return ret;
 }

 
 //this navList is used multiple times to produce multiple pages in one place
 //therefore we need to reset the classes we assigned to previously "active" or "open" items and recalculate
 this.reset_items = function(navList) {
	for(var i in navList)
	{
		if(!navList.hasOwnProperty(i)) continue;
		var item = navList[i];
		item['class'] = false;
		
		if('submenu' in item)
		{
			this.reset_items(item['submenu']);
		}
	}
 }



 this.get_breadcrumbs = function() {
	return $breadcrumbs
 }

}