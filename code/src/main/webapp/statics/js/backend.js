Date.prototype.format = function(format){
    var o = {
        "M+" : this.getMonth()+1, // month
        "d+" : this.getDate(), // day
        "h+" : this.getHours(), // hour
        "m+" : this.getMinutes(), // minute
        "s+" : this.getSeconds(), // second
        "q+" : Math.floor((this.getMonth()+3) / 3), // quarter
        "S" : this.getMilliseconds() // millisecond
    };
    if(/(y+)/.test(format)) {
        format = format.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length));
    }
    for(var k in o) {
        if(new RegExp("("+ k +")").test(format)) {
            var temp = RegExp.$1.length == 1 ? o[k] : ("00"+ o[k]).substr((""+ o[k]).length);
            format = format.replace(RegExp.$1, temp);
        }
    }
    return format;
};
/**
 * 格式化日期
 */
function Web_formatDateTime(dateVal) {
    if(!dateVal) return '';
    var dateObj = new Date(dateVal);
    return dateObj.format('yyyy-MM-dd hh:mm:ss');
}
/**
 * 获取datagrid已Checked的主键数组
 */
function Web_getDatagridCheckedIdArray($datagrid, idField) {
    var rows = $datagrid.datagrid('getChecked');
    var idField = idField || $datagrid.datagrid('options')['idField'];
    var idArray = [], size = rows.length;
    for(var i = 0; i < size; i++) {
        idArray.push(rows[i][idField]);
    }
    return idArray;
}
/**
 * 格式化URL地址
 */
function Web_generateUrl(url) {
    if(!url) return 'javascript:void(0);';
    if(url.match(/^\w+:\/\/.*$/)) return url;
    return webUrl + url;
}
$.fn.linkbuttonToggle = function () {
    var $obj = $(this);
    if(arguments.length) {
        if(true == $obj.data('toggle')) return true;
        $obj.data('toggle', true);
        var options = $obj.linkbutton('options');
        $obj.data('text', options.text);
        options.text = arguments[0];
        $obj.linkbutton(options);
        return false;
    }
    $obj.data('toggle', false);
    var options = $obj.linkbutton('options');
    options.text = $obj.data('text');
    $obj.linkbutton(options);
}
/**
 * 消息提示
 */
function Web_alertInfo(msg, callBack) {
    $.messager.alert('提示' , msg, 'info', callBack);
}
/**
 * 消息确认
 */
function Web_confirm(msg, callBack) {
    $.messager.confirm('提示' , msg, callBack);
}

/**
 * 刷新当前页面
 */
function Web_refreshCurrentPage() {
    window.location.reload();
}

/**
 * 打开新页面
 */
function Web_openPage(url) {
    window.open(url);
}

/**
 * 页面跳转，若url为整数则前进或后退历史记录
 */
function Web_redirectPage(url) {
    if(isNaN(url)) {
        window.location.href = url;
    } else {
        window.history.go(url);
    }
}

/**
 * 判断对象是否为空
 */
function Web_empty(object) {
    if(typeof object == "undefined") return true;
    if(null == object) return true;
    if(typeof object == "boolean") return !object;
    object += "";
    if(object.length < 1) return true;
    if("0" == object) return true;
    return false;
}

/**
 * 去除字符串两边指定的字符串
 */
function Web_trim(str, trimStr) {
    if(Web_empty(str)) return "";
    if(typeof trimStr == "undefined" || null == trimStr) {
        trimStr = "";
    }
    var regexLeft = eval("/^" + trimStr + "*/");
    str = str.replace(regexLeft, "");
    var regexRight = eval("/" + trimStr + "*$/");
    str = str.replace(regexRight, "");
    return str;
}
/**
 * 去除字符串左边指定的字符串
 */
function Web_trimLeft(str, trimStr) {
    if(Web_empty(str)) return "";
    if(typeof trimStr == "undefined" || null == trimStr) {
        trimStr = "";
    }
    var regexLeft = eval("/^" + trimStr + "*/");
    str = str.replace(regexLeft, "");
    return str;
}
/**
 * 去除字符串右边指定的字符串
 */
function Web_trimRight(str, trimStr) {
    if(Web_empty(str)) return "";
    if(typeof trimStr == "undefined" || null == trimStr) {
        trimStr = "";
    }
    var regexRight = eval("/" + trimStr + "*$/");
    str = str.replace(regexRight, "");
    return str;
}
/**
 * 按照约定规则解析JSON字符串
 */
function Web_parseMessage(json) {
    if(typeof json == "string") {
        try {
            json = $.parseJSON(json);
        } catch (e) {
            json = null;
        }
    }
    if(null == json) {
        json = {
            status : 500,
            message : '登陆超时，或服务器处理异常',
            data : null
        };
    }
    return json;
}
/**
 * 界面通用处理
 */
$(function () {
    var $window = $(window);
    var $platformMenuTop = $('#platform-menu-top');
    var $platformMenuLeft = $('.platform-menu-left');
    var $pageContentFull = $('.page-content-full');
    var $pageContentFullPrev = $pageContentFull.prev();
    var $pageTableList = $pageContentFull.children('.page-table-list');
    $platformMenuTop.children('.platform-menu-item').on('click', function () {
        var id = $(this).data('id');
        $platformMenuLeft.addClass('hide');
        $platformMenuLeft.each(function () {
            var $obj = $(this);
            if($obj.data('parent') != id) return true;
            $obj.removeClass('hide');
            return false;
        });
    });
    if($pageContentFull.length < 1) return;
    $window.on('resize', function () {
        $pageContentFull.height($window.height()
            - $pageContentFullPrev.position().top - $pageContentFullPrev.height() - 2);
        $pageTableList.datagrid('resize');
    }).trigger('resize');
});
