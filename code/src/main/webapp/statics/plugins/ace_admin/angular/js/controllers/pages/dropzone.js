angular.module('AceApp').controller('DropzoneCtrl', ['$scope', '$timeout', function($scope, $timeout ) {

  $scope.dropzoneConfig = {
	parallelUploads: 3,
	maxFileSize: 30
  };
  
}]);


