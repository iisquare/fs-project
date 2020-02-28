
angular.module('AceApp').controller('CalendarCtrl', function($scope, $timeout, $compile, uiCalendarConfig) {

	//jQuery UI droppable options
	$scope.draggableOpts = {
		zIndex: 999,
		revert: true,      // will cause the event to go back to its
		revertDuration: 0  //  original position after the drag
	};	
	
	
	
    var date = new Date();
    var d = date.getDate();
    var m = date.getMonth();
    var y = date.getFullYear();

    /* event source that contains custom events on the scope */
    $scope.events = [
      {
		id: 1,
		title: 'All Day Event',
		start: new Date(y, m, 1),
		className: 'label-important'
	  },
	  {
		id: 2,
		title: 'Long Event',
		start: moment().subtract(5, 'days').format('YYYY-MM-DD'),
		end: moment().subtract(1, 'days').format('YYYY-MM-DD'),
		className: 'label-success'
	  },
	  {
		id: 3,
		title: 'Some Event',
		start: new Date(y, m, d-3, 16, 0),
		allDay: false,
		className: 'label-info'
	  }
    ];


    /* config object */
    $scope.uiConfig = {
      calendar:{
        editable: true,
		selectable: true,
		droppable: true,
		drop: function(date) { // this function is called when something is dropped
	
			$scope.events.push({
				title: 'New event',
				start: date,
				allDay: false,
				className: [$(this).attr('data-class')],
				id: $scope.events.length + 1,
			});			
			
			if($scope.removeAfterDrag) $(this).remove();
			
		},
		
		selectable: true,
		selectHelper: true,
		select: function(start, end, allDay) {
			
			var title = prompt("New Event Title:");
			if ( !title ) return;
			$timeout(function() {
				$scope.events.push(
					{
						title: title,
						start: start,
						end: end,
						allDay: allDay,
						className: 'label-info'
					}
				);
			});
		}
		,

		buttonHtml: {
			prev: '<i class="ace-icon fa fa-chevron-left"></i>',
			next: '<i class="ace-icon fa fa-chevron-right"></i>'
		},
	
		header: {
			left: 'prev,next today',
			center: 'title',
			right: 'month,agendaWeek,agendaDay'
		},
		
		
        eventClick: function(event, jsEvent, view) {
			var title = prompt("Edit Title:", event.title);
			if ( !title ) return;
			$timeout(function() {
				$scope.events[event.id - 1].title = title;
			});
		},
        eventDrop: $scope.alertOnDrop,
        eventResize: $scope.alertOnResize,
        eventRender: $scope.eventRender
      }
    };

    
    /* event sources array*/
    $scope.eventSources = [$scope.events];

	
    /* alert on eventClick */
    $scope.alertOnEventClick = function(date, jsEvent, view){
        alert(date.title + ' was clicked ');
    };
    /* alert on Drop */
     $scope.alertOnDrop = function(event, delta, revertFunc, jsEvent, ui, view){
       //$scope.alertMessage = ('Event Droped to make dayDelta ' + delta);
    };
    /* alert on Resize */
    $scope.alertOnResize = function(event, delta, revertFunc, jsEvent, ui, view ){
       //$scope.alertMessage = ('Event Resized to make dayDelta ' + delta);
    };

	
    /* add custom event*/
    $scope.addEvent = function() {
      $scope.events.push({
        title: 'Open Sesame',
        start: new Date(y, m, 28),
        end: new Date(y, m, 29),
        className: ['openSesame']
      });
    };
    /* remove event */
    $scope.remove = function(index) {
      $scope.events.splice(index,1);
    };


});
