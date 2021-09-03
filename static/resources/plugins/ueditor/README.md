# ueditor

## 功能修改
- 修复自动保存Bug
```
# ueditor.all.js
'contentchange': function () {

    if (!me.getOpt('enableAutoSave')) {
        return;
    }

    if ( !saveKey ) {
        return;
    }

```
- 修复页面销毁时，滚动异常
```
# ueditor.all.js
window.onscroll = function(){
    if(lastScrollY === null){
        lastScrollY = this.scrollY
    }else if(this.scrollY == 0 && lastScrollY != 0 && !me){
        me.window.scrollTo(0,0);
        lastScrollY = null;
    }
}
```
- CodeMirror样式，增加.edui-editor前缀
```
# third-party/codemirror/codemirror.css
```
- 移除在线文件浏览功能
```
# dialogs/image/image.html
<span class="tab" data-content-id="online" style="display: none;"><var id="lang_tab_online"></var></span>
<span class="tab" data-content-id="search" style="display: none;"><var id="lang_tab_search"></var></span>

# dialogs/attachment/attachment.html
<span class="tab" data-content-id="online" style="display: none;"><var id="lang_tab_online"></var></span>

# dialogs/background/background.html
<span class="" data-content-id="imgManager" style="display: none;"><var id="lang_background_local"></var></span>
```
