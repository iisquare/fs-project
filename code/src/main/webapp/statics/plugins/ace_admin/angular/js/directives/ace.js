
angular.module('ace.directives', ['ui.router'])
.directive('uiBreadcrumb', ['$rootScope', '$state', function($rootScope, $state) {
	return {
		restrict: 'AE',
		replace: true,
		scope: {
			home: '='
		},
		template: '<ul class="breadcrumb">' +
            '<li><i class="ace-icon {{home}} home-icon"></i> <a href="">Home</a></li> ' +
            '<li ng-repeat="item in breadcrumbs.list"><a ng-if="item.url" ui-sref="{{item.url}}" ng-bind="item.title"></a><a ng-if="!item.url" href="" ng-bind="item.title"></a></li> ' +
            '<li class="active" ng-bind="breadcrumbs.lastTitle"></li>' +
            '</ul>',
		link: function (scope) {
			var getParentName = function(name) {
				var name = (/^(.+)\.[^.]+$/.exec(name) || [null, null])[1];
				return name;
			}
			
			$rootScope.$on('$stateChangeSuccess', function(event, toState, toParams, fromState, fromParams) {
				//list of breadcrumb items
				var breadcrumbs = {
					list: [],
					lastTitle: toState.title
				};
				
				
				var activeItems = {};//list of active sidebar items

				
				var currentName = toState.name;
				activeItems[currentName] = true;
				//find parents and add them to breadcrumbs, activeItems and subMenuOpen list
				while( (currentName = getParentName(currentName)) ) {
					var state = $state.get(currentName);
					activeItems[currentName] = true;
					breadcrumbs.list.push({title: state.title, url: state.abstract ? false : state.url});
					
					$rootScope.subMenuOpen[currentName] = true;
					//$rootScope.subMenuCollapsed[currentName] = false;
				}
				
				breadcrumbs.list.reverse();
				scope.breadcrumbs = breadcrumbs;
				
				$rootScope.pageTitle = toState.title;//page title
				$rootScope.activeItems = activeItems;
			});

		}
	};
}])

.directive('aceScroll', [function() {
	return {
		restrict: 'A',
		scope: {
		  options: '='
		},
		link: function (scope, element, attributes) {
			var scroller = null;

			scope.$watch('options', function() {
				if(scroller == null) {
					scroller = jQuery(element).ace_scroll(scope.options).ace_scroll('ref');
				}
				else {
					scroller.update(scope.options);
					scroller.reset();
				}
			}, true);
		}
	};
}])


.directive('widgetBox', ['$timeout', '$localStorage', function($timeout, $localStorage) {
	return {
	  restrict: 'A',

	  scope: {
		wbReloading:'=?',
		wbFullscreen:'=?',
		wbClosed:'=?',
		
		wbHidden:'=?',
		wbToggle:'=?',
		
		
		onReload: '&',
		onClose: '&',
		onFullscreen: '&',
		onShow: '&',
		onHide: '&',
		onToggle: '&',
		
		onReloaded: '&',
		onClosed: '&',
		onFullscreened: '&',
		onShown: '&',
		onHidden: '&',
		onToggled: '&',
		
		save: '=?',
		saveName: '=?'
	  },

	  link: function ($scope, element, attributes) {
			element.addClass('widget-box');
			
			
			$scope.wbReloading = $scope.wbReloading || false;
			$scope.wbFullscreen = $scope.wbFullscreen || false;
			$scope.wbClosed = $scope.wbClosed || false;
			$scope.wbHidden = $scope.wbHidden || false;
			$scope.wbToggle = $scope.wbToggle || !$scope.wbHidden;
			
		

			var loopUpdate = {'reload': false, 'close': false, 'fullscreen': false, 'toggle': false};
			var beforeEvents = 
			{
			 'reload': function() {
				//reset 'wbReloading' to 'true' before each reload				
				$scope.wbReloading = true;
			  },
			 'close': function() {
				$scope.wbClose = true;
			 }, 
			 'fullscreen': function() {
				$scope.wbFullscreen = !$scope.wbFullscreen;
			 },
			 'show': function() {
				$scope.wbHidden = false;
			 },			 
			'hide': 1,
			'toggle': function() {
				$scope.wbToggle = !$scope.wbToggle;
			}
			};
			
			var afterEvents = 
			{
			 'reloaded': 1,
			 'closed': 1,
			 'fullscreened': 1,
			 'hidden': function() {
				$scope.wbHidden = true;
			 },
			 'shown': 1,
			 'toggled': 1
			};

			for(var ev_name in beforeEvents) {
				(function() {
					var beforeName = ev_name, beforeCallback = 'on'+beforeName.replace(/^[a-z]/, function($1) { return $1.toUpperCase() });	
					element.on(beforeName+'.ace.widget', function(ev) {
						if(ev.namespace != 'ace.widget') return;
						
						if ( $scope[beforeCallback] && $scope[beforeCallback]() === false ) {
							ev.preventDefault();
							return;
						}
						
						if(angular.isFunction(beforeEvents[beforeName])) {
							loopUpdate[beforeName] = true;
							$scope.$apply(function() {
								beforeEvents[beforeName]();
							});
						}
					});
				})();
			}
			
			for(var ev_name in afterEvents) {
				(function() {
					var afterName = ev_name, afterCallback = 'on'+afterName.replace(/^[a-z]/, function($1) { return $1.toUpperCase() });	
					element.on(afterName+'.ace.widget', function(ev) {
						if(ev.namespace != 'ace.widget') return;
						
						if ( $scope[afterCallback] && $scope[afterCallback]() === false ) {
							ev.preventDefault();
						}
						if(angular.isFunction(afterEvents[afterName])) {
							$scope.$apply(function() {
								afterEvents[afterName]();
							});
						}
					});
				})();
			}
			
			
			////////////
			var widget_id = element.attr('id') || '';
			
			var save = !!(($scope.save || false) && widget_id);
			var saveName = $scope.saveName || 'ace.widget-state';
			saveName += ('-' + widget_id);
			$localStorage[saveName] = $localStorage[saveName] || {};
			
			
			var storage = $localStorage[saveName];
			if (save && storage) {
				if('toggle' in storage) {
					$scope.wbToggle = storage['toggle'];
					$scope.wbHidden = !$scope.wbToggle;
				}
				if('fullscreen' in storage) $scope.wbFullscreen = storage['fullscreen'];
				if('close' in storage) $scope.wbClose = storage['close'];
			}
			////////////
			
			
			$scope.$watch('wbReloading', function(newValue, oldValue) {
				if(oldValue === newValue) return;
				if(loopUpdate['reload']) return (loopUpdate['reload'] = false);
				
				if(oldValue === true && newValue === false) {
					element.trigger('reloaded.ace.widget');
				}
				else if(oldValue === false && newValue === true) {
					//don't trigger if called from widget toolbar
					element.widget_box('reload');
				}				
			});
			
			
			$scope.$watch('wbToggle', function(newValue, oldValue) {
				if(save && newValue !== undefined) $localStorage[saveName]['toggle'] = newValue;
				
				if(oldValue === newValue) return;
				if(loopUpdate['toggle']) return (loopUpdate['toggle'] = false);
				
				if(newValue === true) {
					$scope.wbHidden = false;
					element.widget_box('show');
				}
				else if(newValue === false) {
					//$scope.wbHidden = true;
					element.widget_box('hide');
				}
			});
			
			
			$scope.$watch('wbFullscreen', function(newValue, oldValue) {
				if(save && newValue !== undefined) $localStorage[saveName]['fullscreen'] = newValue;
				
				if(oldValue === newValue) return;
				if(loopUpdate['fullscreen']) return (loopUpdate['fullscreen'] = false);
				
				element.widget_box('fullscreen', newValue);
			});
			
			$scope.$watch('wbClose', function(newValue, oldValue) {
				if(save && newValue !== undefined) $localStorage[saveName]['close'] = newValue;
				
				if(oldValue === newValue) return;
				if(loopUpdate['close']) return (loopUpdate['close'] = false);
				
				if(newValue === true) element.widget_box('close');
			});
			
			
			if($scope.wbClose) element.widget_box('close');
			else if($scope.wbHidden) $timeout(function() {
				element.widget_box('hideFast');
			});
			
	  }//link
	};
}])


.directive('widgetHeader', ['$timeout', function($timeout){
	return {
	  restrict: 'EA',
	  transclude: true,
	  replace: true,
	  scope: {
		toolbar:'=?',
		toggleIconDown:'=?',
		toggleIconUp:'=?',
		wbHidden:'=?'
	  },
	  template: '<div class="widget-header">' +
					'<h5 class="widget-title" ng-transclude></h5>' +
					'<div class="widget-toolbar" ng-if="!!toolbar">' +
						'<a ng-if="toolbar.fullscreen" href="" data-action="fullscreen" class="orange2"><i class="ace-icon fa fa-expand"></i></a> ' +
						'<a ng-if="toolbar.reload" href="" data-action="reload"><i class="ace-icon fa fa-refresh"></i></a> ' +
						'<a ng-if="toolbar.toggle" href="" data-action="collapse"><i class="ace-icon fa" ng-class="{\'{{toggleIconUp}}\': !wbHidden, \'{{toggleIconDown}}\': wbHidden}" data-icon-hide="{{toggleIconUp}}" data-icon-show="{{toggleIconDown}}"></i></a> ' +
						'<a ng-if="toolbar.close"  href="" data-action="close"><i class="ace-icon fa fa-times"></i></a> ' +
					'</div>' +
				'</div>',
		
		link: function ($scope, element, attributes) {
			$scope.toggleIconDown = $scope.toggleIconDown || 'fa-chevron-down';
			$scope.toggleIconUp = $scope.toggleIconUp || 'fa-chevron-up';
			
			$scope.toolbar = $scope.toolbar || {};
			$scope.wbHidden = $scope.wbHidden || false;
		}
	};
}])

.directive('aceFileinput', [function() {
	return {
		restrict: 'A',
		scope: {
		  options: '=',
		  ngModel: '=',
		  props: '=',
		  onChange: '=',
		  onError: '=',
		  onPreview: '=',
		  fileList: '='
		},
		link: function (scope, element, attrs) {
			scope.loopUpdate = false;

			element
			.ace_file_input(scope.options)
			.on('change', function() {
				scope.$apply(function() {
				  scope.loopUpdate = true;
				  scope.ngModel = element.data('ace_input_files');
				});
				
				if(angular.isFunction(scope.onChange)) {
				  scope.onChange();
				}
			})
			.on('file.error.ace', function(event, info) {
				if(angular.isFunction(scope.onError)) scope.onError(info);
			})
			.on('file.preview.ace', function(event, info) {
				if(angular.isFunction(scope.onPreview)) scope.onPreview(info);
			});
			
			if(scope.fileList) element.ace_file_input('show_file_list', scope.fileList);
			
			
			scope.$watch('options', function(newOptions) {
				element.ace_file_input('update_settings', newOptions);
			});
			
			scope.$watch('ngModel', function(newValue) {
				if(!scope.loopUpdate) {
					if(newValue == null) element.ace_file_input('reset_input');
				}
				else scope.loopUpdate = false;
			});
			
			scope.$watch('props.loading', function(newValue) {
				if(newValue === true) element.ace_file_input('loading');
				else if(newValue === false) element.ace_file_input('loading', false);
				else if(typeof newValue === 'string') element.ace_file_input('loading', newValue);
			});
			scope.$watch('props.enabled', function(newValue) {
				if(newValue === true) element.ace_file_input('enable');
				else if(newValue === false) element.ace_file_input('disable');
			});
			scope.$watch('props.reset_button', function(newValue) {
				if(newValue === true) element.ace_file_input('enable_reset', true);
				else if(newValue === false) element.ace_file_input('enable_reset', false);
			});

		}
	};
}])

.directive('aceColorpicker', function() {
	return {
		restrict: 'EA',
		replace: true,
		scope: {
		  ngModel: '=?',
		  ngValue: '=?',
		  options: '=?',
		  colors: '=?',
		  addNew: '=?'
		},
		
		template: '<div uib-dropdown class="dropdown-colorpicker">' +
                      '<a href="" uib-dropdown-toggle><span class="btn-colorpicker" ng-style="{\'background-color\': selectedColor}"></span></a>' +
                      '<ul uib-dropdown-menu aria-labelledby="colorpicker-dropdown" ng-class="{\'dropdown-menu-right\': options.pull_right, \'dropdown-caret\': options.caret}">' +
						 '<li ng-repeat="color in sourceColors">' +
							'<a href="" ng-click="selectColor(color.color)" ng-class="{\'colorpick-btn\': true , \'selected\': color.selected}" ng-style="{\'background-color\': color.color}"></a>' +
						 '</li>' +
                      '</ul>' +
                      '</div>',
		link: function ($scope, element, attributes) {

			$scope.addNew = $scope.addNew || false;//if ngModel is assigned a new value, should we add it to our list or not?		
			$scope.sourceColors = {};
			$scope.options = angular.extend({'caret': true}, $scope.options);
			
			var selectedColor = false;
			$scope.selectedColor = false;
			
			//list of colors
			//we convert it to an object like {'#FF0000': {color: '#FFFF00', value: 'redValue', selected: false} , ... }
			$scope.$watch('colors', function(newValue) {
				var isObj = false;				
				var sourceColors = $scope.colors || [];
				
				if( angular.isArray(sourceColors) ) isObj = false;
				else if( angular.isObject(sourceColors) ) isObj = true;
				else return;
				
				$scope.sourceColors = {};
				angular.forEach(sourceColors, function(value, index) {
					if(isObj) {
						//index is color name, value is some value
						$scope.sourceColors[index] = {'color': index, 'value': value, 'selected': false};
					}
					else {
						if( angular.isObject(value) ) {
							//value is an object {color: red, value: something}
							$scope.sourceColors[value.color] = {'color': value.color, 'value': value.value, 'selected': false};
						}
						else {
							//value is a string (color)
							$scope.sourceColors[value] = {'color': value, 'value': value, 'selected': false};
						}
					}
				});
			});

			//gets called when a color is selected
			$scope.selectColor = function(color) {
			  $scope.ngModel = color;
			}			
			return $scope.$watch('ngModel', function(newValue) {
				if(!newValue) return;
				
				var newColor;
				if(angular.isObject(newValue) && ('color' in newValue)) newColor = newValue.color;
				else if(angular.isString(newValue)) newColor = newValue;
				else return;
				
				//if we already have the color
				if( $scope.sourceColors.hasOwnProperty(newColor) ) {
					if(selectedColor) $scope.sourceColors[selectedColor].selected = false;
					$scope.sourceColors[newColor].selected = true;
					selectedColor = newColor;
				}
				//if we don't have the new color let's add it
				else if($scope.addNew) {
					if(selectedColor) $scope.sourceColors[selectedColor].selected = false;
					
					if(angular.isObject(newValue) && ('color' in newValue)) {
						$scope.sourceColors[newColor] = 
						{
							'color': newColor,
							'value': ('value' in newValue) ? newValue.value : newColor,
							'selected': true
						};
						$scope.ngModel = selectedColor = newColor;//ngModel shouldn't be an object
					}
					else if(angular.isString(newValue)) {
						$scope.sourceColors[newColor] = {'color': newColor, 'value': newColor, 'selected': true};
						selectedColor = newColor;
					}
				}
				
				$scope.selectedColor = selectedColor;
				$scope.ngValue = newValue in $scope.sourceColors ? $scope.sourceColors[newColor].value : '';
			});
		}
	};
})


.directive('aceSidebar', ['$timeout', '$rootScope', function($timeout, $rootScope) {
	return {
		restrict: 'A',
		scope: {
		  options: '=',
		  props: '=?',
		  scroll: '=?',
		  hover: '=?'
		},
		link: function (scope, element, attributes) {
			var $element = jQuery(element);
			var sidebarScroll = false, sidebarHover = false;
			
			$element.ace_sidebar(typeof scope.options === 'object' ? scope.options : {});
			if(scope.scroll) {
				$element.ace_sidebar_scroll(typeof scope.scroll === 'object' ? scope.scroll : {});
				sidebarScroll = true;
			}
			if(scope.hover) {
				$element.ace_sidebar_hover(typeof scope.hover === 'object' ? scope.hover : {});
				sidebarHover = true;
			}
			
			
			var loopUpdate = {'minimized': false, 'toggle': false};
			scope.props = scope.props || {};
			
			scope.$watch('props.minimized', function(newValue) {
				if( !loopUpdate['minimized'] ) {
					loopUpdate['minimized'] = true;
					
					if(newValue == true) $element.ace_sidebar('collapse');
					else $element.ace_sidebar('expand');
				}
				else loopUpdate['minimized'] = false;
			});
			
			scope.$watch('props.toggle', function(newValue) {
				if( !loopUpdate['toggle'] ) {
					loopUpdate['toggle'] = true;
					
					if(newValue == true) $element.ace_sidebar('mobileShow');
					else $element.ace_sidebar('mobileHide');
				}
				else loopUpdate['toggle'] = false;
			});
			
			scope.$watch('props.reset', function(newValue) {
				if(newValue) {
					if(sidebarScroll) $element.ace_sidebar_scroll('reset');
					if(sidebarHover) $element.ace_sidebar_hover('reset');
				}
				scope.props.reset = false;
			});
			
			
			
			$element
			.on('mobileShow.ace.sidebar', function() {
				$timeout(function() { scope.props.toggle = true; });
			})
			.on('mobileHide.ace.sidebar', function() {
				$timeout(function() { scope.props.toggle = false; });
			})
			.on('collapse.ace.sidebar', function() {
				$timeout(function() { scope.props.minimized = true; });
			})
			.on('expand.ace.sidebar', function() {
				$timeout(function() { scope.props.minimized = false; });
			});

			////////
			$element
			.on('show.ace.submenu', '.submenu', function() {
				$rootScope.subMenuOpen[jQuery(this).attr('data-name')] = true;
			})
			.on('hidden.ace.submenu', '.submenu', function() {
				$rootScope.subMenuOpen[jQuery(this).attr('data-name')] = false;
			});

		}
	};
}]);
