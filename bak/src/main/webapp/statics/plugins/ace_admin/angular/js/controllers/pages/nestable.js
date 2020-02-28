'use strict';

angular.module('AceApp').controller('NestableCtrl', ['$scope', function($scope) {
	
	$scope.items = [
          {
            item: {text: 'a'},
            children: []
          },
          {
            item: {text: 'ITEM'},
            children: [
              {
                item: {text: 'c'},
                children: []
              },
              {
                item: {text: 'd'},
                children: []
              }
            ]
          },
          {
            item: {text: 'e'},
            children: []
          },
          {
            item: {text: 'f'},
            children: []
          }
        ];

}]);
