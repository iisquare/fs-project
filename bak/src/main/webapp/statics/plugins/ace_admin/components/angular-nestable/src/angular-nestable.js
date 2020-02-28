/**
 * Angular nestable 0.0.1
 * Copyright (c) 2014 Kamil Pekala
 * https://github.com/kamilkp/ng-nestable
 */

/**
 * Sample output HTML
	<div class="dd">
		<ol class="dd-list">
			<li class="dd-item" data-id="1">
				<!-- item element -->
			</li>
			<li class="dd-item" data-id="2">
				<!-- item element -->
			</li>
			<li class="dd-item" data-id="3">
				<!-- item element -->
				<ol class="dd-list">
					<li class="dd-item" data-id="4">
						<!-- item element -->
					</li>
					<li class="dd-item" data-id="5">
						<!-- item element -->
					</li>
				</ol>
			</li>
		</ol>
	</div>
 */

/**
 * Sample model object
	[
		{
			item: {},
			children: []
		},
		{
			item: {},
			children: [
				{
					item: {},
					children: []
				}
			]
		},
		{
			item: {},
			children: []
		}
	]
 */

;(function(window, document, angular, undefined){
	angular.module('ng-nestable', [])
		.provider('$nestable', function(){
			var modelName = '$item';
			var defaultOptions = {};

			this.$get = function(){
				return {
					modelName: modelName,
					defaultOptions: defaultOptions
				};
			};

			/**
			 * Method to set model variable for nestable elements
			 * @param  {[string]} value
			 */
			this.modelName = function(value){
				modelName = value;
			};

			/**
			 * Method to set default nestable options
			 * @param  {[object]} value
			 * You can change the follow options:

				maxDepth        : number of levels an item can be nested (default 5)
				group           : group ID to allow dragging between lists (default 0)
				
				listNodeName    : The HTML element to create for lists (default 'ol')
				itemNodeName    : The HTML element to create for list items (default 'li')
				rootClass       : The class of the root element .nestable() was used on (default 'dd')
				listClass       : The class of all list elements (default 'dd-list')
				itemClass       : The class of all list item elements (default 'dd-item')
				dragClass       : The class applied to the list element that is being dragged (default 'dd-dragel')
				handleClass     : The class of the content element inside each list item (default 'dd-handle')
				collapsedClass  : The class applied to lists that have been collapsed (default 'dd-collapsed')
				placeClass      : The class of the placeholder element (default 'dd-placeholder')
				emptyClass      : The class used for empty list placeholder elements (default 'dd-empty')
				expandBtnHTML   : The HTML text used to generate a list item expand button (default '<button data-action="expand">Expand></button>')
				collapseBtnHTML : The HTML text used to generate a list item collapse button (default '<button data-action="collapse">Collapse</button>')

			 */
			this.defaultOptions = function(value){
				defaultOptions = value;
			};
		})
		.directive('ngNestable', ['$compile', '$nestable', function($compile, $nestable){
			return {
				restrict: 'A',
				require: 'ngModel',
				compile: function(element){
					var itemTemplate = element.html();
					element.empty();
					return function($scope, $element, $attrs, $ngModel){
						var options = $.extend(
							{},
							$nestable.defaultOptions,
							$scope.$eval($attrs.ngNestable)
						);
						$scope.$watchCollection(function(){
							return $ngModel.$modelValue;
						}, function(model){
							if(model){

								/**
								 * we are running the formatters here instead of watching on $viewValue because our model is an Array
								 * and angularjs ngModel watcher watches for "shallow" changes and otherwise the possible formatters wouldn't
								 * get executed
								 */
								model = runFormatters(model, $ngModel);
								// TODO: optimize as rebuilding is not necessary here
								var root = buildNestableHtml(model, itemTemplate);
								$element.empty().append(root);
								$compile(root)($scope);
								root.nestable(options);
								root.on('change', function(){
									$ngModel.$setViewValue(root.nestable('serialize'));
									$scope && $scope.$root && $scope.$root.$$phase || $scope.$apply();
								});
							}
						});
					};
				},
				controller: angular.noop
			};

			function buildNestableHtml(model, tpl){
				var root = $('<div class="dd"></div>');
				var rootList = $('<ol class="dd-list"></ol>').appendTo(root);
				model.forEach(function f(item){
					var list = Array.prototype.slice.call(arguments).slice(-1)[0];
					if(!(list instanceof $)) list = rootList;

					var listItem = $('<li class="dd-item"></li>');
					var listElement = $('<div ng-nestable-item class="dd-handle"></div>');
					listElement.append(tpl).appendTo(listItem);
					list.append(listItem);
					listItem.data('item', item.item);
					if(isArray(item.children) && item.children.length > 0){
						var subRoot = $('<ol class="dd-list"></ol>').appendTo(listItem);
						item.children.forEach(function(item){
							f.apply(this, Array.prototype.slice.call(arguments).concat([subRoot]));
						});
					}
				});

				return root;
			}

			function isArray(arr){
				return Object.prototype.toString.call(arr) === '[object Array]';
			}

			function runFormatters(value, ctrl){
				var formatters = ctrl.$formatters,
				idx = formatters.length;

				ctrl.$modelValue = value;
				while(idx--) {
					value = formatters[idx](value);
				}

				return value;
			}
	}])
	.directive('ngNestableItem', ['$nestable', function($nestable){
		return {
			scope: true,
			require: '^ngNestable',
			link: function($scope, $element){
				$scope[$nestable.modelName] = $element.parent().data('item');
			}
		};
	}]);
})(window, document, window.angular);