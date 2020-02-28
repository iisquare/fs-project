// AngularJS directives for jQuery sparkline
angular.module('AceApp')
.directive('sparkline', [function() {
	return {
		restrict: 'A',
		scope: {
			ngModel: '=',
			options: '=?'
		},
		link: function (scope, elem, attrs) {
			scope.values = scope.values || [0];
			var $options = {
				type: 'bar',
				barColor: '#EF1E25',
				chartRangeMin: 0
			};
			var options = null;
			
			scope.$watch('options', function(newValue) {
				render();
			});
			
			scope.$watch('ngModel', function(newValue) {
				render();
			});
			
			var render = function() {
				options = !options ? angular.extend($options, scope.options) : angular.extend(options, scope.options);
				$(elem).sparkline(angular.fromJson(scope.ngModel), options);
			};
		}
	};
}]);


angular.module('AceApp')
.directive('prettify', function() {
	return {
		restrict: 'A',
		scope: {
			linenums: '=?',
			lang: '=?'
		},
		link: function (scope, elem, attrs) {
			scope.lang = scope.lang || false;
			scope.linenums = angular.isDefined(scope.linenums) ? scope.linenums : true;
			
			try {
				var pretty = window.prettyPrintOne(elem.html(), scope.lang, scope.linenums)
				elem.empty().html(pretty);
			} catch(e) {}
		}
	};
});



angular.module('AceApp')
.directive('inputLimiter', function(){
  return {
    restrict: 'A',
	scope: {
		options: '=?'
	},
    link: function($scope, element, attrs){
	  $scope.$watch('options', function(newOptions) {
		element.inputlimiter($scope.options);
	  });
    }
  };
});



angular.module('AceApp')
.directive('fxSpinner', function(){
  return {
    restrict: 'A',
	scope: {
		ngModel: '=?',
		options: '=?'
	},
    link: function($scope, element, attrs){
		var $spinner = null;
		var loopUpdate = false;
		
		function spinner() {
			$spinner = element.ace_spinner($scope.options).closest('.ace-spinner').off('changed.fu.spinbox').on('changed.fu.spinbox', function(){
				$scope.$apply(function() {
					loopUpdate = true;
					return $scope.ngModel = $spinner.spinbox('getValue');
				});
			});
		}
		
		$scope.$watch('options', function(newOptions) {
			spinner();
		});
		
		$scope.$watch('ngModel', function(newValue) {
			if( $spinner && newValue !== undefined ) {
				if(!loopUpdate) $spinner.spinbox('setValue', newValue);
				else loopUpdate = false;
			}
		});
    }
  };
});


  
//https://github.com/cletourneau/angular-bootstrap-datepicker/
angular.module('AceApp')
.directive('bsDatepicker', function($timeout) {
  return {
    restrict: 'A',
    scope: {
      options: '=',
      ngModel: '='
    },
    link: function(scope, element) {
      scope.inputHasFocus = false;	  
	  scope.options = angular.extend(jQuery.fn.datepicker.defaults, scope.options || {});
	  
	  var loopUpdate = false;//only update datepicker in watch(ngModel) if model is not updated itself inside changeDate
     
	  element.datepicker(scope.options).on('changeDate', function(e) {
        var defaultFormat, defaultLanguage, format, language;
        defaultFormat = jQuery.fn.datepicker.defaults.format;
        format = scope.options.format || defaultFormat;
        defaultLanguage = jQuery.fn.datepicker.defaults.language;
        language = scope.options.language || defaultLanguage;
        
		$timeout(function() {
		  loopUpdate = true;
          return scope.ngModel = jQuery.fn.datepicker.DPGlobal.formatDate(e.date, format, language);
        });
      });
	  
	  
      element.on('focus', function() {
        return scope.inputHasFocus = true;
      }).on('blur', function() {
        return scope.inputHasFocus = false;
      }).on('hide', function() {
        return scope.inputHasFocus = false;
      });
	  
      
	  return scope.$watch('ngModel', function(newValue) {
        if (!scope.inputHasFocus) {
          if(!loopUpdate) return element.datepicker('update', newValue);
		  else loopUpdate = false;
        }
      });
	  
    }
  };
});


angular.module('AceApp')
.directive('bsTimepicker', ['$timeout', function($timeout) {
  return {
    restrict: 'A',
    scope: {
      options: '=',
      ngModel: '='
    },
    link: function(scope, element) {
      scope.inputHasFocus = false;
	  scope.options = angular.extend(jQuery.fn.timepicker.defaults, scope.options || {});
	  
	  var loopUpdate = false;
	  scope.start = true;
	  
      element.timepicker(scope.options).on('changeTime.timepicker', function(e) {
		$timeout(function(){
			loopUpdate = true;
			scope.ngModel = e.time.value;
		});
      });
	  
      element.on('focus', function() {
		element.timepicker('showWidget');
		return scope.inputHasFocus = true;
      }) 
	  .on('blur', function() {
        return scope.inputHasFocus = false;
      }).on('hide', function() {
        return scope.inputHasFocus = false;
      });
	  
	  
      return scope.$watch('ngModel', function(newValue) {
        if (!scope.inputHasFocus) {
          if(!loopUpdate) return element.timepicker('update', newValue);
		  else loopUpdate = false;
        }
      });
    }
  };
}]);



angular.module('AceApp')
.directive('dateTimepicker', function($timeout) {
  return {
    restrict: 'A',
    scope: {
      options: '=',
      ngModel: '='
    },
    link: function(scope, element) {
      scope.inputHasFocus = false;
	  scope.options = angular.extend(jQuery.fn.datetimepicker.defaults, scope.options || {});
	  
	   var loopUpdate = false;
	  
      element.datetimepicker(scope.options).on('dp.change', function(e) {
		$timeout(function(){
		  loopUpdate = true;
          scope.ngModel = moment(e.date).format(element.data("DateTimePicker").format());
        });
      });
      element.on('focus', function() {
			element.datetimepicker('show');
			return scope.inputHasFocus = true;
      })
	  .on('blur', function() {
			return scope.inputHasFocus = false;
      }).on('hide', function() {
			return scope.inputHasFocus = false;
      });
	 
	  
      return scope.$watch('ngModel', function(newValue) {
        if (!scope.inputHasFocus) {
          if(!loopUpdate) return element.data("DateTimePicker").date(newValue);
		  else loopUpdate = false;
        }
      });
    }
  };
});




angular.module('AceApp')
.directive('bsWysiwyg', function ($timeout) {
 return {
	restrict: 'A',
	scope: {
      options: '=',
	  ngModel: '=',
	  toolbarStyle: '='
    },
	link: function(scope, element) {
	  var loopUpdate = false;
	  jQuery(element).addClass('wysiwyg-editor').ace_wysiwyg(scope.options).prev().addClass(scope.toolbarStyle || 'wysiwyg-style2');

	  element.on('blur', function(){
		$timeout(function() {
			loopUpdate = true;
			scope.ngModel = element.html();
		});
	  });
	  
	  scope.$watch('ngModel', function(value) {
		if(!loopUpdate) jQuery(element).html(value);
		else loopUpdate = false;
      });
	  
	}
 };
});



angular.module('AceApp')
.directive('bsMarkdown', function ($timeout) {
 return {
	restrict: 'A',
	scope: {
      options: '=',
	  ngModel: '='
    },
	link: function(scope, element) {
	  var loopUpdate = false;
		
	  jQuery(element).markdown(scope.options);
	  jQuery(element).parent().find('.btn').addClass('btn-white');
	  
	  scope.$watch('ngModel', function(value) {
  		if(!loopUpdate) jQuery(element).val(value);
		else loopUpdate = false;
      });
	  
	  element.on('blur', function(){
		$timeout(function() {
			loopUpdate = true;
			scope.ngModel = element.val();
		});
	  });
	  
	}
 };
});



angular.module('AceApp')
.directive('jqSortable', ['$localStorage', function($localStorage) {
  return {
	restrict: 'A',
	scope: {
	  options: '=',
	  connectWith: '=',
	  target: '=',
	  save: '=?',
	  saveName: '=?'
	},
	link: function(scope, element, attrs, ctrls) {

		scope.options = scope.options || {};
		if(scope.connectWith) scope.options['connectWith'] = scope.connectWith;
		
		var saveName = scope.saveName || 'ace.widget-order';
		
		
		var savePos = false;
		if(scope.save) {
			savePos = function() {			
				var columns = scope.target;
				var cells = scope.options['items'];
			
				
				var item_order = {}
				jQuery(columns).each(function() {
					var column_id = jQuery(this).attr('id');
					if(!column_id) return;
					item_order[column_id] = [];				
					
					jQuery(this).find(cells).each(function() {
						var cell_id = jQuery(this).attr('id');
						if(!cell_id) return;
						item_order[column_id].push(cell_id);
						//now we know each container contains which widgets/cells
					});
				});
				
				$localStorage[saveName] = JSON.stringify(item_order);
			}
			
			//on initial load
			try {
				var column_list = JSON.parse($localStorage[saveName]);
				for(var column_id in column_list) if(column_list.hasOwnProperty(column_id)) {

					var cells_inside_column = column_list[column_id];
					if(cells_inside_column.length == 0) continue;
					
					for(var c = 0; c < cells_inside_column.length; c++) {
						var cell = cells_inside_column[c];
						jQuery('#'+cell).appendTo('#'+column_id);
					}
				}
			}
			catch(e) {}
		}
		
		
		element.find(scope.target)
		.sortable(scope.options)
		.on( 'sortupdate', function( event, ui ) {
			scope.save && savePos();
		} );

	}
  };
}]);


angular.module('AceApp')
.directive('editable', ['$timeout', function($timeout) {
	return {
		restrict: "A",
		scope: {
			options: '=',
			ngModel: '='
		},

		link: function(scope, element, attrs) {
			var options = scope.options || {};
			var successCallback = null;
			if(options.success) {
				successCallback = options.success;
			}
			options.success = function(response, newValue) {
				var ret;
				if(successCallback) ret = successCallback(response, newValue);
				if(typeof ret !== 'undefined') newValue = ret;
				
				element.editable('setValue', newValue);
				$timeout(function() {
					scope.ngModel = newValue;
				});
			}

			element.addClass('editable').editable(options).editable('setValue', scope.ngModel);
			scope.$watch('ngModel', function(newValue, oldValue) {
				if(typeof newValue !== 'undefined')
					element.editable('setValue', newValue);
			});
			scope.$watch('options', function(newValue, oldValue) {
				element.editable('destroy').editable(newValue).editable('setValue', scope.ngModel);
			}, true);
		}
	};
}]);
