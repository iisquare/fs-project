'use strict';

angular.module('AceApp').controller('ButtonsCtrl', ['$scope', '$timeout', function($scope, $timeout) {
	
	//the loading button
	$scope.loading = false;
	$scope.toggleLoading = function() {
		if($scope.loading) return;
		
		$scope.loading = true;
		$timeout(function() {
			$scope.loading = false;
		}, 2000);
	};

	
	//pagination variables
	$scope.totalItems = 45;
	$scope.currentPage = 1;
	
	
	
	//font icons
	$scope.arrayChunk = function(array, size) {
		var chunks = [];

		while (array.length > 0) {
			chunks.push(array.splice(0, size));
		}
		
		return chunks;
	}
	
	$scope.fontAwesome = [
	'fa-adjust',
	'fa-asterisk',
	'fa-ban',
	'fa-bar-chart-o',
	'fa-barcode',
	'fa-flask',
	'fa-beer',
	'fa-bell-o',
	'fa-bell',
	'fa-bolt',
	'fa-book',
	'fa-bookmark',
	'fa-bookmark-o',
	'fa-briefcase',
	'fa-bullhorn',
	'fa-calendar',
	'fa-camera',
	'fa-camera-retro',
	'fa-certificate',

	'fa-check-square-o',
	'fa-square-o',
	'fa-circle',
	'fa-circle-o',
	'fa-cloud',
	'fa-cloud-download',
	'fa-cloud-upload',
	'fa-coffee',
	'fa-cog',
	'fa-cogs',
	'fa-comment',
	'fa-comment-o',
	'fa-comments',
	'fa-comments-o',
	'fa-credit-card',
	'fa-tachometer',
	'fa-desktop',
	'fa-arrow-circle-o-down',
	'fa-download',

	'fa-pencil-square-o',
	'fa-envelope',
	'fa-envelope-o',
	'fa-exchange',
	'fa-exclamation-circle',
	'fa-external-link',
	'fa-eye-slash',
	'fa-eye',
	'fa-video-camera',
	'fa-fighter-jet',
	'fa-film',
	'fa-filter',
	'fa-fire',
	'fa-flag',
	'fa-folder',
	'fa-folder-open',
	'fa-folder-o',
	'fa-folder-open-o',
	'fa-cutlery',

	'fa-gift',
	'fa-glass',
	'fa-globe',
	'fa-users',
	'fa-hdd-o',
	'fa-headphones',
	'fa-heart',
	'fa-heart-o',
	'fa-home',
	'fa-inbox',
	'fa-info-circle',
	'fa-key',
	'fa-leaf',
	'fa-laptop',
	'fa-gavel',
	'fa-lemon-o',
	'fa-lightbulb-o',
	'fa-lock',
	'fa-unlock']
	
	$scope.glyphicon = [
	'glyphicon-asterisk',
	'glyphicon-plus',
	'glyphicon-euro',
	'glyphicon-minus',
	'glyphicon-cloud',
	'glyphicon-envelope',
	'glyphicon-pencil',
	'glyphicon-glass',
	'glyphicon-music',
	'glyphicon-search',
	'glyphicon-heart',
	'glyphicon-star',
	'glyphicon-star-empty',
	'glyphicon-user',
	'glyphicon-film',
	'glyphicon-th-large',
	'glyphicon-th',
	'glyphicon-th-list',
	'glyphicon-ok',

	'glyphicon-remove',
	'glyphicon-zoom-in',
	'glyphicon-zoom-out',
	'glyphicon-off',
	'glyphicon-signal',
	'glyphicon-cog',
	'glyphicon-trash',
	'glyphicon-home',
	'glyphicon-file',
	'glyphicon-time',
	'glyphicon-road',
	'glyphicon-download-alt',
	'glyphicon-download',
	'glyphicon-upload',
	'glyphicon-inbox',
	'glyphicon-play-circle',
	'glyphicon-repeat',
	'glyphicon-refresh',
	'glyphicon-list-alt',

	'glyphicon-lock',
	'glyphicon-flag',
	'glyphicon-headphones',
	'glyphicon-volume-off',
	'glyphicon-volume-down',
	'glyphicon-volume-up',
	'glyphicon-qrcode',
	'glyphicon-barcode',
	'glyphicon-tag',
	'glyphicon-tags',
	'glyphicon-book',
	'glyphicon-bookmark',
	'glyphicon-print',
	'glyphicon-camera',
	'glyphicon-font',
	'glyphicon-bold',
	'glyphicon-italic',
	'glyphicon-text-height',
	'glyphicon-text-width',

	'glyphicon-align-left',
	'glyphicon-align-center',
	'glyphicon-align-right',
	'glyphicon-align-justify',
	'glyphicon-list',
	'glyphicon-indent-left',
	'glyphicon-indent-right',
	'glyphicon-facetime-video',
	'glyphicon-picture',
	'glyphicon-map-marker',
	'glyphicon-adjust',
	'glyphicon-tint',
	'glyphicon-edit',
	'glyphicon-share',
	'glyphicon-check',
	'glyphicon-move',
	'glyphicon-step-backward',
	'glyphicon-fast-backward',
	'glyphicon-backward']



}]);
