angular.module('AceApp').controller('ContentSliderCtrl', ['$scope', '$aside', function($scope, $aside) {
	
 $scope.openAside = function(position, direction) {
  $aside.open({
	templateUrl: 'aside-'+direction+'.html',
	placement: position,
	backdrop: (position == 'top' || position == 'right') ? true : false,
	controller: function($scope, $uibModalInstance) {
		$scope.ok = function(e) {
			$uibModalInstance.close();
			e.stopPropagation();
		};
		$scope.cancel = function(e) {
			$uibModalInstance.dismiss();
			e.stopPropagation();
		};
	}
  })
 }

}]);
