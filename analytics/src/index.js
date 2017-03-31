/**
 * 统计触发器
 */
(function (parent) {
	if(typeof parent == "undefined") return;
	if(typeof parent.Analytics != "undefined") return;
	var Analytics = function () {};
	Analytics.trigger = function (page, eve, params) {
		console.log(this);
	};
	parent.Analytics = Analytics;
	// 测试模块
	Analytics.trigger();
})(window);