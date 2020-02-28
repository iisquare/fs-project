// Define module using Universal Module Definition pattern
// https://github.com/umdjs/umd/blob/master/returnExports.js

(function (root, factory) {
  if (typeof define === 'function' && define.amd) {
    // Support AMD. Register as an anonymous module.
    // EDIT: List all dependencies in AMD style
    define(['angular', 'dropzone'], factory);
  }
  else {
    // No AMD. Set module as a global variable
    // EDIT: Pass dependencies to factory function
    factory(root.angular, root.Dropzone);
  }
}(this,
//EDIT: The dependencies are passed to this function
function (angular, Dropzone) {
  //---------------------------------------------------
  // BEGIN code for this module
  //---------------------------------------------------

  'use strict';

  return angular.module('ngDropzone', [])
    .directive('ngDropzone', function () {
      return {
        restrict: 'AE',
        template: '<div ng-transclude></div>',
        transclude: true,
        scope: {
          dropzone: '=',
          dropzoneConfig: '=',
          eventHandlers: '='
        },
        link: function(scope, element, attrs, ctrls) {
          try { Dropzone }
          catch (error) {
            throw new Error('Dropzone.js not loaded.');
          }

          var dropzone = new Dropzone(element[0], scope.dropzoneConfig);

          if (scope.eventHandlers) {
            Object.keys(scope.eventHandlers).forEach(function (eventName) {
              dropzone.on(eventName, scope.eventHandlers[eventName]);
            });
          }

          scope.dropzone = dropzone;
        }
      };
    });

  //---------------------------------------------------
  // END code for this module
  //---------------------------------------------------
}));


