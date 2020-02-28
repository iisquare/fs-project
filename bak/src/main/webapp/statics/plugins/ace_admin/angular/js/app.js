/**
 * @name AceApp
 * @description
 * # AceApp
 *
 * Main module of the application.
 */
var app = angular
  .module('AceApp', [
    'ngAnimate',
    'ngResource',
    'ngSanitize',
    'ngTouch',
	//'angular-loading-bar',
	'oc.lazyLoad',
	'ui.bootstrap',
	'ui.router',
	'ace.directives',
	'ngStorage'	
  ])
  .config(function ($stateProvider, $urlRouterProvider, $ocLazyLoadProvider/**, cfpLoadingBarProvider*/) {
	//cfpLoadingBarProvider.includeSpinner = true;
	
	$urlRouterProvider.otherwise('/dashboard');
	
    $stateProvider
	  .state('dashboard', {
		url: '/dashboard',
		title: 'Dashboard',
		icon: 'fa fa-tachometer',
        
		templateUrl: 'views/pages/dashboard.html',
		controller: 'DashboardCtrl',
		
		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([{
						name: 'angular-flot',
						serie: true,
						files: ['../components/Flot/jquery.flot.js',
						'../components/Flot/jquery.flot.pie.js',
						'../components/Flot/jquery.flot.resize.js',
						'../components/angular-flot/angular-flot.js']
					},
					{
						name: 'easypiechart',
						files: ['../components/_mod/easypiechart/angular.easypiechart.js']
					},
					{
						name: 'sparkline',
						files: ['../components/jquery.sparkline/index.js']
					},
					{
						name: 'AceApp',
						files: ['js/controllers/pages/dashboard.js']
					}]);
			}]
		}
      })
	  
	  
	  .state('ui', {
		'abstract': true,
		//url: '/ui',
		title: 'UI & Elements',
		template: '<ui-view/>',
		
		icon: 'fa fa-desktop'
      })
	  .state('ui.typography', {
		url: '/typography',
		title: 'Typography',

		templateUrl: 'views/pages/typography.html',
		
		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([{
						name: 'prettify',
						files: ['../components/_mod/google-code-prettify/prettify.css',	'../components/google-code-prettify/src/prettify.js']
					}]);
			}]
		}
      })
	  .state('ui.elements', {
		url: '/elements',
		title: 'Elements',

		templateUrl: 'views/pages/elements.html',
		controller: 'ElementsCtrl',
		
		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([{
						name: 'bootbox',
						files: ['../components/bootbox.js/bootbox.js']
					},
					{
						name: 'easypiechart',
						files: ['../components/_mod/easypiechart/angular.easypiechart.js']
					},
					{
						name: 'gritter',
						files: ['../components/jquery.gritter/js/jquery.gritter.js']
					},
					{
						serie: true,
						name: 'ui.slider',
						files: ['../components/_mod/jquery-ui.custom/jquery-ui.custom.js', '../components/jqueryui-touch-punch/jquery.ui.touch-punch.js', '../components/angular-ui-slider/src/slider.js']
					},
					{
						serie: true,
						name: 'angularSpinner',
						files: ['../components/spin.js/spin.js',	'../components/spin.js/jquery.spin.js', '../components/angular-spinner/angular-spinner.js']
					},
					{
						name: 'AceApp',
						files: ['js/controllers/pages/elements.js']
					},
					{
						name: 'AceApp',
						insertBefore: '#main-ace-style',
						files: [
							'../components/jquery.gritter/css/jquery.gritter.css',
							'../components/_mod/jquery-ui.custom/jquery-ui.custom.css',
							'css/pages/elements.css'
							]
					}]);
			}]
		}
      })
	  .state('ui.buttons', {
		url: '/buttons',
		title: 'Buttons',

		templateUrl: 'views/pages/buttons.html',
		controller: 'ButtonsCtrl',
		
		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([
					{
						name: 'AceApp',
						files: ['js/controllers/pages/buttons.js']
					}]);
			}]
		}
      })
	  .state('ui.content-slider', {
		url: '/content-slider',
		title: 'Content Slider',

		templateUrl: 'views/pages/content-slider.html',
		controller: 'ContentSliderCtrl',
		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([{
						name: 'ngAside',
						files: ['../components/angular-aside/dist/js/angular-aside.js']
					},
					{
						name: 'AceApp',
						files: ['js/controllers/pages/content-slider.js']
					},
					{
						name: 'AceApp',
						insertBefore: '#main-ace-style',
						files: ['../components/angular-aside/dist/css/angular-aside.css']
					}]);
			}]
		}
      })
	  .state('ui.treeview', {
		url: '/treeview',
		title: 'Treeview',

		templateUrl: 'views/pages/treeview.html',
		controller: 'TreeviewCtrl',
		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([{
						name: 'treeControl',
						files: ['../components/angular-tree-control/angular-tree-control.js']
					},
					{
						name: 'AceApp',
						files: ['js/controllers/pages/treeview.js']
					}]);
			}]
		}
      })
	  .state('ui.nestable-list', {
		url: '/nestable-list',
		title: 'Nestable Lists',

		templateUrl: 'views/pages/nestable-list.html',
		controller: 'NestableCtrl',
		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([{
						name: 'ngNestable',
						files: ['../components/_mod/jquery.nestable/jquery.nestable.js', '../components/angular-nestable/src/angular-nestable.js']
					},
					{
						name: 'AceApp',
						files: ['js/controllers/pages/nestable.js']
					}]);
			}]
		}
      })


	  
	  .state('table', {
		url: '/table',
		title: 'Tables',
		icon: 'fa fa-list',

		templateUrl: 'views/pages/table.html',
		controller: 'TableCtrl',
		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([
					{
						serie: true,
						name: 'dataTables',
						files: ['../components/datatables/media/js/jquery.dataTables.js', '../components/_mod/datatables/jquery.dataTables.bootstrap.js', '../components/angular-datatables/dist/angular-datatables.js']
					},					
					{
						name: 'AceApp',
						files: ['js/controllers/pages/table.js']
					}]);
			}]
		}
      })
	  
	  
	  .state('form', {
		'abstract': true,
		//url: '/form',
		title: 'Forms',
		template: '<ui-view/>',
		icon: 'fa fa-pencil-square-o'
      })
	  .state('form.form-elements', {
		url: '/form-elements',
		title: 'Form Elements',

		templateUrl: 'views/pages/form-elements.html',
		controller: 'FormCtrl',
		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([
					{
						serie: true,
						name: 'ui.slider',
						files: ['../components/_mod/jquery-ui.custom/jquery-ui.custom.js', '../components/jqueryui-touch-punch/jquery.ui.touch-punch.js', '../components/angular-ui-slider/src/slider.js']
					},
					
					{
						name: 'text_area',
						files: ['../components/jquery-inputlimiter/jquery.inputlimiter.js', '../components/angular-elastic/elastic.js']
					},
					
					{
						name: 'mask',
						files: ['../components/angular-ui-mask/dist/mask.js']
					},
					
					{
						name: 'chosen',
						files: ['../components/chosen/chosen.jquery.js', '../components/angular-chosen-localytics/chosen.js']
					},
					
					{
						name: 'spinner',
						files: ['../components/fuelux/js/spinbox.js']
					},

					{
						name: 'datepicker',
						files: ['../components/bootstrap-datepicker/dist/js/bootstrap-datepicker.js']
					},
					
					{
						serie: true,
						name: 'daterange',
						files: ['../components/moment/moment.js', '../components/bootstrap-daterangepicker/daterangepicker.js', '../components/angular-daterangepicker/js/angular-daterangepicker.js']
					},
					
					{
						name: 'timepicker',
						files: ['../components/bootstrap-timepicker/js/bootstrap-timepicker.js']
					},
					
					{
						serie: true,
						name: 'datetimepicker',
						files: ['../components/moment/moment.js', '../components/eonasdan-bootstrap-datetimepicker/src/js/bootstrap-datetimepicker.js']
					},
					
					{
						name: 'colorpicker',
						files: ['../components/angular-bootstrap-colorpicker/js/bootstrap-colorpicker-module.js', '../components/angular-bootstrap-colorpicker/css/colorpicker.css']
					},
					
					{
						name: 'knob',
						files: ['../components/jquery-knob/js/jquery.knob.js', '../components/angular-knob/src/angular-knob.js']
					},
					
					
					{
						serie: true,
						name: 'typeahead',
						files: ['../components/typeahead.js/dist/bloodhound.js', '../components/typeahead.js/dist/typeahead.jquery.js', '../components/angular-typeahead/angular-typeahead.js']
					},
					
					{
						name: 'multiselect',
						files: ['../components/angular-bootstrap-multiselect/angular-bootstrap-multiselect.js']
					},
					
					{
						name: 'select2',
						files: ['../components/ui-select/dist/select.js']
					},
					
					
					{
						name: 'AceApp',
						files: ['js/controllers/pages/form-elements.js']
					},

					
					{
						name: 'AceApp',
						insertBefore: '#main-ace-style',
						files: [
								'../components/chosen/chosen.css',
								'../components/_mod/jquery-ui.custom/jquery-ui.custom.css',
								'../components/eonasdan-bootstrap-datetimepicker/build/css/bootstrap-datetimepicker.css',
								'../components/bootstrap-timepicker/css/bootstrap-timepicker.css',
								'../components/bootstrap-datepicker/dist/css/bootstrap-datepicker3.css',
								'../components/bootstrap-daterangepicker/daterangepicker.css',
								'../components/select2.v3/select2.css'
								]
					}]);
			}]
		}
      })
	  .state('form.form-wizard', {
		url: '/form-wizard',
		title: 'Form Wizard',

		templateUrl: 'views/pages/form-wizard.html',
		controller: 'WizardCtrl',
		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([{
						name: 'ngMessages',
						files: ['../components/angular-messages/angular-messages.js']
					},
					
					{
						name: 'wizard',
						files: ['../components/angular-wizard/dist/angular-wizard.js']
					},
					
					{
						name: 'mask',
						files: ['../components/angular-ui-mask/dist/mask.js']
					},
					
					{
						name: 'chosen',
						files: ['../components/chosen/chosen.jquery.js', '../components/angular-chosen-localytics/chosen.js']
					},
					
					{
						name: 'AceApp',
						files: ['js/controllers/pages/form-wizard.js']
					},
					{
						name: 'AceApp',
						insertBefore: '#main-ace-style',
						files: ['../components/chosen/chosen.css']
					}]);
			}]
		}
		
      })
	  .state('form.wysiwyg', {
		url: '/wysiwyg',
		title: 'Wysiwyg & Markdown',

		templateUrl: 'views/pages/wysiwyg.html',
		controller: 'WysiwygCtrl',

		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([
					{
						name: 'wysiwyg',
						serie: true,
						files: [
								'../components/bootstrap/js/dropdown.js',//for wysiwyg dropdowns
								'../components/jquery.hotkeys/index.js',
								'../components/_mod/bootstrap-wysiwyg/bootstrap-wysiwyg.js']
					},
					
					{
						name: 'markdown',
						files: ['../components/markdown/lib/markdown.js','../components/bootstrap-markdown/js/bootstrap-markdown.js']
					},
					
					{
						name: 'AceApp',	
						files: ['js/controllers/pages/wysiwyg.js']
					}
				]);
			}]
		}	


      })
	  .state('form.dropzone', {
		url: '/dropzone',
		title: 'Dropzone File Upload',

		templateUrl: 'views/pages/dropzone.html',
		controller: 'DropzoneCtrl',

		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([
					{
						name: 'dropzone',
						serie: true,
						files: ['../components/dropzone/dist/dropzone.js', '../components/angular-dropzone/lib/angular-dropzone.js']
					},
					
					{
						name: 'AceApp',	
						files: ['js/controllers/pages/dropzone.js']
					},
					
					{
						name: 'AceApp',
						insertBefore: '#main-ace-style',
						files: ['../components/dropzone/dist/dropzone.css']
					}
				]);
			}]
		}
      })
	  
	  
	  .state('widgets', {
		url: '/widgets',
		title: 'Widgets',
		icon: 'fa fa-list-alt',

		templateUrl: 'views/pages/widgets.html',
		controller: 'WidgetCtrl',
		
		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([
					{
						serie: true,
						name: 'sortable',
						files: ['../components/_mod/jquery-ui.custom/jquery-ui.custom.js', '../components/jqueryui-touch-punch/jquery.ui.touch-punch.js']
					},
										
					{
						name: 'AceApp',	
						files: ['js/controllers/pages/widgets.js']
					}
				]);
			}]
		}
      })
	  
	  .state('calendar', {
		url: '/calendar',
		title: 'Calendar',

		templateUrl: 'views/pages/calendar.html',
		controller: 'CalendarCtrl',
		
		icon: 'fa fa-calendar',
		badge: '<i class="ace-icon fa fa-exclamation-triangle red bigger-130"></i>',
		'badge-class': 'badge-transparent',
		tooltip: '2&nbsp;Important&nbsp;Events',
		'tooltip-class': 'tooltip-error',
		
		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([
					{
						serie: true,
						name: 'calendar',
						files: ['../components/moment/moment.js', '../components/fullcalendar/dist/fullcalendar.js', '../components/angular-ui-calendar/src/calendar.js']
					},
					
					{
						serie: true,
						name: 'drag',
						files: ['../components/_mod/jquery-ui.custom/jquery-ui.custom.js', '../components/jqueryui-touch-punch/jquery.ui.touch-punch.js', '../components/angular-dragdrop/src/angular-dragdrop.js']
					},			
					
					{
						name: 'bootbox',
						files: ['../components/bootbox.js/bootbox.js']
					},
					
					{
						name: 'AceApp',	
						files: ['js/controllers/pages/calendar.js']
					},
					
					{
						name: 'AceApp',
						insertBefore: '#main-ace-style',
						files: ['../components/fullcalendar/dist/fullcalendar.css']
					}
				]);
			}]
		}
      })
	  
	  .state('gallery', {
		url: '/gallery',
		title: 'Gallery',
		
		icon: 'fa fa-picture-o',

		templateUrl: 'views/pages/gallery.html',
		controller: 'GalleryCtrl',
		
		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([
					{
						name: 'gallery',
						files: ['../components/jquery-colorbox/jquery.colorbox.js', '../components/angular-colorbox/js/angular-colorbox.js']
					},
					
					{
						name: 'AceApp',	
						files: ['js/controllers/pages/gallery.js']
					},
					
					{
						name: 'AceApp',
						insertBefore: '#main-ace-style',
						files: ['../components/jquery-colorbox/example1/colorbox.css']
					}
				]);
			}]
		}
		
      })

	  .state('more', {
		'abstract': true,
		//url: '/more',
		title: 'More Pages',
		template: '<ui-view/>',
		icon: 'fa fa-tag'
      })
	  .state('more.profile', {
		url: '/profile',
		title: 'User Profile',

		templateUrl: 'views/pages/profile.html',
		controller: 'ProfileCtrl',
		
		resolve: {
			lazyLoad: ['$ocLazyLoad', function($ocLazyLoad) {
				return $ocLazyLoad.load([
		
					{
						name: 'datepicker',
						serie: true,
						files: ['../components/moment/moment.js', '../components/bootstrap-datepicker/dist/js/bootstrap-datepicker.js']
					},
					
					{
						name: 'spinner',
						files: ['../components/fuelux/js/spinbox.js']
					},
					
					{
						serie: true,
						name: 'jquery-ui',
						files: ['../components/_mod/jquery-ui.custom/jquery-ui.custom.js', '../components/jqueryui-touch-punch/jquery.ui.touch-punch.js']
					},
					
					{
						name: 'x-editable',	
						serie: true,
						files: ['../components/_mod/x-editable/bootstrap-editable.js', '../components/_mod/x-editable/ace-editable.js']
					},
					
					{
						name: 'AceApp',	
						files: ['js/controllers/pages/profile.js']
					},
					
					{
						name: 'AceApp',
						insertBefore: '#main-ace-style',
						files: ['../components/_mod/jquery-ui.custom/jquery-ui.custom.css',
								'../components/bootstrap-datepicker/dist/css/bootstrap-datepicker3.css',
								'../components/_mod/x-editable/bootstrap-editable.css']
					}
				]);
			}]
		}
      })
	  .state('more.even', {
		'abstract': true,
		  
		title: 'Even More',
		template: '<ui-view/>'

      })
	  
	  .state('more.even.error', {
		url: '/error',
		title: 'Error',
		templateUrl: 'views/pages/error.html'
      })
	  
	  
	  /**
	  .state('more.inbox', {
		url: '/inbox',
		title: 'Inbox',

		templateUrl: 'views/pages/inbox.html'
      })
	  .state('more.pricing', {
		url: '/pricing',
		title: 'Pricing',

		templateUrl: 'views/pages/pricing.html'
      })
	  .state('more.invoice', {
		url: '/invoice',
		title: 'Invoice',

		templateUrl: 'views/pages/invoice.html'
      })
	  .state('more.timeline', {
		url: '/timeline',
		title: 'Timeline',

		templateUrl: 'views/pages/timeline.html'
      })
	  
	  
	  .state('other', {
		'abstract': true,
		title: 'Other Pages',
		template: '<ui-view/>',
		icon: 'fa fa-file-o',
		badge: '5', 
		'badge-class': 'badge-primary'
      })
	  .state('other.faq', {
		url: '/faq',
		title: 'FAQ',

		templateUrl: 'views/pages/faq.html'
      })
	  .state('other.error', {
		url: '/error',
		title: 'Error',

		templateUrl: 'views/pages/error.html'
      })
	  .state('other.grid', {
		url: '/grid',
		title: 'Grid',

		templateUrl: 'views/pages/grid.html'
      })
	  .state('other.blank', {
		url: '/blank',
		title: 'Blank',

		templateUrl: 'views/pages/blank.html'
      })
	  */
  })
  .run(function($rootScope) {

  });
