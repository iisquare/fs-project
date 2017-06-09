angular.module('ui.knob', [])
  .directive('knob', function () {
    return {
      restrict: 'EACM',
      template: function(elem, attrs){

        return '<input value="{{ knob }}">';

      },
      replace: true,
      scope: true,
      link: function (scope, elem, attrs) {

        scope.knob = scope.$eval(attrs.knobData);
        var $elem = angular.element(elem);

        var renderKnob = function(){

          scope.knob = scope.$eval(attrs.knobData);

          var opts = {}; 
          if(!angular.isUndefined(attrs.knobOptions)){
            opts = scope.$eval(attrs.knobOptions);
          }

          if(!angular.isUndefined(attrs.knobMax)){
            var max = scope.$eval(attrs.knobMax);
            if(!angular.isUndefined(max)){

              opts.max = max;
            
            }
          }
          angular.element(elem).knob(opts);
        };

        var updateMax = function updateMax() {
          var max = scope.$eval(attrs.knobMax);
          var val = scope.$eval(attrs.knobData);
          $elem.trigger('configure', {
            'max': parseInt(max, 10)
          }).trigger('change');
          $elem.val(val).change();
        };

        scope.$watch(attrs.knobData, function () {
          scope.knob = scope.$eval(attrs.knobData);
          $elem.val(scope.knob).change();
        });

        scope.$watch(attrs.knobMax, function() {
          updateMax();
        });

        scope.$watch(attrs.knobOptions, function () {
          elem.trigger('configure',scope.$eval(attrs.knobOptions));
          renderKnob();
        }, true);

      }
    };
  });
