angular.module('AceApp')
.constant('SAVE_SETTING', true)
.controller('MainController', function ($scope, $rootScope, $http, $timeout, $location/**, cfpLoadingBar*/) {

	//some general variables
	$scope.ace = $scope.ace || {};
	$scope.ace.path = {
		'assets': '../assets' //used in page templates when linking to images, etc
	};

	$scope.ace.site = {
		brand_text : 'Ace Admin',
		brand_icon : 'fa fa-leaf',
		version : '1.4'
	};

	//sidebar variables
	$scope.ace.sidebar = {
		'minimized': false,//used to collapse/expand
		'toggle': false,//used to toggle in/out mobile menu
		'reset': false//used to reset sidebar (for sidebar scrollbars, etc)
	};

	
	//viewContentLoading is used in angular/views/index.html to show/hide content and progress bar (spinner icon) when loading new pages
	$rootScope.viewContentLoading = false;
	$rootScope.$on('$stateChangeStart', function(event) {
		//cfpLoadingBar.start();
		$rootScope.viewContentLoading = true;
		
		//also hide sidebar in mobile view when navigating to a new state
		$scope.ace.sidebar.toggle = false;
	});
	$rootScope.$on('$stateChangeSuccess', function(event){ 
		//cfpLoadingBar.complete();
		$rootScope.viewContentLoading = false;
	});
	$rootScope.$on('$stateChangeError', function(event, p1, p2, p3){ 
		//cfpLoadingBar.complete();
		$rootScope.viewContentLoading = false;
	});
	
	
	//this function is used in body's ng-class directive to determine and apply selected user skin!
	$scope.bodySkin = function() {
		var skin = $scope.ace.settings.skinIndex;
		if( skin == 1 || skin == 2 ) return 'skin-'+skin;
		else if( skin == 3 ) return 'no-skin skin-3';
		return 'no-skin';
	};
	
	/////

	
	//in templates with use 'getData' to retrieve data dynamically and cache them for later use! data is located inside 'angular/data' folders
	//you don't need this and it's only a convenience for this demo
	//example: getData('comments', 'dashboard')
	
	$rootScope.appData = $rootScope.appData || {};
	$rootScope.appDataRequest = {};
	$rootScope.getData = function(dataName, type) {
		var type = type || 'page';
		var dataKey = null, dataPath = null;
		if(type == 'page') {
			var pageName = $location.path().match(/([\-a-z]+)$/)[0];
			dataKey = 'page-'+pageName+'-'+dataName;
			dataPath = 'data/pages/'+pageName+'/'+dataName+'.json';
		}
		else {
			dataKey = type+'-'+dataName;
			dataPath = 'data/'+type+'/'+dataName+'.json';
		}
		
		if (!dataPath) return;
		if (dataKey in $rootScope.appData) return $rootScope.appData[dataKey];
		
		if( !(dataKey in $rootScope.appData) && !(dataKey in $rootScope.appDataRequest) ) {
			$rootScope.appDataRequest[dataKey] = true;
			
			$http.get(dataPath).success(function(data) {
				$rootScope.appData[dataKey] = data;
			});
		}
	};
	$rootScope.getCommonData = function(dataName) {
		return $rootScope.getData(dataName, 'common');
	};
	

});


//This controller responds to settings box changes
angular.module('AceApp').controller('SettingsCtrl', function ($scope, $timeout, SAVE_SETTING, $localStorage, StorageGet) {
	$scope.ace = $scope.$parent.ace;
	$scope.ace.settings = $scope.ace.settings || {};
	
	if(SAVE_SETTING) $localStorage['ace.settings'] = $localStorage['ace.settings'] || {};

	$scope.ace.settings = {
		'is_open': false,
		'open': function() {
			$scope.ace.settings.is_open = !$scope.ace.settings.is_open;
		},
		
		'navbar': false,
		'sidebar': false,
		'breadcrumbs': false,
		'hover': false,
		'compact': false,
		'highlight': false,
		
		//'rtl': false,
		
		'skinColor': '#438EB9',
		'skinIndex': 0
	};
	

	if(SAVE_SETTING) StorageGet.load($scope, 'ace.settings');//load previously saved setting values
	
	
	//watch some of the changes to trigger related events required by sidebar, etc
	$scope.$watch('ace.settings.navbar', function(newValue) {
		if(newValue == false) {
			//if navbar is unfixed, so should be sidebar and breadcrumbs
			$scope.ace.settings.sidebar = $scope.ace.settings.breadcrumbs = false;
		}
		$timeout(function() {
			if(jQuery) jQuery(document).trigger('settings.ace', ['navbar_fixed' , newValue]);
		});
		
		if(SAVE_SETTING) $localStorage['ace.settings']['navbar'] = newValue;
	});
	$scope.$watch('ace.settings.sidebar', function(newValue) {
		if(newValue === true) {
			//if sidebar is fixed, so should be navbar
			$scope.ace.settings.navbar = true;
		}
		else if(newValue === false) {
			//if sidebar is unfixed, so should be breadcrumbs
			$scope.ace.settings.breadcrumbs = false;
		}
		$timeout(function() {
			if(jQuery) jQuery(document).trigger('settings.ace', ['sidebar_fixed' , newValue]);
		});
		
		if(SAVE_SETTING) $localStorage['ace.settings']['sidebar'] = newValue;
	});
	$scope.$watch('ace.settings.breadcrumbs', function(newValue) {
		if(newValue === true) {
			//if breadcrumbs is fixed, so should be sidebar
			$scope.ace.settings.sidebar = true;
		}
		$timeout(function() {
			if(jQuery) jQuery(document).trigger('settings.ace', ['breadcrumbs_fixed' , newValue]);
		});
		
		if(SAVE_SETTING) $localStorage['ace.settings']['breadcrumbs'] = newValue;
	});
	$scope.$watch('ace.settings.container', function(newValue) {
		$timeout(function() {
			if(jQuery) jQuery(document).trigger('settings.ace', ['main_container_fixed' , newValue]);
		});
		
		if(SAVE_SETTING) $localStorage['ace.settings']['container'] = newValue;
	});
	
	//////
	$scope.$watch('ace.settings.compact', function(newValue) {
		if(newValue === true) {
			//if sidebar is compact, it should be in 'hover' mode as well
			$scope.ace.settings.hover = true;
		}
		$timeout(function() {
			//reset sidebar scrollbars and submenu hover
			$scope.$parent.ace.sidebar.reset = true;
		} , 500);
		
		if(SAVE_SETTING) $localStorage['ace.settings']['compact'] = newValue;
	});
	$scope.$watch('ace.settings.hover', function(newValue) {
		if(newValue === false) {
			//if sidebar is not 'hover' , it should not be compact
			$scope.ace.settings.compact = false;
		}
		$timeout(function() {
			//reset sidebar scrollbars and submenu hover
			$scope.$parent.ace.sidebar.reset = true;
		} , 500);
		
		if(SAVE_SETTING) $localStorage['ace.settings']['hover'] = newValue;
	});
	$scope.$watch('ace.settings.highlight', function(newValue) {
		if(SAVE_SETTING) $localStorage['ace.settings']['highlight'] = newValue;
	});
	
	////
	
	$scope.$watch('ace.settings.skinIndex', function(newValue) {
		//save skinIndex for later
		if(SAVE_SETTING) $localStorage['ace.settings']['skinIndex'] = newValue;
	});

});


//Sidebar Controller
app.controller('SidebarCtrl', function($scope, $state, $rootScope, $timeout, SidebarList, SAVE_SETTING, $localStorage, StorageGet) {
  $scope.ace = $scope.$parent.ace;
  $scope.ace.sidebar = $scope.ace.sidebar || {};
  
  if(SAVE_SETTING) {
	StorageGet.load($scope, 'ace.sidebar');//load previously saved sidebar properties
	$scope.$watch('ace.sidebar.minimized', function(newValue) {
		$localStorage['ace.sidebar']['minimized'] = newValue;
	});
  }
  
  
  ////
  //make a list of sidebar items using router states in angular/js/app.js
  $scope.sidebar = SidebarList.getList( $state.get() );

  //these are used to determine if a sidebar item is 'open' or 'active'
  $rootScope.subMenuOpen = {};
  $rootScope.isSubOpen = function(name) {
	if( !(name in $rootScope.subMenuOpen) ) $rootScope.subMenuOpen[name] = false;
	return $rootScope.subMenuOpen[name];
  }
  $rootScope.isActiveItem = function(name) {
	return $rootScope.activeItems ? $rootScope.activeItems[name] : false;
  }

});


//nothing important, just a snippet to convert ui.router states into an array of sidebar items to be used in the partial template (sidebar.html)
//make a list of sidebar items using router states in angular/js/app.js
app.service('SidebarList', function() {
	//parent name for a state
    var getParentName = function(name) {
		var name = (/^(.+)\.[^.]+$/.exec(name) || [null, null])[1];
		return name;
    };
	//how many parents does this state have?
	var getParentCount = function(name) {
		return name.split('.').length;
	};
	
	this.getList = function(uiStateList) {

	  var sidebar = {'root': []};//let's start with root and call it root! (see views/layouts/default/partial/sidebar.html)
	  var parentList = {};//each node(item) can be a parent, so we add it to this list, and later if we find its children we know where to find the parent!

	  for(var i = 0 ; i < uiStateList.length ; i++) {
		var state = uiStateList[i];
		if(!state.name) continue;
		
		//copy state to 'item' (so state is not changed)
		var item = {};
		angular.copy(state, item);
		delete item['resolve']; delete item['templateUrl'];//delete these, we don't need them
		
		//item.name is state's name (dashboard, ui.elements, etc)
		item.url = item.name || '';
		
		parentList[item.name] = item;//save this item as a possible parent, and later we add possible children to it as submenu
		
		var parentName = getParentName(item.name);
		if(!parentName) {
			//no parent, so a root item
			sidebar.root.push(item);
			item['level-1'] = true;
		}
		else {
			//get the parent and add this item as a submenu element of parent
			var parentNode = parentList[parentName];
			if ( !('submenu' in parentNode) ) parentNode['submenu'] = [];
			parentNode['submenu'].push(item);
			item['level-'+getParentCount(item.name)] = true;
		}
	  }
	  
	  parentList = null;

	  return sidebar;
	};
	
});

//just load localStorage stored values, such as ace.settings, or ace.sidebar
app.service('StorageGet', function($localStorage) {
	
	this.load = function($scope, name) { 
	  $localStorage[name] = $localStorage[name] || {};
	  
	  var $ref = $scope;
	  var parts = name.split('.');//for example when name is "ace.settings" or "ace.sidebar"
	  for(var i = 0; i < parts.length; i++) $ref = $ref[parts[i]];
	  //now $ref refers to $scope.ace.settings
	  
	  for(var prop in $localStorage[name]) if($localStorage[name].hasOwnProperty(prop)) {
		$ref[prop] = $localStorage[name][prop];
	  }
	};
	
});