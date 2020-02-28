
angular.module('AceApp').controller('WizardCtrl', function($scope, $timeout, WizardHandler) {

 //we keep track of current step so that we know when to hide/show 'finish' button and 'next' button
 $scope.currentStepName = null;
 $scope.currentStepNumber = 1;
 $scope.isFinalStep = false;
 
 $scope.$watch('currentStepName', function(newValue) {
	$scope.currentStepNumber = WizardHandler.wizard('myWizard').currentStepNumber(); 
	$scope.totalStepCount = WizardHandler.wizard('myWizard').totalStepCount();
	
	$scope.isFinalStep = $scope.currentStepNumber == $scope.totalStepCount;
 });
 
 //last step
 $scope.finishedWizard = function() {
	alert("Thank you!");
 };
 


 $scope.formSubmitted = false;//indicates whether user has tried submitting for or not
 $scope.updateFormScope = function(){
   $scope.formScope = this;//get a reference to form object
 };
 
 //check if a specific field has error
 $scope.hasError = function(field) {
	var myForm = $scope.formScope.myForm;	
	return (myForm[field].$dirty || $scope.formSubmitted) && myForm[field].$invalid;
 };

 //check if form has an invalid field
 $scope.isValidForm = function(){
	$scope.formSubmitted = true;
	return !($scope.validationOn && $scope.formScope.myForm.$invalid);
 };
 
 $scope.canEnterStep = function() {
	var currentStep = WizardHandler.wizard('myWizard').currentStep();
	//enter this step only if previous step has been completed, or we have already been in this step
	var ret = (currentStep.wzData.number == this.wzData.number - 1 && currentStep.completed) || (this.completed == true);
	return ret;
 }; 

 //check if user has selected any kind of subscription
 $scope.subscription = [0,0];
 //iterate 'subscription' array and see if any of them are true(checked) now!
 $scope.userSubscribed = function(input) {
	return Object.keys(input).some(function (key) {
		return input[key];
	});
 };

 ///
 
 //alert boxes
 $scope.alert = {
	'shown': [true,	true, true,	true],
	'close': function(index) {
		$scope.alert.shown[index] = false;
	}
 };

});


