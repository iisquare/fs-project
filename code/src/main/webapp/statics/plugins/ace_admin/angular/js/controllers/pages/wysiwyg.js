angular.module('AceApp').controller('WysiwygCtrl', ['$scope', '$timeout', function($scope, $timeout ) {

  $scope.html_content = '<h2>Edit wysiwyg!</h2>';

  /**
  $scope.wysi_opts = {
	  toolbar:
		[
			'bold',
			{name:'italic' , title:'Change Title!', icon: 'ace-icon fa fa-leaf'},
			'strikethrough',
			null,
			'insertunorderedlist',
			'insertorderedlist',
			null,
			'justifyleft',
			'justifycenter',
			'justifyright'
		],
		speech_button: false
  }
  */
  
  $scope.markdown_content = '**Edit markdown!**';
  
}]);


