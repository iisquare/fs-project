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