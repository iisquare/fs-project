
angular.module('AceApp').controller('FormCtrl', function($scope, $timeout, $http, $rootScope) {
	
	$scope.$sliders = [77, 55, 33, 40, 88];//jQuery UI slider ng-models
	
	$scope.inputSizing = ['', 'input-sm', 'input-lg', 'input-mini', 'input-small', 'input-medium', 'input-large', 'input-xlarge', 'input-xxlarge'];
	//the slider models used when resizing input elements
	$scope.$inputSize = 1;
	$scope.$inputGrid = 1;
		
	
	//text limiter options
	$scope.text_limiter = {
		remText: '%n character%s remaining...',
		limitText: 'max allowed : %n.'
	};
	
	//spinner options
	$scope.$spinner = [
		{value:0, min:0, max:200, step:10, btn_up_class:'btn-info', btn_down_class:'btn-info'},
		{value:0,min:0,max:10000,step:100, touch_spinner: true, icon_up:'ace-icon fa fa-caret-up bigger-110', icon_down:'ace-icon fa fa-caret-down bigger-110'},
		{value:0,min:-100,max:100,step:10, on_sides: true, icon_up:'ace-icon fa fa-plus bigger-110', icon_down:'ace-icon fa fa-minus bigger-110', btn_up_class:'btn-success' , btn_down_class:'btn-danger'},
		{value:0,min:-100,max:100,step:10, on_sides: true, icon_up:'ace-icon fa fa-plus', icon_down:'ace-icon fa fa-minus', btn_up_class:'btn-purple' , btn_down_class:'btn-purple'}
	];
	//model for spinner input
	$scope.spinVal = 0;
	


	//datepicker value and options
	$scope.datePicker = {
		value: null,
		range: null,
		opts:	{
			format: 'mm-dd-yyyy',
			autoclose: true,
			todayHighlight: true
		}
	};

	//daterangepicker value and options
	$scope.dateRange = {
		values: {
			startDate: null,
			endDate: null	
		},
		opts: {
			drops: 'up'
		}
	};
	
	
	//time picker value and options
	$scope.timePicker = {
		value: null,
		opts : {
			minuteStep: 1,
			showSeconds: true,
			showMeridian: false,
			disableFocus: true,
			icons: {
				up: 'fa fa-chevron-up',
				down: 'fa fa-chevron-down'
			}
		}
	};
	
	
	//date time picker value and options
	$scope.dateTimePicker = {
		value: null,
		opts: {
			format: 'MM/DD/YYYY h:mm:ss A',//use this option to display seconds
			icons: {
				time: 'fa fa-clock-o',
				date: 'fa fa-calendar',
				up: 'fa fa-chevron-up',
				down: 'fa fa-chevron-down',
				previous: 'fa fa-chevron-left',
				next: 'fa fa-chevron-right',
				today: 'fa fa-arrows ',
				clear: 'fa fa-trash',
				close: 'fa fa-times'
			}
		}
	};
	

	//colorpicker
	$scope.colorPicker = {
		value: '#FFFFFF'
	};


	//knob value and options
	$scope.knob1 = {
		value: 15,
		opts: {
			min: 0,
			max: 100,
			step: 10,
			width: 80,
			height: 80,
			thickness: 0.2
		}
	};
	
	$scope.knob2 = {
		value: 41,
		opts: {
			min: 0,
			max: 100,
			step: 10,
			width: 80,
			height: 80,
			thickness: 0.2,
			fgColor: "#87B87F",
			displayPrevious: true,
			angleArc: 250,
			angleOffset: -125
		}
	};
	
	$scope.knob3 = {
		value: 1,
		opts: {
			min: 0,
			max: 10,
			step: 1,
			width: 150,
			height: 150,
			thickness: 0.2	,
			fgColor: "#B8877F",
			angleOffset: 90,
			cursor: true
		}
	};



	//rating  
	$scope.rate = 7;
	$scope.max = 10;
	$scope.isReadonly = false;

	$scope.hoveringOver = function(value) {
		$scope.overStar = value;
		$scope.percent = 100 * (value / $scope.max);
	};

	$scope.ratingStates = [
		{stateOn: 'glyphicon-ok-sign', stateOff: 'glyphicon-ok-circle'}
	];

	$scope.rating = {
		on: 'fa fa-star orange2 bigger-150',
		off: 'fa fa-star-o grey  bigger-150'
	};
  
  
	//////////

	//Twitter typeahead
	$scope.typeahead = null;
	// Instantiate the bloodhound suggestion engine
	var states = new Bloodhound({
		datumTokenizer: function(d) { return Bloodhound.tokenizers.whitespace(d.name) },
		queryTokenizer: Bloodhound.tokenizers.whitespace,
		local: $rootScope.getCommonData('states')
	});

	// initialize the bloodhound suggestion engine
	states.initialize();

	// Typeahead options object
	$scope.typeaheadOpts = {
		highlight: true
	};

	// Single dataset example
	$scope.typeaheadData = {
		displayKey: 'name',
		source: states.ttAdapter()
	};
	
	//update typeahead when data becomes avaiable
	var unbind = $rootScope.$watch("getCommonData('states')", function(newValue) {
		  if(!newValue) return;
		  unbind();
		  
		  states = new Bloodhound({
			datumTokenizer: function(d) { return Bloodhound.tokenizers.whitespace(d.name) },
			queryTokenizer: Bloodhound.tokenizers.whitespace,
			local: newValue
		  });
		  states.initialize();
		  $scope.typeaheadData = {
			displayKey: 'name',
			source: states.ttAdapter()
		  };
	});


	//Multiselect
	$scope.selectOptions = [
		"Cheese",
		"Tomatoes",
		"Mozzarella",
		"Mushrooms",
		"Pepperoni"
	];
	$scope.multiselectModel = [];
	
	
	//Select2
	$scope.availableColors = ['Red','Green','Blue','Yellow','Magenta','Maroon','Umbra','Turquoise'];
	$scope.select2Color = null;

	
	///////
	//file input properties
	$scope.fileInputOptions1 = {
		no_file:'No File ...',
		btn_choose:'Choose',
		btn_change:'Change',
		droppable:false,
		onchange:null,
		thumbnail:false
	};
	
	$scope.fileInputOptions2 = {
		style: 'well',
		btn_choose: 'Drop files here or click to choose',
		btn_change: null,
		no_icon: 'ace-icon fa fa-cloud-upload',
		droppable: true,
		thumbnail: 'small'
	};
	
	//update file input settings when we select 'picture only'
	$scope.pictureOnly = false;
	$scope.$watch('pictureOnly', function(newValue, oldValue) {
		if(newValue) {
			$scope.fileInputOptions2 = {
				'btn_choose': "Drop images here or click to choose",
				'no_icon': "ace-icon fa fa-picture-o",
				'allowExt': ["jpeg", "jpg", "png", "gif" , "bmp"],
				'allowMime': ["image/jpg", "image/jpeg", "image/png", "image/gif", "image/bmp"]
			};
		}
		else {
			$scope.fileInputOptions2 = {
				'btn_choose' : "Drop files here or click to choose",
				'no_icon' : "ace-icon fa fa-cloud-upload",
				'whitelist_ext' : null,
				'whitelist_mime' : null
			};
		}
	});
	
	//file input models
	$scope.files1 = null;//will contain a list of File objects
	$scope.files2 = null;//will contain a list of File objects
	$scope.fileInputProps1 = {};//file input properties, such as 'loading'm 'reset_button', etc ...
	
	
	//callback when file selection has changed
	$scope.fileChanged = function() {
		if(!$scope.files1) return;

		//example for uploading file
		/**
		$scope.fileInputProps1.loading = true;
		
		var formData = new FormData();
		formData.append('avatar', $scope.files1[0]);
		
		$http.post(
			'../examples/file-upload.php',
			formData, 
			{
				headers: {'Content-Type': undefined,  'HTTP_X_REQUESTED_WITH': 'XMLHttpRequest'}
			}
		)
		.success(function (data) {
			$scope.fileInputProps1.loading = false;
        })
        .error(function (data, status) {
			$scope.fileInputProps1.loading = false;
        });
		*/
	};
	
	/**
	$timeout(function() {
		$scope.files1 = null;//will reset file input
	});
	*/

	
});