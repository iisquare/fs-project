if (!Object.assign) Object.assign = function () {
    var obj = arguments[0];
    for (var i = 1; i < arguments.length; i++) {
        var item = arguments[i];
        for (var key in item) {
            if (!obj[key]) obj[key] = {};
            var value = item[key];
            if (null != value && typeof value == 'object') {
                Object.assign(obj[key], value);
            } else {
                obj[key] = value;
            }
        }
    }
    return obj;
};

var DateUtil = {
    format: function () {
        var time = arguments.length > 0 ? arguments[0] : new Date();
        var fmt = arguments.length > 1 ? arguments[1] : 'yyyy-MM-dd hh:mm:ss';
        if (!time) return '';
        var date = Object.prototype.toString.call(time) === '[object Date]' ? time : new Date(time);
        if (/(y+)/.test(fmt)) {
            fmt = fmt.replace(RegExp.$1, (date.getFullYear() + '').substr(4 - RegExp.$1.length));
        }
        var o = {
            'M+': date.getMonth() + 1,
            'd+': date.getDate(),
            'h+': date.getHours(),
            'm+': date.getMinutes(),
            's+': date.getSeconds()
        }
        for (var k in o) {
            if (new RegExp('(' + k + ')').test(fmt)) {
                var str = o[k] + '';
                fmt = fmt.replace(RegExp.$1, (RegExp.$1.length === 1) ? str : ('00' + str).substr(str.length));
            }
        }
        return fmt;
    }
}
