
var fs = require('fs');

function mergePartials() {
	
	//merge and cache each pages's HTML partial template files
	var folders = fs.readdirSync(__dirname+'/../angular/views/pages/partial');
	folders.forEach( function (name) {
		
		var stats = fs.statSync(__dirname+'/../angular/views/pages/partial/'+name);
		if( stats.isDirectory() ) {
			var folderPath = __dirname+'/../angular/views/pages/partial/'+name;
			var partials = fs.readdirSync(folderPath);
			
			var mergedParts = '';
			partials.forEach( function (partial) {
				var filename;//file name, which we use as the variable name
				if (! (filename = partial.match(/(.+?)\.html$/)) ) return;
				
				var content = fs.readFileSync( folderPath+'/'+partial , 'utf-8' );
				mergedParts += '<script type="text/ng-template" id="views/pages/partial/'+name+'/'+partial+'">\n'+content+"\n</script>";
				mergedParts += "\n\n";
			});
			
			
			var cacheText = "\n\n\n\n<!-- **CACHED PARTIAL TEMPLATES** -->";		
			
			var mainContent = fs.readFileSync( __dirname+'/../angular/views/pages/'+name+'.html' , 'utf-8' );
			fs.writeFileSync( __dirname+'/../angular/views/pages/'+name+'.html.merged' , mainContent + cacheText + "\n\n" + mergedParts , 'utf-8' );
		}
	});
	
	
	
	//function that scans a folder for .json data files and concats them	
	function getMergedData(dataFolder, dataPrefixName) {
		var mergedData = '';
		
		var partials = fs.readdirSync(dataFolder);
		partials.forEach( function (partial) {
			var filename;//file name, which we use as the variable name
			if (! (filename = partial.match(/(.+?)\.json$/)) ) return;
			
			var content = fs.readFileSync( dataFolder+'/'+partial , 'utf-8' );
			try {
				content = JSON.parse(content);
				content = JSON.stringify(content, null, 0);
			}
			catch(e) {}			
			
			mergedData += '$rootScope.appData["'+dataPrefixName+'-'+filename[1]+'"]='+content+";\n";
		});
		
		mergedData = "\n/** DATA MERGED AND CACHED USING GRUNT TASKS FOR ACE DEMO **/\n$rootScope.appData = $rootScope.appData || {};\n" + mergedData+"\n";
		mergedData = mergedData.replace(/\$/g, '$$$$');//replace $ with $$ so that later in regex replace, '$1' tokens (like in prices,etc) are not replaced!
		
		return mergedData;
	}
	
	//browse through each directory
	var pageDataFolders = fs.readdirSync(__dirname+'/../angular/data/pages');
	pageDataFolders.forEach( function (name) {
		var pageDataFolder = __dirname+'/../angular/data/pages/'+name;
		var stats = fs.statSync(pageDataFolder);
		if( stats.isDirectory() ) {
			var mergedData = getMergedData(pageDataFolder , 'page-'+name);

			var pageControllerFile =  __dirname+'/../angular/js/controllers/pages/'+name+'.js';
			var mainContent = fs.readFileSync(pageControllerFile , 'utf-8' );
			fs.writeFileSync( pageControllerFile + '.merged' , mainContent.replace(/(controller.*function\s*\(.*\)\s*\{)/i , "$1\n"+mergedData+"\n") , 'utf-8' );
		}
	});
	
	//merge main controller data (navbar, etc)
	var mainControllerFile =  __dirname+'/../angular/js/controllers/main.js';
	var mainContent = fs.readFileSync(mainControllerFile , 'utf-8' );
	fs.writeFileSync( mainControllerFile + '.merged' , mainContent.replace(/(controller.*function\s*\(.*\)\s*\{)/i, "$1\n"+getMergedData(__dirname+'/../angular/data/common' , 'common')+"\n") , 'utf-8' );
}


//this is used for grunt
if(typeof module !== 'undefined') {
 module.exports = function mergePartial(grunt) {
  mergePartials();
  grunt.log.writeln('Angular partial page templates and data merged.');
 }
}