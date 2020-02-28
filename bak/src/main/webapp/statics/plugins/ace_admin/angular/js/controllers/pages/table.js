angular.module('AceApp').controller('TableCtrl', function ($scope, $http, $rootScope, DTOptionsBuilder, DTColumnDefBuilder) {

	$scope.dtOptions = DTOptionsBuilder.newOptions().withDisplayLength(10);

    $scope.dtColumnDefs = [
        DTColumnDefBuilder.newColumnDef(0).notSortable().withClass('sorting_disabled'),
        DTColumnDefBuilder.newColumnDef(5).notSortable(),
        DTColumnDefBuilder.newColumnDef(6).notSortable()
    ];

});

//Controller for selectAll checkbox
angular.module('AceApp').controller('SelectTableCtrl', function ($scope, $rootScope) {
	$scope.selectAll = false;
	$scope.$watch('selectAll', function(newValue) {
		angular.forEach($rootScope.getData('domains'), function(domain, key) {
			domain.selected = newValue;
		});
	});

});
