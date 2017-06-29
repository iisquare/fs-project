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
$(function () {
    var $platformMenuTop = $('#platform-menu-top');
    var $platformMenuLeft = $('#platform-menu-left');
    $.ajax({
        url: webUrl + '/menu/tree/',
        dataType: 'json',
        success: function (result) {
            if (!result || 0 != result.code) return;
            var html = [];
            for (var i in result.data) {
                var item = result.data[i];
                html.push('<li class="platform-menu-item" data-index="', i, '"><a href="',
                    Web_generateUrl(item.url), '"><i class="', item.icon, '"></i> ', item.name, ' </a></li>');
            }
            $platformMenuTop.html(html.join(''));
            $platformMenuTop.children('.platform-menu-item').on('click', function () {
                var data = result.data[$(this).data('index')];
                var html = [];
                for (var i in data.children) {
                    var item = data.children[i];
                    html.push('<li class="platform-menu-item">');
                    html.push('<a href="', Web_generateUrl(item.url), '" class="dropdown-toggle">');
                    html.push('<i class="menu-icon ', item.icon, '"></i>');
                    html.push('<span class="menu-text"> ', item.name, ' </span>');
                    if(item.children.length > 0) html.push(' <b class="arrow fa fa-angle-down"></b>');
                    html.push('</a><b class="arrow"></b>');
                    if(item.children.length < 1) {
                        html.push('</li>');
                        continue;
                    }
                    html.push('<ul class="submenu">');
                    for (var j in item.children) {
                        var child = item.children[j];
                        html.push('<li class="platform-menu-item"><a href="', Web_generateUrl(child.url), '">');
                        html.push('<i class="menu-icon fa fa-caret-right"></i> ', child.name, ' </a>');
                        html.push('<b class="arrow"></b></li>');
                    }
                    html.push('</ul></li>');
                }
                $platformMenuLeft.html(html.join(''));
            }).eq(0).trigger('click');
        }
    });
});
