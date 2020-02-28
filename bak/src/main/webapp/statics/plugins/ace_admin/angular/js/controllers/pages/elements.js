
angular.module('AceApp')
.provider('$gritter', function() {
  this.$get = function () {
	return {
		add: jQuery.gritter.add,
		remove: jQuery.gritter.remove,
		removeAll: jQuery.gritter.removeAll
	}
  }
})
/**
.provider('$bootbox', function () {
	this.$get = function () {
		return {
			alert: bootbox.alert,
			confirm: bootbox.confirm,
			prompt: bootbox.prompt,
			dialog: bootbox.dialog,
			setDefaults: bootbox.setDefaults
		}
	}
});
*/

angular.module('AceApp').controller('ElementsCtrl', function($scope, $timeout, $uibModal, $gritter) {
  $scope.tabShown = false;
  
  //there is no built-in support for 'tabs' on right or left or bottom, we do this using jQuery when content is loaded
  $scope.$on('$viewContentLoaded', function () {
	if(!jQuery) return;
	$timeout(function() {
		jQuery('#tab-below').addClass('tabs-below').find('.nav').appendTo('#tab-below');
		jQuery('#tab-left').addClass('tabs-left');		
		jQuery('#tab-color').find('.nav').addClass('padding-12 tab-color-blue background-blue');
		
		$scope.tabShown = true;
	});
  });


  //for Accordion
  $scope.isOpen = [true, false, false];
  $scope.toggleOpen = function(index) {
	$scope.isOpen[index] = !$scope.isOpen[index];
	if($scope.isOpen[index]) {
		for(var i = 0; i < $scope.isOpen.length; i++) if(i != index) $scope.isOpen[i] = false;
	}
  };


  //easy pie chart options and values
  $scope.easypiechart = [
	 {
		"options": {
			barColor: '#D15B47',
			trackColor: '#EEEEEE',
			scaleColor: false,
			lineCap: 'butt',
			lineWidth: 8,
			size: 75
		},
		"value": 20
	 },
	 {
		"options": {
			barColor: '#87CEEB',
			trackColor: '#EEEEEE',
			scaleColor: false,
			lineCap: 'butt',
			lineWidth: 8,
			size: 75
		},
		"value": 55
	},
	{
		"options": {
			barColor: '#87B87F',
			trackColor: '#EEEEEE',
			scaleColor: false,
			lineCap: 'butt',
			lineWidth: 8,
			size: 75
		},
		"value": 90
	}];
	
	
	//alert boxes
	$scope.alert = {
		'shown': [true,	true, true,	true],
		'close': function(index) {
			$scope.alert.shown[index] = false;
		}
	};
	
	//gritter options
	//see js/directives/vendor.js as well
	$scope.gritter = {
		'count': 0,
		'light': false,
		'show': function(id) {
			var options = angular.copy($scope.gritter[id]);
		
			if( !('before_open' in options) ) options.before_open = function() { $scope.gritter.count = $scope.gritter.count + 1 }
			if( !('after_close' in options) ) options.after_close = function() { $scope.gritter.count = $scope.gritter.count - 1 }
			
			options['class_name'] = (options['class_name'] || '') + ($scope.gritter.light ? ' gritter-light' : '');
			
			$gritter.add(options);
		},
		'clear': function() {
			$gritter.removeAll();
			$scope.gritter.count = 0;
		},
		
		'regular': {
			title: 'This is a regular notice!',
			text: 'This will fade out after a certain amount of time. Vivamus eget tincidunt velit. Cum sociis natoque penatibus et <a href="#" class="blue">magnis dis parturient</a> montes, nascetur ridiculus mus.',
			image: $scope.$parent.ace.path.assets+'/avatars/avatar1.png'
		},
		'sticky': {
			title: 'This is a sticky notice!',
			text: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus eget tincidunt velit. Cum sociis natoque penatibus et <a href="#" class="red">magnis dis parturient</a> montes, nascetur ridiculus mus.',
			image: $scope.$parent.ace.path.assets+'/avatars/avatar.png',
			sticky: true,
			class_name: 'gritter-info'
		},
		'without-image': {
			title: 'This is a notice without an image!',
			text: 'This will fade out after a certain amount of time. Vivamus eget tincidunt velit. Cum sociis natoque penatibus et <a href="#" class="orange">magnis dis parturient</a> montes, nascetur ridiculus mus.',
			class_name: 'gritter-success'
		},
		'max-3': {
			title: 'This is a notice with a max of 3 on screen at one time!',
			text: 'This will fade out after a certain amount of time. Vivamus eget tincidunt velit. Cum sociis natoque penatibus et <a href="#" class="green">magnis dis parturient</a> montes, nascetur ridiculus mus.',
			image: $scope.$parent.ace.path.assets+'/avatars/avatar3.png',
			class_name: 'gritter-warning',
			before_open: function(){
				if($scope.gritter.count >= 3) return false;
				$scope.gritter.count = $scope.gritter.count + 1
			}
		},
		'center': {
			title: 'This is a centered notification',
			text: 'Just add "gritter-center"',
			class_name: 'gritter-info gritter-center'
		},
		'error': {
			title: 'This is a warning notification',
			text: 'Vivamus eget tincidunt velit. Cum sociis natoque penatibus et',
			class_name: 'gritter-error'
		}
	};
	
	//bootbox options
	//see js/directives/vendor.js as well
	$scope.bootbox = {
		'show': function(id) {
			var options = $scope.bootbox[id]
			$bootbox[options['type']].call(null, options);
		},

		'regular': {
			type: 'prompt',
			title: 'What is your name?',
			callback: function(result) {}
		},
		'confirm': {
			type: 'confirm',
			message: 'Are you sure?',
			callback: function(result) {}
		},
		'custom': {
			type: 'dialog',
			message: "<span class='bigger-110'>I am a custom dialog with smaller buttons</span>",
			buttons: 			
			{
				"success" :
				 {
					"label" : "<i class='ace-icon fa fa-check'></i> Success!",
					"className" : "btn-sm btn-success"
				},
				"danger" :
				{
					"label" : "Danger!",
					"className" : "btn-sm btn-danger"
				}, 
				"click" :
				{
					"label" : "Click ME!",
					"className" : "btn-sm btn-primary"
				}, 
				"button" :
				{
					"label" : "Just a button...",
					"className" : "btn-sm"
				}
			}
		}
	};
	
	/////////////////
	$scope.openModal = function() {
		var modalInstance = $uibModal.open({
		  animation: true,
		  templateUrl: 'modalContent.html',
		  controller: function ($scope, $uibModalInstance) {
			  $scope.ok = function () {
				$uibModalInstance.close();
			  };
			  $scope.cancel = function () {
				$uibModalInstance.dismiss('cancel');
			  };
		  },
		  //size: 'lg',
		});
	};
	
	
	//////////////////
	//Spin.js slider class names
	$scope.slider = {
		styles: ['', 'green', 'red', 'purple', 'orange', 'dark'],
		getStyle: function(index) {
			index = index % $scope.slider.styles.length;
			if(index > 0) return 'ui-slider-'+ $scope.slider.styles[index];
		},
	
		options: {
			range: 'min'
		}
	};
	
	//we put values in a separate object so when values it is changed, "sliders" isn't changed! because if changed, sliders will be redrawn
	$scope.sliderValues = {
		lines: 12,
		length: 7,
		width: 4,
		radius: 10,
		corners: 1,
		rotate: 0,
		trail: 60,
		speed: 1
	};
	
	$scope.sliders = [
		{
			name: 'Lines',
			min: 5, max: 16
		},
		{
			name: 'Length',
			min: 0, max: 30
		},
		{
			name: 'Width',
			min: 2, max: 20
		},
		{
			name: 'Radius',
			min: 0, max: 40
		},
		{
			name: 'Corners',
			min: 0, max: 1, step: 0.1,
		},
		{
			name: 'Rotate',
			min: 0, max: 90
		},
		{
			name: 'Trail',
			min: 10, max: 100
		},
		{
			name: 'Speed',
			min: 0.5, max: 2.2, step: 0.1
		}
	];

	
	
	//sample dropdown menus
	$scope.sampleDropdowns = [
	 {
		list: [
			{text: 'Action'},
			{text: 'Another action'},
			{text: 'Something else here'},
			{divider: true},
			{text: 'Separated link'}
		]
	 },
	 
	 {
		type: 'danger',
		list: [
			{text: 'Action'},
			{text: 'Another action'},
			{text: 'Something else here'},
			{divider: true},
			{
				text: 'More options',
				dropdown: {
					type: 'danger',
					list: [
						{text: 'Second level link'},
						{text: 'Second level link'},
						{text: 'Second level link'},
						{text: 'Second level link'},
						{text: 'Second level link'}
					]
				}
			}
		]
	 },
	 
	 {
		type: 'light',
		list: [
			{text: 'Action'},
			{text: 'Another action'},
			{text: 'Something else here'},
			{divider: true},
			{
				text: 'More options',
				dropup: true,
				dropdown: {
					type: 'light',
					list: [
						{text: 'Second level link'},
						{text: 'Second level link'},
						{text: 'Second level link'},
						{text: 'Second level link'},
						{text: 'Second level link'}
					]
				}
			}
		]
	 },
	 
	 {
		type: 'purple',
		list: [
			{text: 'Action'},
			{text: 'Another action'},
			{text: 'Something else here'},
			{divider: true},
			{
				text: 'More options',
				dropup: true,				
				dropdown: {
					type: 'purple',
					right: true,
					list: [
						{text: 'Second level link'},
						{text: 'Second level link'},
						{text: 'Second level link'},
						{text: 'Second level link'},
						{text: 'Second level link'}
					]
				}
			}
		]
	 }
	];


});