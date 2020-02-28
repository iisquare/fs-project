//this is similar to the file-upload.html example
//replace the code inside profile page where it says ***UPDATE AVATAR HERE*** with the code below

// ***UPDATE AVATAR HERE*** //

var submit_url = 'file-upload.php';//please modify submit_url accordingly
var deferred = null;
var avatar = '#avatar';

//if value is empty (""), it means no valid files were selected
//but it may still be submitted by x-editable plugin
//because "" (empty string) is different from previous non-empty value whatever it was
//so we return just here to prevent problems
var value = $(avatar).next().find('input[type=hidden]:eq(0)').val();
if(!value || value.length == 0) {
	deferred = new $.Deferred
	deferred.resolve();
	return deferred.promise();
}

var $form = $(avatar).next().find('.editableform:eq(0)')
var file_input = $form.find('input[type=file]:eq(0)');
var pk = $(avatar).attr('data-pk');//primary key to be sent to server

var ie_timeout = null


if( "FormData" in window ) {
	var formData_object = new FormData();//create empty FormData object
	
	//serialize our form (which excludes file inputs)
	$.each($form.serializeArray(), function(i, item) {
		//add them one by one to our FormData 
		formData_object.append(item.name, item.value);							
	});
	//and then add files
	$form.find('input[type=file]').each(function(){
		var field_name = $(this).attr('name');
		var files = $(this).data('ace_input_files');
		if(files && files.length > 0) {
			formData_object.append(field_name, files[0]);
		}
	});

	//append primary key to our formData
	formData_object.append('pk', pk);

	deferred = $.ajax({
				url: submit_url,
			   type: 'POST',
		processData: false,//important
		contentType: false,//important
		   dataType: 'json',//server response type
			   data: formData_object
	})
}
else {
	deferred = new $.Deferred

	var temporary_iframe_id = 'temporary-iframe-'+(new Date()).getTime()+'-'+(parseInt(Math.random()*1000));
	var temp_iframe = 
			$('<iframe id="'+temporary_iframe_id+'" name="'+temporary_iframe_id+'" \
			frameborder="0" width="0" height="0" src="about:blank"\
			style="position:absolute; z-index:-1; visibility: hidden;"></iframe>')
			.insertAfter($form);
			
	$form.append('<input type="hidden" name="temporary-iframe-id" value="'+temporary_iframe_id+'" />');
	
	//append primary key (pk) to our form
	$('<input type="hidden" name="pk" />').val(pk).appendTo($form);
	
	temp_iframe.data('deferrer' , deferred);
	//we save the deferred object to the iframe and in our server side response
	//we use "temporary-iframe-id" to access iframe and its deferred object

	$form.attr({
			  action: submit_url,
			  method: 'POST',
			 enctype: 'multipart/form-data',
			  target: temporary_iframe_id //important
	});

	$form.get(0).submit();

	//if we don't receive any response after 30 seconds, declare it as failed!
	ie_timeout = setTimeout(function(){
		ie_timeout = null;
		temp_iframe.attr('src', 'about:blank').remove();
		deferred.reject({'status':'fail', 'message':'Timeout!'});
	} , 30000);
}


//deferred callbacks, triggered by both ajax and iframe solution
deferred
.done(function(result) {//success
	var res = result[0];//the `result` is formatted by your server side response and is arbitrary
	if(res.status == 'OK') $(avatar).get(0).src = res.url;
	else alert(res.message);
})
.fail(function(result) {//failure
	alert("There was an error");
})
.always(function() {//called on both success and failure
	if(ie_timeout) clearTimeout(ie_timeout)
	ie_timeout = null;	
});

return deferred.promise();
// ***END OF UPDATE AVATAR HERE*** //