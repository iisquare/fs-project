"use strict";

angular.module("angular-bootstrap-multiselect-test", ["ui.multiselect"]).controller("index", function($scope) {
	$scope.singleselectModel= [
		"",
		"Apples",
		{},
		{
			id: 0,
			name: "Apples"
		},
		{
			id: 0,
			name: "Apples",
			bla: "blubb"
		},
		"",
		""
	];
	$scope.multiselectModel = [
		[],
		[
			"Apples",
			"Peaches"
		],
		[],
		[
			{
				id: 0,
				name: "Apples"
			},
			{
				id: 2,
				name: "Peaches"
			}
		],
		[
			{
				id: 0,
				name: "Apples",
				bla: "blubb"
			},
			{
				id: 2,
				name: "Peaches",
				bla: "blubber"
			}
		]
	];

	$scope.selectOptions = [
		"Apples",
		"Bananas",
		"Peaches"
	];
	$scope.selectOptionsLong = [
		"Apples",
		"Bananas",
		"Peaches",
		"Apples2",
		"Bananas2",
		"Peaches2",
		"Apples3",
		"Bananas3",
		"Peaches3",
		"Apples4",
		"Bananas4",
		"Peaches4"
	];
	$scope.selectOptionsObjects = [
		{
			id: 0,
			name: "Apples"
		},
		{
			id: 1,
			name: "Bananas"
		},
		{
			id: 2,
			name: "Peaches"
		}
	];
	$scope.selectOptionsLongEntries = [
		"ThisIsATestWithVeryLongEntries11111",
		"ThisIsATestWithVeryLongEntries22222",
		"ThisIsATestWithVeryLongEntries33333",
		"ThisIsATestWithVeryLongEntries44444",
		"ThisIsATestWithVeryLongEntries55555",
	];
});