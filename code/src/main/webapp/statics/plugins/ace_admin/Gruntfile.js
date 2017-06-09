/*!
 * Ace's Gruntfile
 */

module.exports = function (grunt) {
  grunt.util.linefeed = '\n';

  var fs = require('fs');
  var path = require('path');
  var generateRTL = require('./build/rtl.js');
  
  var fixIE = require('./build/files/fix-ie.js');//fix IE9- CSS limit issue
  var mergePartials = require('./build/angular-partials.js');//merge page's partial templates and append it to main page template (for fewer http requests)
  
  /**
  var path = {
	'assets': 'assets',
	'components': 'components',
	'angular': 'angular',
	'frontend': 'frontend'
  };
  */  

  function getAceJs(type) {
	var jsList = grunt.file.readJSON('assets/js/src/scripts.json');
	var list = [];
	for(var file in jsList) 
		if(jsList.hasOwnProperty(file) && file.indexOf(type) == 0 && jsList[file] == true) 
			list.push('assets/js/src/'+file)
	
	return list;
  };

  function filesToMinify(type) {
	var type = type || 'js';
	var assetFiles = grunt.file.readJSON('mustache/app/data/common/'+(type === 'css' ? 'style' : 'script')+'-mapping.min.json');
	
	var list = [];
	var regxType = new RegExp("\\.min\\."+type+"$", "i");
	
	var addFile = function(name) {
		if( /**!fs.existsSync(name) &&*/ list.indexOf(name) == -1 ) {
			list.push(name.replace(regxType , '.'+type));
		}
	}
	
	for(var f in assetFiles) if(assetFiles.hasOwnProperty(f)) {
		var file = assetFiles[f];
		
		if(grunt.util.kindOf(file) == 'array') {
			for(var i = 0; i < file.length ; i++) addFile(file[i]);
		}
		else {
			addFile(file);
		}
	}
	
	
	var angularFiles = grunt.file.read('angular/js/app.js');
	var reg = new RegExp('(components(:?[\\w\\.\\/\\-]+?)\.'+type+')\'', 'ig');
	var match;
	while (match = reg.exec(angularFiles)) {
		if(list.indexOf(match[1]) == -1) list.push(match[1]);
	}
	
	return list;
  };
  var $CDN_Replacement = {
	'font-awesome.css': '//maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css',
	'ace-fonts.css': '//fonts.googleapis.com/css?family=Open+Sans:400,300',
	'angular.js': '//ajax.googleapis.com/ajax/libs/angularjs/1.5.0/angular.min.js',
	'jquery.js': '//ajax.googleapis.com/ajax/libs/jquery/2.1.4/jquery.min.js',
	'bootstrap.js': '//netdna.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js'
  };
  var nowDate = new Date();
  
  
  
  
  
  // Project configuration.
  grunt.initConfig({
	
    // Metadata
    pkg: grunt.file.readJSON('package.json'),
    banner: '/*!\n' +
            ' * Ace v<%= pkg.version %>\n' +
            ' */\n',
    // NOTE: This jqueryCheck code is duplicated in customizer.js; if making changes here, be sure to update the other copy too.
    jqueryCheck: 'if (typeof jQuery === \'undefined\') { throw new Error(\'Ace\\\'s JavaScript requires jQuery\') }\n\n',

    // Task configuration.
    clean: {
      dist: ['dist'],
	  all: ['dist', 'demo', 'html'],
	  demo: ['frontend/**.minified', 'angular/**/**.minified', 'angular/**/**.merged'],
	  css: ['dist/css/ace.onpage-help.min.css'],
	  misc: ['bower.json.latest']
    },

    concat: {
      options: {
        banner: '<%= banner %>\n<%= jqueryCheck %>',
        stripBanners: false,
		separator: ';'
      },

	  'ace-functions': {
		src: getAceJs('ace'),
		dest: 'assets/js/ace.js'
	  },
      'ace-elements': {
        src: getAceJs('elements'),
        dest: 'assets/js/ace-elements.js'
      },
	  'ace-functions-slim': {
		//smaller version of ace.js for AngularJS
        src: ['assets/js/src/ace.js', 'assets/js/src/ace.sidebar.js', 'assets/js/src/ace.sidebar-scroll-1.js', 'assets/js/src/ace.submenu-hover.js', 'assets/js/src/ace.widget-box.js', 'assets/js/src/ace.touch-drag.js', 'assets/js/src/ace.scrolltop.js'],
        dest: 'assets/js/ace-small.js'
      }
    },
	
	uglify: {
		options: {
			preserveComments: function(node, comment) {
				return (comment.pos < 50);//keep comment if at the start of file!
			}
		},
		
		ace: {
		  files: [{
			  expand: true,
			  cwd: 'assets/js',
			  src: ['ace.js' , 'ace-small.js', 'ace-elements.js' , 'ace-extra.js'],
			  dest: 'dist/js',
			  ext: function(name) { return name.replace(/(\.src)?\.js$/i , '.min.js'); }
		  }]
		},
		
		comps: {
			files: [{
			  expand: true,
			  cwd: '.',
			  src: filesToMinify('js'),
			  dest: null,
			  rename: function(dest, src) {
				src = src.replace(/.js$/ , ".min.js");
				return src;
			  },
			}]
		},
		
		 misc: {
			 files: [{
				'components/bootstrap/js/transition.min.js': 'components/bootstrap/js/transition.js',
				'components/_mod/jquery.mobile.custom/jquery.mobile.custom.min.js': 'components/_mod/jquery.mobile.custom/jquery.mobile.custom.js'
			 }]
		}
	},
	
	
	less: {
	  ace: {
		options: {
			modifyVars: {
				'grid-gutter-width': '24px',
				'grid-float-breakpoint': '@screen-md-min'
			}
		},
        files: {
          'assets/css/ace.css': 'assets/css/less/ace.less'
        }
      },
      main: {
		options: {
			modifyVars: {
				'grid-gutter-width': '24px',
				'grid-float-breakpoint': '@screen-md-min'
			}
		},
        files: {
		  'assets/css/bootstrap.css': 'assets/css/less/bootstrap/bootstrap.less',
          'assets/css/ace.css': 'assets/css/less/ace.less',
		  'assets/css/ace-skins.css': 'assets/css/less/skins/skins.less',
		  'assets/css/ace-rtl.less.css': 'assets/css/less/ace-rtl.less'
        }
      },
	  front: {
		files: {
          'frontend/frontend.css': 'assets/css/less/ace-frontend.less'
        }
	  },
	  misc: {
		 files: [{
			'components/bootstrap-timepicker/css/bootstrap-timepicker.css': 'components/bootstrap-timepicker/css/timepicker.less'
		 }]
	  }
    },
	
	/**
	csscomb: {
	  ace: {
        files: {
          'assets/css/ace.css' : 'assets/css/ace.css',
		  'assets/css/ace-skins.css' : 'assets/css/ace-skins.css',
		  'assets/css/bootstrap.css' : 'assets/css/bootstrap.css'
        }
      }
    },
	*/
	

	cssmin: {
      options: {
        compatibility: 'ie8',
        keepSpecialComments: '*',
        noAdvanced: true
      },
      ace: {
        files: {
          'dist/css/ace.min.css' : 'assets/css/ace.css',
          'dist/css/ace-skins.min.css' : 'assets/css/ace-skins.css',
		  'dist/css/ace-fonts.min.css' : 'assets/css/ace-fonts.css',
          'dist/css/ace-rtl.min.css' : 'assets/css/ace-rtl.css',
          'dist/css/ace-part2.min.css' : 'assets/css/ace-part2.css',
          'dist/css/ace-ie.min.css' : 'assets/css/ace-ie.css',
		  'dist/css/bootstrap.min.css' : 'assets/css/bootstrap.css'
        }
      },
	  comps: {
			files: [{
			  expand: true,
			  cwd: '.',
			  src: filesToMinify('css'),
			  dest: null,
			  rename: function(dest, src) {
				src = src.replace(/.css$/ , ".min.css");
				return src;
			  },
			}]
	   }
    },
	
	
	copy: {
      'demo-assets': {
        expand: true,
        cwd: './assets',
        src: [
		  'css/**/*.{gif,png,jpg,jpeg}',
          'fonts/*',
		  'avatars/*',
		  'images/**',
		  '!**/readme**'
        ],
        dest: 'dist'
      },
	  bootstrap: {
		expand: true,
        cwd: 'components/bootstrap/less',
        src: ['**'],
        dest: 'assets/css/less/bootstrap'
      },
	  fonts: {
		expand: true,
        cwd: 'components/bootstrap/fonts',
        src: ['**'],
        dest: 'assets/fonts'
      }
    },
	
	'string-replace': {
	  //this is not necessary, only changes bootstrap's values like 66.66666667% to 66.666%!
	  bs_grid: {
		files: {
			'assets/css/less/bootstrap/mixins/grid.less' : 'components/bootstrap/less/mixins/grid.less',
			'assets/css/less/bootstrap/mixins/grid-framework.less' : 'components/bootstrap/less/mixins/grid-framework.less'
		},
		options: {
			replacements: [{
			  pattern: /percentage\(\(@(columns|index) \/ @(grid-columns)\)\)/ig,
			  replacement: '~`\(""+\(\(@\{$1\} / @\{$2\}\) * 100\)\).substring\(0, \(@\{$1\} / @\{$2\} < 0.1 ? 5 : 6\)\)+"%"`'
			}]
		}
	  },
	  
	  //use cdn minified css and js files in demo of angular and frontend
	  minify: {
		files: {
			'angular/index.html.minified' : 'angular/index.html',
			'angular/js/app.js.minified' : 'angular/js/app.js',
			'frontend/landing.html.minified' : 'frontend/landing.html',
			'frontend/soon.html.minified' : 'frontend/soon.html'
		},
		options: {
			replacements: [{
			  pattern: /(:?'|")(:?(:?[\w\.\/\-]+?)\/((:?ace\-fonts\.css)|(:?font-awesome\.css)|(:?jquery\.js)|(:?bootstrap\.js)|(:?angular\.js)))(:?'|")/ig,
			  replacement: function(text, a1, a2, a3, a4) {
				return '"'+$CDN_Replacement[a4]+'"';
			  }
			},
			{
				pattern: /(:?components\/(:?[\w\.\/\-]+?)\.(css|js))(:?'|")/ig,
				replacement: 'components/$2.min.$3$4'
			},
			{
				pattern: /(:?assets\/(:?[\w\.\/\-]+?)\.(css|js))(:?'|")/ig,
				replacement: 'dist/$2.min.$3$4'
			},
			{
				pattern: /(:?assets\/(:?[\w\.\/\-]+?)\.(jpg|gif|png))(:?'|")/ig,
				replacement: 'dist/$2.$3$4'
			}
			]
		}
	  },
	  
	  demo_path: {
		files: {
			'frontend/landing.html.minified' : 'frontend/landing.html.minified',
			'angular/js/controllers/main.js.merged' : 'angular/js/controllers/main.js.merged'
		},
		options: {
			replacements: [
			{
				//for landing.html
				pattern: '../html/',
				replacement: '../'
			},
			{
				//for main.js
				pattern: '../assets',
				replacement: '../dist'
			}
			]
		}
	  },
	  
	  cachify: {
		files: {
			'angular/index.html.minified' : 'angular/index.html.minified'
		},
		options: {
			replacements: [{
				//used only for angular/index.html
				pattern: /\<\!\-\- INSERT TEMPLATE CACHE \-\-\>/ig,
				replacement: '<script src="cache/views/index.js"></script>'
			}]
		}
	  },

	  
	  misc: {
		files: {
			'assets/js/ace-small.js' : 'assets/js/ace-small.js'
		},
		options: {
			replacements: [{
				//used only for angular/index.html
				pattern: /^/,
				replacement: '/** Slimmer (smaller) version of ace.js for AngularJS (includes only sidebar and widget box functions) */\n\n'
			}]
		}
	  },
	  
	  
	  bower: {
		files: {
			'bower.json.latest' : 'bower.json'
		},
		options: {
			replacements: [
			  {
				pattern: /\^[\d\.]+\"/g,
				replacement: 'latest"'
			  },
			  {
				pattern: /(\"dependencies\"\: \{)([\s\S]*?)(\})/ig,
				replacement: '$1 $2, "_mod": "http://responsiweb.com/ace/updated-components.zip"\n$3\n'
			  }
			]
		}
	  }
	},
	
	
	exec: {
	  html: {
        command: 'node mustache/js/index.js --output_folder="../../html" --path_base=".." --path_assets="../assets" --path_components="../components" --path_images="../assets/images" --onpage_help=true --development=true'
      },
	  html_ajax: {
        command: 'node mustache/js/ajax.js --output_folder="../../html" --path_base="../.." --path_assets="../../assets" --path_components="../../components" --path_images="../../assets/images" --onpage_help=true --development=true'
      },
      demo: {
        command: 'node mustache/js/index.js --output_folder="../../demo" --path_minified=".min" --path_base="." --path_assets="./dist" --path_components="./components" --path_images="./dist/images" --demo=true --onpage_help=false --development=false --protocol=false --remote_jquery=true --remote_fonts=true --remote_bootstrap_js=true --remote_fontawesome=true'
      },
	  demo_ajax: {
        command: 'node mustache/js/ajax.js --output_folder="../../demo" --path_minified=".min" --path_base=".." --path_assets="../dist" --path_components="../components" --path_images="../dist/images" --demo=true --onpage_help=false --development=false --protocol=false --remote_jquery=true --remote_fonts=true --remote_bootstrap_js=true --remote_fontawesome=true'
      }
    },
	
	compress: {
	  demo: {
		options: {
		  archive: 'demo-v<%= pkg.version %>.zip',
		  mode: 'zip',
		  level: 9
		},
		files: [
		  { cwd: './', src: ['dummy.php'],	dest: '.', date: nowDate },		  
		  { expand: true, cwd: './demo', src: ['**', '!**/readme**', '!**/Copy **', '!**/**- Copy**'],	dest: '.', date: nowDate },
		  { expand: true, cwd: './dist', src: ['**', '!**/readme**', '!**/Copy **', '!**/**- Copy**'],	dest: 'dist', date: nowDate },
		  { expand: true, cwd: './build/demo', src: ['**', '!**/readme**', '!**/Copy **', '!**/**- Copy**'],   dest: 'build/demo', date: nowDate },
		  { expand: true, cwd: './components', src: ['**/*.{min.js,min.css,gif,png,jpg,jpeg}', '!**/{theme,themes,doc,docs,vendor,test,tests,spec}/**', 'jqGrid/js/minified/i18n/grid.locale-en.js'], dest: 'components', date: nowDate },		  
		  { 
			expand: true, cwd: './frontend', src: ['**'], dest: 'frontend',
			rename: function(dest, src) {
				if(fs.existsSync(dest + "/" + src+'.minified') || fs.existsSync(dest + "/" + src+'.merged')) return '__DUMP__.DUMP';//rename unwanted (duplicate) files to DUMP and delete later
				var out = src.replace(/\.(minified|merged)$/, '');
				return dest + "/" + out;
			}
			, date: nowDate
		  },
		  {
			expand: true, cwd: './angular', src: ['**'], dest: 'angular',
			rename: function(dest, src) {
				if(fs.existsSync(dest + "/" + src+'.minified') || fs.existsSync(dest + "/" + src+'.merged')) return '__DUMP__.DUMP';//rename unwanted (duplicate) files to DUMP and delete later
				var out = src.replace(/\.(minified|merged)$/, '');
				return dest + "/" + out;
			}
			, date: nowDate
		  }
		]
	  },
	  template: {
		options: {
		  archive: 'ace-v<%= pkg.version %>.zip',
		  mode: 'zip',
		  level: 9,
		  date: new Date()
		},
		files: [
		  { cwd: './', src: ['changelog', 'credits.txt', 'dummy.php', 'Gruntfile.js', 'index.html', '.bowerrc', 'package.json'],	dest: '.', date: nowDate },
  		  { expand: true, src: ['bower.json.latest'], rename: function(dest, src) { return 'bower.json'; }, date: nowDate },
		  { expand: true, cwd: './assets', src: ['**', '!**/Copy **', '!**/**- Copy**', '!**/*.rar', '!**/*.zip'], dest: 'assets', date: nowDate },
		  { expand: true, cwd: './build', src: ['**', '!**/**node_modules**/**', '!**/**- Copy **'],	dest: 'build', date: nowDate },
		  { expand: true, cwd: './dist', src: ['**', '!**/readme**'],	dest: 'dist', date: nowDate },
		  { expand: true, cwd: './docs', src: ['**', '!**/Copy **', '!**/**- Copy**'], dest: 'docs', date: nowDate },
		  { expand: true, cwd: './examples', src: ['**', '!**/Copy **', '!**/**- Copy**'], dest: 'examples', date: nowDate },
		  { expand: true, cwd: './', src: ['require.html'], dest: 'html', date: nowDate },
		  { expand: true, cwd: './html', src: ['**'], dest: 'html', date: nowDate },
		  { expand: true, cwd: './mustache', src: ['**', '!**/**node_modules**/**', '!**/_cache/*.php', '!**/Copy **', '!**/**- Copy**'], dest: 'mustache', date: nowDate },
		  { expand: true, cwd: './frontend', src: ['**'],	dest: 'frontend', date: nowDate },
		  { expand: true, cwd: './angular', src: ['**'],	dest: 'angular', date: nowDate },
		  { expand: true, cwd: './components', src: ['**/*.{js,css,less,gif,png,jpg,jpeg,eot,svg,ttf,woff,woff2}', '!**/{theme,themes,doc,docs,vendor,test,tests,spec}/**'], dest: 'components', date: nowDate }
		]
	  },
	  
	  components: {
		options: {
		  archive: 'updated-components.zip',
		  mode: 'zip',
		  level: 9,
		  date: new Date()
		},
		files: [
		  { expand: true, cwd: './components/_mod', src: ['**', '!**/*.bower', '!**/*.min.css', '!**/*.min.js'], dest: '.', date: nowDate }
		]
	  }
	},

	
	ngtemplates: {
	  'layout': {
		cwd: 'angular/views/layouts/default/partial',
		src: ['**.html', '**/**.html', '!**/Copy **', '!**/**- Copy**'],
		dest: 'angular/cache/views/index.js',
		options:  {
			module: 'AceApp',
			url: function(url) { 
				return 'views/layouts/default/partial/'+url; 
			},
			htmlmin: {
			  collapseBooleanAttributes:      true,
			  collapseWhitespace:             true,
			  conservativeCollapse:			  true,
			  removeAttributeQuotes:          true,
			  removeComments:                 true, // Only if you don't use comment directives! 
			  removeEmptyAttributes:          true,
			  removeRedundantAttributes:      true,
			  removeScriptTypeAttributes:     true,
			  removeStyleLinkTypeAttributes:  true
			}
		}
	  }
	}
  });


  // These plugins provide necessary tasks.
  //require('load-grunt-tasks')(grunt, { scope: 'devDependencies' });
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-contrib-cssmin');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-compress');
  grunt.loadNpmTasks('grunt-text-replace');
  grunt.loadNpmTasks('grunt-string-replace');
  grunt.loadNpmTasks('grunt-exec');
  grunt.loadNpmTasks('grunt-angular-templates');
  //grunt.loadNpmTasks('grunt-csscomb');


  grunt.registerTask('make-rtl', 'Generate RTL file.', function () {
    generateRTL(grunt);
  });
  grunt.registerTask('fix-ie', 'Fix IE9- CSS limit issue.', function () {
    fixIE(grunt);
  });
  grunt.registerTask('ng-merge', 'Concat page partials templates and data for demo', function () {
    mergePartials(grunt);
  });
  
  
  //register tasks
  grunt.registerTask('demo-cache', ['ngtemplates', 'ng-merge', 'string-replace:demo_path']);//merge partial templates and data
  grunt.registerTask('demo-minify', ['string-replace:minify', 'string-replace:cachify']);//use minified js and css files
    
  grunt.registerTask('copy-bootstrap', ['copy:bootstrap', 'copy:fonts', 'string-replace:bs_grid']);
  grunt.registerTask('css-all', ['copy-bootstrap', 'less', 'make-rtl', 'fix-ie', 'cssmin', 'clean:css']);//build All CSS
  grunt.registerTask('js-all', ['concat', 'string-replace:misc', 'uglify']);//build All CSS

  grunt.registerTask('demo',    ['css-all', 'js-all', 'demo-minify', 'demo-cache', 'exec:demo', 'exec:demo_ajax', 'copy:demo-assets', 'compress:demo', 'clean']);
  grunt.registerTask('release', ['css-all', 'js-all', 'exec:html', 'exec:html_ajax', 'string-replace:bower', 'compress:template', 'compress:components', 'clean']);
  grunt.registerTask('both',    ['demo', 'release']);
};
