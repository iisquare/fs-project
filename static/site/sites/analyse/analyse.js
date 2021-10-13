(function () {
  "use strict";

  const AnalyseUtil = {
      unshift : function () {
          var result = arguments[0];
          result.unshift(...Array.prototype.slice.apply(arguments).slice(1, arguments.length));
          return result;
      },
      domainRoot : function (domain) {
          if(this.isEmpty(domain)) return null;
          if (domain.match(/^(\d+)\.(\d+)\.(\d+)\.(\d+)$/)) return domain;
          domain = domain.split('.');
          if(domain.length < 3) return this.unshift(domain, '').join('.');
          var specials = ['.com.cn', '.net.cn', '.org.cn', '.gov.cn'];
          var result = this.unshift(domain.slice(-2), '').join('.');
          for (let i in specials) {
              if(specials[i] == result) {
                  return this.unshift(domain.slice(-3), '').join('.');
              }
          }
          return result;
      },
      exists : function (item, obj) {
          for (let i in obj) {
              if(item == obj[i]) return true;
          }
          return false;
      },
      random : function (min = 1, max = 999999) {
          return Math.floor(Math.random() * (max - min + 1) + min);
      },
      extend : function () { // 浅拷贝
          if(arguments.length < 1) return {};
          var obj = arguments[0];
          if(!obj instanceof Object) return {};
          for (let i = 1; i < arguments.length; i++) {
              var item = arguments[i];
              if(null == item || !item instanceof Object) continue;
              for (let key in item) {
                  obj[key] = item[key];
              }
          }
          return obj;
      },
      trim : function (str, charlist = ' ') {
          if(null == str) return '';
          if(Object.prototype.toString.call(str) != '[object String]') return '';
          str = str.replace(new RegExp('^[' + charlist + ']+'), '');
          str = str.replace(new RegExp('[' + charlist + ']+$'), '');
          return str;
      },
      isEmpty : function (data) {
          if(null == data) return true;
          if(data instanceof  Array && data.length < 1) return true;
          if(data instanceof  Object && Object.getOwnPropertyNames(data).length < 1) return true;
          if('[object String]' == Object.prototype.toString.call(data) && AnalyseUtil.trim(data).length < 1) return true;
          return false;
      },
      cookie : function (key, value, params = {}) {
          if(1 == arguments.length) {
              let results = document.cookie.split('; ');
              for (let i in results) {
                  let result = results[i].split('=');
                  if(result[0] == key) return result[1];
              }
              return null;
          }
          let result = [];
          result.push([key, value].join('='));
          if(params.domain) result.push(['domain', params.domain].join('='));
          if(params.expires) {
              var date = new Date();
              date.setTime(date.getTime() + params.expires);
              result.push(['expires', date.toGMTString()].join('='));
          }
          if(params.path) result.push(['domain', params.path].join('='));
          document.cookie = result.join('; ');
      },
      kvURLSearchParams (params) {
          const result = {}
          params.forEach((value, key) => {
            result[key] = value
          })
          return result
      }
  };

  var Analyse = function (params = {}) {

      var _params = AnalyseUtil.extend({}, Analyse.defaults, params);
      _params.rs = (new Date()).getTime(); // 统计开始时间
      if(_params.cookieUseRoot) { // Cookie的域名
          _params.domain = AnalyseUtil.domainRoot(_params.cookieDomain);
      } else {
          _params.domain = _params.cookieDomain;
      }
      // 步长计算
      if(sameOrigin(_params.domain, document.referrer, _params.cookieUseRoot)) {
          _params.step = parseInt(AnalyseUtil.cookie(_params.cookiePrefix + _params.cookieStep)); // 历史步长
          if(isNaN(_params.step)) _params.step = 0;
          _params.step++; // 增加步长
      } else { // 非同源来源重新计算步长
          _params.step = 1;
      }
      AnalyseUtil.cookie(_params.cookiePrefix + _params.cookieStep, _params.step, { domain : _params.domain });
      // 用户唯一标识
      _params.uuid = AnalyseUtil.cookie(_params.cookiePrefix + _params.cookieUUID);
      if (!_params.uuid) {
        _params.uuid = uuid();
        AnalyseUtil.cookie(_params.cookiePrefix + _params.cookieUUID, _params.uuid, { domain : _params.domain, expires: 360 * 24 * 60 * 60 * 1000 });
      }

      function uuid () {
        var d = new Date().getTime();
        var d2 = (performance && performance.now && (performance.now() * 1000)) || 0;
        return Array(32).fill('x').join('').replace(/[xy]/g, function(c) {
            var r = Math.random() * 16;
            if(d > 0) {
                r = (d + r) % 16 | 0;
                d = Math.floor(d / 16);
            } else {
                r = (d2 + r) % 16 | 0;
                d2 = Math.floor(d2 / 16);
            }
            return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
      }

      function sameOrigin(domain, referrer, useRoot) {
          if(!referrer) return false;
          var hostname = new URL(referrer).hostname;
          return (useRoot ? AnalyseUtil.domainRoot(hostname) : hostname) == domain;
      }

      function log (data) {
          /**
           * 隐含参数
           * IP : 客户端IP，直接在服务端获取
           * User-Agent ： 浏览器标识
           * Referer ： 当前页面地址（真实的Referer由统计端以参数形式发送）
           */
          var url = _params.logUrl;
          data.si = _params.id; // 统计标识
          data.rs = _params.rs; // 请求开始时间
          data.ui = _params.uuid; // 用户标识
          data.su = document.referrer; // 访问来源
          data.hs = _params.hashQuery; // 请求Hash参数
          if(window.screen) { // 屏幕信息
              data.ds = window.screen.width + 'x' + window.screen.height;
              data.cl = window.screen.colorDepth;
          }
          data.ce = (document.cookie || navigator.cookieEnabled) ? 1 : 0; // Cookie是否启用
          data.ln = navigator.browserLanguage ? navigator.browserLanguage : navigator.language; // 语言
          data.st = _params.step; // 步长
          data.rc = (new Date()).getTime() - _params.rs; // 当前记录距统计开始时间的间隔
          data.nv = window.performance?.getEntriesByType('navigation')?.map(nav => nav.type).shift() ?? ''; // 导航方式
          if(data) url += '?' + new URLSearchParams(buildQuery(data)).toString();
          (new Image()).src = url;
      }

      function buildQuery(data, parent = '') {
          if(!data instanceof Object) return null;
          var querys = {};
          for (let key in data) {
              var item = data[key];
              if(AnalyseUtil.isEmpty(item)) continue;
              if(!AnalyseUtil.isEmpty(parent)) key = parent + _params.paramsSeparator + key;
              if(item instanceof Object) {
                  AnalyseUtil.extend(querys, buildQuery(item, key))
              } else {
                  querys[key] = item;
              }
          }
          return querys;
      }

      this.send = function (type, action = null, params = {}) {
          log({tp : type, ac : action , ps : new URLSearchParams(buildQuery(params)).toString()});
      }

      this.config = function (key, value) {
          if(1 == arguments.length) return _params[key];
          _params[key] = value;
          return true;
      }
  };
  // 解析当前链接
  var urlResult = new URL(window.location.href);
  Analyse.defaults = {
      id: '', // 网站标识
      version: '0.0.1', // 版本号
      logUrl: '//' + process.env.FS_BI_HOST + '/analyse.gif', // 记录地址
      paramsSeparator: '.', // 请求参数键名称的分隔符
      cookiePrefix: 'fs_bi_', // 键值前缀
      cookieDomain: urlResult.hostname, // Cookie种植域名
      cookieUseRoot: true, // 步长是否种植到根域名下
      cookieStep: 'step', // 步长的键名称
      cookieUUID: 'uuid', // 用户唯一标识
      hashQuery: urlResult.hash.startsWith('#') ? urlResult.hash.substring(1) : urlResult.hash, // 页面Hash参数
  };
  // 获取配置参数
  var params = {};
  var scriptRegExp = new RegExp('^.*?/dest/analyse/analyse\.js');
  for (let i in document.scripts) {
      var url = document.scripts[i].src;
      if(!scriptRegExp.test(url)) continue;
      params = AnalyseUtil.kvURLSearchParams(new URL(url).searchParams);
      break;
  }
  // 实例化统计对象
  var $fa = new Analyse(params);
  // 触发统计
  $fa.send('page', 'init', {});
  window.addEventListener('beforeunload', function () {
      $fa.send('page', 'destroy', {});
  });
  window.FA = $fa;
})();
