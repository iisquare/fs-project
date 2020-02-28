/**
 <b>Sidebar functions</b>. Collapsing/expanding, toggling mobile view menu and other sidebar functions.
*/

(function($ , undefined) {
	var sidebar_count = 0;

	function Sidebar(sidebar, settings) {
		var self = this;
		this.$sidebar = $(sidebar);
		this.$sidebar.attr('data-sidebar', 'true');
		if( !this.$sidebar.attr('id') ) this.$sidebar.attr( 'id' , 'id-sidebar-'+(++sidebar_count) )

		
		//get a list of 'data-*' attributes that override 'defaults' and 'settings'
		var attrib_values = ace.helper.getAttrSettings(sidebar, $.fn.ace_sidebar.defaults, 'sidebar-');
		this.settings = $.extend({}, $.fn.ace_sidebar.defaults, settings, attrib_values);


		//some vars
		this.minimized = false;//will be initialized later
		this.collapsible = false;//...
		this.horizontal = false;//...
		this.mobile_view = false;//


		//return an array containing sidebar state variables
		this.vars = function() {
			return {'minimized': this.minimized, 'collapsible': this.collapsible, 'horizontal': this.horizontal, 'mobile_view': this.mobile_view}
		}
		this.get = function(name) {
			if(this.hasOwnProperty(name)) return this[name];
		}
		this.set = function(name, value) {
			if(this.hasOwnProperty(name)) this[name] = value;
		}
		

		//return a reference to self (sidebar instance)
		this.ref = function() {
			return this;
		}

		
		//toggle icon for sidebar collapse/expand button
		var toggleIcon = function(minimized, save) {
			var icon = $(this).find(ace.vars['.icon']), icon1, icon2;
			if(icon.length > 0) {
				icon1 = icon.attr('data-icon1');//the icon for expanded state
				icon2 = icon.attr('data-icon2');//the icon for collapsed state

				if(typeof minimized !== "undefined") {
					if(minimized) icon.removeClass(icon1).addClass(icon2);
					else icon.removeClass(icon2).addClass(icon1);
				}
				else {
					icon.toggleClass(icon1).toggleClass(icon2);
				}
				
				try {
					if(save !== false) ace.settings.saveState(icon.get(0));
				} catch(e) {}
			}
		}
		
		//if not specified, find the toggle button related to this sidebar
		var findToggleBtn = function() {
			var toggle_btn = self.$sidebar.find('.sidebar-collapse');
			if(toggle_btn.length == 0) toggle_btn = $('.sidebar-collapse[data-target="#'+(self.$sidebar.attr('id')||'')+'"]');
			if(toggle_btn.length != 0) toggle_btn = toggle_btn[0];
			else toggle_btn = null;
			
			return toggle_btn;
		}
		
		
		//collapse/expand sidebar
		this.toggleMenu = function(toggle_btn, save) {
			if(this.collapsible) return false;

			this.minimized = !this.minimized;
			var save = !(toggle_btn === false || save === false);
			
		
			if(this.minimized) this.$sidebar.addClass('menu-min');
			else this.$sidebar.removeClass('menu-min');

			try {
				if(save) ace.settings.saveState(sidebar, 'class', 'menu-min', this.minimized);
			} catch(e) {}
		
			if( !toggle_btn ) {
				toggle_btn = findToggleBtn();
			}
			if(toggle_btn) {
				toggleIcon.call(toggle_btn, this.minimized, save);
			}

			//force redraw for ie8
			if(ace.vars['old_ie']) ace.helper.redraw(sidebar);
			
			
			$(document).trigger('settings.ace', ['sidebar_collapsed' , this.minimized, sidebar, save]);
			
			if( this.minimized ) this.$sidebar.trigger($.Event('collapse.ace.sidebar'));
			else this.$sidebar.trigger($.Event('expand.ace.sidebar'));
			
			
			return true;
		}
		this.collapse = function(toggle_btn, save) {
			if(this.collapsible) return;
			
			this.minimized = false;
			this.toggleMenu(toggle_btn, save) 
		}
		this.expand = function(toggle_btn, save) {
			if(this.collapsible) return;
			
			this.minimized = true;
			this.toggleMenu(toggle_btn, save);
		}
		

		
		this.showResponsive = function() {
			this.$sidebar.removeClass(responsive_min_class).removeClass(responsive_max_class);
		}
		
		//collapse/expand in 2nd mobile style
		this.toggleResponsive = function(toggle_btn, showMenu) {
			if( !this.mobile_view || this.mobile_style != 3 ) return;
		
			if( this.$sidebar.hasClass('menu-min') ) {
				//remove menu-min because it interferes with responsive-max
				this.$sidebar.removeClass('menu-min');
				var btn = findToggleBtn();
				if(btn) toggleIcon.call(btn);
			}


			var showMenu = typeof showMenu === 'boolean' ? showMenu : (typeof toggle_btn === 'boolean' ? toggle_btn : this.$sidebar.hasClass(responsive_min_class));

			if(showMenu) {
				this.$sidebar.addClass(responsive_max_class).removeClass(responsive_min_class);
			}
			else {
				this.$sidebar.removeClass(responsive_max_class).addClass(responsive_min_class);
			}
			this.minimized = !showMenu;


			if( !toggle_btn || typeof toggle_btn !== 'object' ) {
				toggle_btn = this.$sidebar.find('.sidebar-expand');
				if(toggle_btn.length == 0) toggle_btn = $('.sidebar-expand[data-target="#'+(this.$sidebar.attr('id')||'')+'"]');
				if(toggle_btn.length != 0) toggle_btn = toggle_btn[0];
				else toggle_btn = null;
			}
			
			if(toggle_btn) {
				var icon = $(toggle_btn).find(ace.vars['.icon']), icon1, icon2;
				if(icon.length > 0) {
					icon1 = icon.attr('data-icon1');//the icon for expanded state
					icon2 = icon.attr('data-icon2');//the icon for collapsed state

					if(!showMenu) icon.removeClass(icon2).addClass(icon1);
					else icon.removeClass(icon1).addClass(icon2);
				}
			}

			if(showMenu) self.$sidebar.trigger($.Event('mobileShow.ace.sidebar'));
			else self.$sidebar.trigger($.Event('mobileHide.ace.sidebar'));
			
			$(document).triggerHandler('settings.ace', ['sidebar_collapsed' , this.minimized]);
		}
		
		
		//some helper functions
		
		//determine if we have 4th mobile style responsive sidebar and we are in mobile view
		this.is_collapsible = function() {
			var toggle
			return (this.$sidebar.hasClass('navbar-collapse'))
			&& ((toggle = $('.navbar-toggle[data-target="#'+(this.$sidebar.attr('id')||'')+'"]').get(0)) != null)
			&&  toggle.scrollHeight > 0
			//sidebar is collapsible and collapse button is visible?
		}
		//determine if we are in mobile view
		this.is_mobile_view = function() {
			var toggle
			return ((toggle = $('.menu-toggler[data-target="#'+(this.$sidebar.attr('id')||'')+'"]').get(0)) != null)
			&&  toggle.scrollHeight > 0
		}




		var submenu_working = false;
		//show submenu
		this.show = function(sub, $duration, shouldWait) {
			//'shouldWait' indicates whether to wait for previous transition (submenu toggle) to be complete or not?
			shouldWait = (shouldWait !== false);
			if(shouldWait && submenu_working) return false;
					
			var $sub = $(sub);
			var event;
			$sub.trigger(event = $.Event('show.ace.submenu'))
			if (event.isDefaultPrevented()) {
				return false;
			}
			
			if(shouldWait) submenu_working = true;


			$duration = typeof $duration !== 'undefined' ? $duration : this.settings.duration;
			
			$sub.css({
				height: 0,
				overflow: 'hidden',
				display: 'block'
			})
			.removeClass('nav-hide').addClass('nav-show')//only for window < @grid-float-breakpoint and .navbar-collapse.menu-min
			.parent().addClass('open');
			
			sub.scrollTop = 0;//this is for submenu_hover when sidebar is minimized and a submenu is scrollTop'ed using scrollbars ...

			
			var complete = function(ev, trigger) {
				ev && ev.stopPropagation();
				$sub
				.css({'transition-property': '', 'transition-duration': '', overflow:'', height: ''})
				//if(ace.vars['webkit']) ace.helper.redraw(sub);//little Chrome issue, force redraw ;)

				if(trigger !== false) $sub.trigger($.Event('shown.ace.submenu'))
				
				if(shouldWait) submenu_working = false;
			}
			
			
			var finalHeight = sub.scrollHeight;

			if($duration == 0 || finalHeight == 0 || !$.support.transition.end) {
				//(if duration is zero || element is hidden (scrollHeight == 0) || CSS3 transitions are not available)
				complete();
			}
			else {
				$sub
				.css({
					 'height': finalHeight,
					 'transition-property': 'height',
					 'transition-duration': ($duration/1000)+'s'
					}
				)
				.one($.support.transition.end, complete);
				
				//there is sometimes a glitch, so maybe retry
				if(ace.vars['android'] ) {
					setTimeout(function() {
						complete(null, false);
						ace.helper.redraw(sub);
					}, $duration + 20);
				}
			}

			return true;
		 }
		 
		 
		 //hide submenu
		 this.hide = function(sub, $duration, shouldWait) {
			//'shouldWait' indicates whether to wait for previous transition (submenu toggle) to be complete or not?
			shouldWait = (shouldWait !== false);
			if(shouldWait && submenu_working) return false;
		 
			
			var $sub = $(sub);
			var event;
			$sub.trigger(event = $.Event('hide.ace.submenu'))
			if (event.isDefaultPrevented()) {
				return false;
			}
			
			if(shouldWait) submenu_working = true;
			

			$duration = typeof $duration !== 'undefined' ? $duration : this.settings.duration;
			
			
			var initialHeight = sub.scrollHeight;
			$sub.css({
				height: initialHeight,
				overflow: 'hidden',
				display: 'block'
			})
			.parent().removeClass('open');

			sub.offsetHeight;
			//forces the "sub" to re-consider the new 'height' before transition

			
			var complete = function(ev, trigger) {
				ev && ev.stopPropagation();
				$sub
				.css({display: 'none', overflow:'', height: '', 'transition-property': '', 'transition-duration': ''})
				.removeClass('nav-show').addClass('nav-hide')//only for window < @grid-float-breakpoint and .navbar-collapse.menu-min

				if(trigger !== false) $sub.trigger($.Event('hidden.ace.submenu'))
				
				if(shouldWait) submenu_working = false;
			}
			
			
			if( $duration == 0 || initialHeight == 0 || !$.support.transition.end) {
				//(if duration is zero || element is hidden (scrollHeight == 0) || CSS3 transitions are not available)
				complete();
			}
			else {
				$sub
				.css({
					 'height': 0,
					 'transition-property': 'height',
					 'transition-duration': ($duration/1000)+'s'
					}
				)
				.one($.support.transition.end, complete);
				
				//there is sometimes a glitch, so maybe retry
				if(ace.vars['android'] ) {
					setTimeout(function() {
						complete(null, false);
						ace.helper.redraw(sub);
					}, $duration + 20);
				}
			}

			return true;
		 }

		 //toggle submenu
		 this.toggle = function(sub, $duration) {
			$duration = $duration || self.settings.duration;
		 
			if( sub.scrollHeight == 0 ) {//if an element is hidden scrollHeight becomes 0
				if( this.show(sub, $duration) ) return 1;
			} else {
				if( this.hide(sub, $duration) ) return -1;
			}
			return 0;
		 }



		//toggle mobile menu
		this.mobileToggle = function(showMenu) {
			if(this.mobile_view) {
				if(this.mobile_style == 1 || this.mobile_style == 2) {
					this.toggleMobile(typeof showMenu === 'object' ? showMenu : null, typeof showMenu === 'boolean' ? showMenu : null);
				}
				else if(this.mobile_style == 3) {
					this.toggleResponsive(typeof showMenu === 'object' ? showMenu : null, typeof showMenu === 'boolean' ? showMenu : null);
				}
				//return true;
			}
			else if(this.collapsible) {
				this.toggleCollapsible(typeof showMenu === 'object' ? showMenu : null, typeof showMenu === 'boolean' ? showMenu : null);
				//return true;
			}

			//return true;
		}
		this.mobileShow = function() {
			this.mobileToggle(true);
		}
		this.mobileHide = function() {
			this.mobileToggle(false);
		}
		
		
		this.toggleMobile = function(toggle_btn, showMenu) {
			if(!(this.mobile_style == 1 || this.mobile_style == 2)) return;
			
			var showMenu = typeof showMenu === 'boolean' ? showMenu : (typeof toggle_btn === 'boolean' ? toggle_btn : !this.$sidebar.hasClass('display'));
			
		
			if( !toggle_btn || typeof toggle_btn !== 'object' ) {
				toggle_btn = $('.menu-toggler[data-target="#'+(this.$sidebar.attr('id')||'')+'"]');
				if(toggle_btn.length != 0) toggle_btn = toggle_btn[0];
				else toggle_btn = null;
			}
			if(showMenu) {
				this.$sidebar.addClass('display');
				if(toggle_btn) $(toggle_btn).addClass('display');
			}
			else {
				this.$sidebar.removeClass('display');
				if(toggle_btn) $(toggle_btn).removeClass('display');
			}
			
			if(showMenu) self.$sidebar.trigger($.Event('mobileShow.ace.sidebar'));
			else self.$sidebar.trigger($.Event('mobileHide.ace.sidebar'));
		}
		
		
		this.toggleCollapsible = function(toggle_btn, showMenu) {
			if(this.mobile_style != 4) return;
			
			var showMenu = typeof showMenu === 'boolean' ? showMenu : (typeof toggle_btn === 'boolean' ? toggle_btn : !this.$sidebar.hasClass('in'));
			if(showMenu) {
				this.$sidebar.collapse('show');
			}
			else {
				this.$sidebar.removeClass('display');
				this.$sidebar.collapse('hide');
			}

			if(showMenu) self.$sidebar.trigger($.Event('mobileShow.ace.sidebar'));
			else self.$sidebar.trigger($.Event('mobileHide.ace.sidebar'));
		}
		
		
		
		
		
		
		////////////////
		//private functions
		//sidebar vars
		var minimized_menu_class  = 'menu-min';
		var responsive_min_class  = 'responsive-min';
		var responsive_max_class  = 'responsive-max';
		var horizontal_menu_class = 'h-sidebar';

		var sidebar_mobile_style = function() {
			//differnet mobile menu styles
			this.mobile_style = 1;//default responsive mode with toggle button inside navbar
			if(this.$sidebar.hasClass('responsive') && !$('.menu-toggler[data-target="#'+this.$sidebar.attr('id')+'"]').hasClass('navbar-toggle')) this.mobile_style = 2;//toggle button behind sidebar
			 else if(this.$sidebar.hasClass(responsive_min_class)) this.mobile_style = 3;//minimized menu
			  else if(this.$sidebar.hasClass('navbar-collapse')) this.mobile_style = 4;//collapsible (bootstrap style)
		}
		sidebar_mobile_style.call(self);
		  
		function update_vars() {
			this.mobile_view = this.mobile_style < 4 && this.is_mobile_view();
			this.collapsible = !this.mobile_view && this.is_collapsible();

			this.minimized = 
			(!this.collapsible && this.$sidebar.hasClass(minimized_menu_class))
			 ||
			(this.mobile_style == 3 && this.mobile_view && this.$sidebar.hasClass(responsive_min_class))

			this.horizontal = !(this.mobile_view || this.collapsible) && this.$sidebar.hasClass(horizontal_menu_class)
		}

		//update some basic variables
		$(window).on('resize.sidebar.vars' , function(){
			update_vars.call(self);
		}).triggerHandler('resize.sidebar.vars')
		
		
		//toggling (show/hide) submenu elements
		this.$sidebar.on(ace.click_event+'.ace.submenu', '.nav-list', function (ev) {
			var nav_list = this;

			//check to see if we have clicked on an element which is inside a .dropdown-toggle element?!
			//if so, it means we should toggle a submenu
			var link_element = $(ev.target).closest('a');
			if(!link_element || link_element.length == 0) return;//return if not clicked inside a link element

			var minimized  = self.minimized && !self.collapsible;
			//if .sidebar is .navbar-collapse and in small device mode, then let minimized be uneffective
	
			if( !link_element.hasClass('dropdown-toggle') ) {//it doesn't have a submenu return
				//just one thing before we return
				//if sidebar is collapsed(minimized) and we click on a first level menu item
				//and the click is on the icon, not on the menu text then let's cancel event and cancel navigation
				//Good for touch devices, that when the icon is tapped to see the menu text, navigation is cancelled
				//navigation is only done when menu text is tapped

				if( ace.click_event == 'tap'
					&&
					minimized
					&&
					link_element.get(0).parentNode.parentNode == nav_list )//only level-1 links
				{
					var text = link_element.find('.menu-text').get(0);
					if( text != null && ev.target != text && !$.contains(text , ev.target) ) {//not clicking on the text or its children
						ev.preventDefault();
						return false;
					}
				}


				//ios safari only has a bit of a problem not navigating to link address when scrolling down
				//specify data-link attribute to ignore this
				if(ace.vars['ios_safari'] && link_element.attr('data-link') !== 'false') {
					//only ios safari has a bit of a problem not navigating to link address when scrolling down
					//please see issues section in documentation
					document.location = link_element.attr('href');
					ev.preventDefault();
					return false;
				}

				return;
			}
			
			ev.preventDefault();
			
			


			var sub = link_element.siblings('.submenu').get(0);
			if(!sub) return false;
			var $sub = $(sub);

			var height_change = 0;//the amount of height change in .nav-list

			var parent_ul = sub.parentNode.parentNode;
			if
			(
				( minimized && parent_ul == nav_list )
				 || 
				( ( $sub.parent().hasClass('hover') && $sub.css('position') == 'absolute' ) && !self.collapsible )
			)
			{
				return false;
			}

			
			var sub_hidden = (sub.scrollHeight == 0)

			//if not open and visible, let's open it and make it visible
			if( sub_hidden && self.settings.hide_open_subs ) {//being shown now
			  $(parent_ul).find('> .open > .submenu').each(function() {
				//close all other open submenus except for the active one
				if(this != sub && !$(this.parentNode).hasClass('active')) {
					height_change -= this.scrollHeight;
					self.hide(this, self.settings.duration, false);
				}
			  })
			}

			if( sub_hidden ) {//being shown now
				self.show(sub, self.settings.duration);
				//if a submenu is being shown and another one previously started to hide, then we may need to update/hide scrollbars
				//but if no previous submenu is being hidden, then no need to check if we need to hide the scrollbars in advance
				if(height_change != 0) height_change += sub.scrollHeight;//we need new updated 'scrollHeight' here
			} else {
				self.hide(sub, self.settings.duration);
				height_change -= sub.scrollHeight;
				//== -1 means submenu is being hidden
			}

			//hide scrollbars if content is going to be small enough that scrollbars is not needed anymore
			//do this almost before submenu hiding begins
			//but when minimized submenu's toggle should have no effect
			if (height_change != 0) {
				if(self.$sidebar.attr('data-sidebar-scroll') == 'true' && !self.minimized) 
					self.$sidebar.ace_sidebar_scroll('prehide', height_change)
			}

			return false;
		});
		
		
	}//end of Sidebar
	

	//sidebar events
	
	//menu-toggler
	$(document)
	.on(ace.click_event+'.ace.menu', '.menu-toggler', function(e){
		var btn = $(this);
		var sidebar = $(btn.attr('data-target'));
		if(sidebar.length == 0) return;
		
		e.preventDefault();
				
		//sidebar.toggleClass('display');
		//btn.toggleClass('display');
		
		sidebar.ace_sidebar('mobileToggle', this);
		
		var click_event = ace.click_event+'.ace.autohide';
		var auto_hide = sidebar.attr('data-auto-hide') === 'true';

		if( btn.hasClass('display') ) {
			//hide menu if clicked outside of it!
			if(auto_hide) {
				$(document).on(click_event, function(ev) {
					if( sidebar.get(0) == ev.target || $.contains(sidebar.get(0), ev.target) ) {
						ev.stopPropagation();
						return;
					}

					sidebar.ace_sidebar('mobileToggle', this, false);
					$(document).off(click_event);
				})
			}

			if(sidebar.attr('data-sidebar-scroll') == 'true') sidebar.ace_sidebar_scroll('reset');
		}
		else {
			if(auto_hide) $(document).off(click_event);
		}

		return false;
	})
	//sidebar collapse/expand button
	.on(ace.click_event+'.ace.menu', '.sidebar-collapse', function(e){
		
		var target = $(this).attr('data-target'), $sidebar = null;
		if(target) $sidebar = $(target);
		if($sidebar == null || $sidebar.length == 0) $sidebar = $(this).closest('.sidebar');
		if($sidebar.length == 0) return;

		e.preventDefault();
		$sidebar.ace_sidebar('toggleMenu', this);
	})
	//this button is used in `mobile_style = 3` responsive menu style to expand minimized sidebar
	.on(ace.click_event+'.ace.menu', '.sidebar-expand', function(e){
		var target = $(this).attr('data-target'), $sidebar = null;
		if(target) $sidebar = $(target);
		if($sidebar == null || $sidebar.length == 0) $sidebar = $(this).closest('.sidebar');
		if($sidebar.length == 0) return;	
	
		var btn = this;
		e.preventDefault();
		$sidebar.ace_sidebar('toggleResponsive', this);
		
		var click_event = ace.click_event+'.ace.autohide';
		if($sidebar.attr('data-auto-hide') === 'true') {
			if( $sidebar.hasClass(responsive_max_class) ) {
				$(document).on(click_event, function(ev) {
					if( $sidebar.get(0) == ev.target || $.contains($sidebar.get(0), ev.target) ) {
						ev.stopPropagation();
						return;
					}

					$sidebar.ace_sidebar('toggleResponsive', btn);
					$(document).off(click_event);
				})
			}
			else {
				$(document).off(click_event);
			}
		}
	})

	
	$.fn.ace_sidebar = function (option, value, value2) {
		var method_call;

		var $set = this.each(function () {
			var $this = $(this);
			var data = $this.data('ace_sidebar');
			var options = typeof option === 'object' && option;

			if (!data) $this.data('ace_sidebar', (data = new Sidebar(this, options)));
			if (typeof option === 'string' && typeof data[option] === 'function') {
				if(value instanceof Array) method_call = data[option].apply(data, value);
				else if(value2 !== undefined) method_call = data[option](value, value2);
				else method_call = data[option](value);
			}
		});

		return (method_call === undefined) ? $set : method_call;
	};
	
	
	$.fn.ace_sidebar.defaults = {
		'duration': 300,
		'hide_open_subs': true
    }


})(window.jQuery);
