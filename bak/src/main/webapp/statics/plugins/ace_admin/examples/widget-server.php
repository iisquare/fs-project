<?php
//This is a very simple PHP backend with no validation or any kind of checking, etc

session_start();

$action = $_REQUEST['action'];

switch($action) {
	case 'save':
		$widget_data = isset($_SESSION['widget_data']) ? $_SESSION['widget_data'] : array();
		
		$type = $_POST['type'];
		$info = json_decode($_POST['info']);
				
		//widget order
		if($type == 'widget-order') {
			$widget_data['widget-order'] = $info;
		}
		//widget state (collapsed, expanded or closed?)
		else if($type == 'widget-state') {
			if( !isset($widget_data['widget-state']) ) $widget_data['widget-state'] = array();
			
			$widget = $info;
			$widget_data['widget-state'][$widget->id] = $widget->state;
		}
		//save all widget_data as part of our session data
		$_SESSION['widget_data'] = $widget_data;
	break;
	
	case 'load':
		//retrieve session data for widgets and send it to browser (ajax call)
		$widget_data = isset($_SESSION['widget_data']) ? $_SESSION['widget_data'] : array();
		echo json_encode($widget_data);
	break;
	
	case 'reset':
		//clear session data for widgets
		unset($_SESSION['widget_data']);
	break;
}