angular.module('knobApp', ['ui.knob'])

angular.module('knobApp')
  .controller('exampleCtrl', ['$scope', function($scope){

    $scope.max = 1000;

    $scope.data = 30;

    $scope.knobOptions = {
      'width':100,
      'displayInput': false
    };

    //02

    $scope.data2 = 0;

    $scope.options2 = {
      'width':150,
      'cursor':true,
      'thickness':0.3,
      'fgColor':'#222'
    };

    $scope.options3 = {
      'displayPrevious': true,
      'min': -100
    };

    $scope.options4 = {
      'angleOffset':90,
      'linecap': 'round'
    };

    $scope.options5 = {
      'fgColor':'#66CC66',
      'angleOffset': '-125',
      'angleArc': 250
    };

    $scope.options7 = {
      'width':75,
      'fgColor':'#ffec03',
      'skin': 'tron',
      'thickness': .2,
      'displayPrevious': true
    };

    $scope.options14 = {
      'readOnly': true
    };

    $scope.options18 = {
      'width':700
    };

  }]);