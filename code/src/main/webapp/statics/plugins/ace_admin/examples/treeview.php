<?php
 $parent = isset($_POST['id']) ? (int)$_POST['id'] : 0;
 $result = array();
 $data = array();
 
 try {
	$db = new PDO('sqlite:data/treeview-city.sqlite');
	
	//this query selects all records that are children of our requested $parent id
	//all fields of each record is retrieved (c.*)
	//also the following subquery adds a new field (child_count) to our records
	//(SELECT COUNT(*) FROM city c2 WHERE c2.parent = c.id) = number of records that their parent id is equal to our record's id
    foreach($db->query('SELECT c.* ,
						(SELECT COUNT(*) FROM city c2 WHERE c2.parent = c.id) as child_count
						FROM city c WHERE c.parent='.$parent)
			as $row)
	{
		
			$item = array(
				'text' => $row['text'] ,
				'type' => $row['child_count'] > 0 ? 'folder' : 'item',
				'additionalParameters' =>  array('id' => $row['id'])
			);
			if($row['child_count'] > 0)
				 $item['additionalParameters']['children'] = true;
			else {
				  //we randomly make some items pre-selected for demonstration only
				  //in your app you can set $item['additionalParameters']['item-selected'] = true
				  //for those items that have been previously selected and saved and you want to show them to user again
				if(mt_rand(0, 3) == 0)
					$item['additionalParameters']['item-selected'] = true;
			}

			$data[$row['id']] = $item;
	}

	$result['status'] = 'OK';
	$result['data'] = $data;
	
 }
 catch(PDOException $ex) {
     $result['status'] = 'ERR';
	 $result['message'] = $ex->getMessage();
 }


echo json_encode($result);