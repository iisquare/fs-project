/* global alert */
/* global angular */

var app = angular.module('app', ['angular-flot']);

app.controller('FlotCtrl', ['$scope', function ($scope) {
  //
  // Standard Chart Example
  //

  $scope.dataset = [{ data: [], yaxis: 1, label: 'sin' }];
  $scope.options = {
    legend: {
      container: '#legend',
      show: true
    }
  };

  for (var i = 0; i < 14; i += 0.5) {
    $scope.dataset[0].data.push([i, Math.sin(i)]);
  }

  //
  // Categories Example
  //

  $scope.categoriesDataset = [[['January', 10], ['February', 8], ['March', 4], ['April', 13], ['May', 17], ['June', 9]]];
  $scope.categoriesOptions = {
    series: {
      bars: {
        show: true,
        barWidth: 0.6,
        align: 'center'
      }
    },
    xaxis: {
      mode: 'categories',
      tickLength: 0
    }
  };

  //
  // Pie Chart Example
  //

  $scope.pieDataset = [];
  $scope.pieOptions = {
    series: {
      pie: {
        show: true
      }
    }
  };

  var pieSeries = Math.floor(Math.random() * 6) + 3;

  for (i = 0; i < pieSeries; i++) {
    $scope.pieDataset[i] = {
      label: 'Series' + (i + 1),
      data: Math.floor(Math.random() * 100) + 1
    };
  }

  //
  // Event example
  //

  $scope.eventDataset = angular.copy($scope.categoriesDataset);
  $scope.eventOptions = angular.copy($scope.categoriesOptions);
  $scope.eventOptions.grid = {
    clickable: true,
    hoverable: true
  };

  $scope.onEventExampleClicked = function (event, pos, item) {
    alert('Click! ' + event.timeStamp + ' ' + pos.pageX + ' ' + pos.pageY);
  };

  $scope.onEventExampleHover = function (event, pos, item) {
    console.log('Hover! ' + event.timeStamp + ' ' + pos.pageX + ' ' + pos.pageY);
  };
}]);
