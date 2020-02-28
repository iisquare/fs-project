/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Develer S.r.L.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
angular.module("angular-flot",[]).directive("flot",["$timeout",function(a){return{restrict:"EA",template:"<div></div>",scope:{dataset:"=",options:"=",callback:"=",onPlotClick:"&",onPlotHover:"&",onPlotSelected:"&"},link:function(b,c,d){var e=null,f=d.width||"100%",g=d.height||"100%";if(((b.options||{}).legend||{}).container instanceof jQuery)throw new Error('Please use a jQuery expression string with the "legend.container" option.');b.dataset||(b.dataset=[]),b.options||(b.options={legend:{show:!1}});var h=$(c.children()[0]);h.css({width:f,height:g});var i=function(){var a=$.plot(h,b.dataset,b.options);return b.callback&&b.callback(a),a};h.on("plotclick",function(c,d,e){a(function(){b.onPlotClick({event:c,pos:d,item:e})})}),h.on("plotselected",function(c,d){a(function(){b.onPlotSelected({event:c,ranges:d})})}),h.on("plothover",function(c,d,e){a(function(){b.onPlotHover({event:c,pos:d,item:e})})});var j=function(){e=i()},k=b.$watch("options",j,!0),l=function(a){return e?(e.setData(a),e.setupGrid(),e.draw()):void(e=i())},m=b.$watch("dataset",l,!0);d.$observe("width",function(a){a&&(f=a,h.css("width",a))}),d.$observe("height",function(a){a&&(g=a,h.css("height",a))}),c.on("$destroy",function(){h.off("plotclick"),h.off("plothover"),h.off("plotselected"),e.shutdown(),m(),k()})}}}]);