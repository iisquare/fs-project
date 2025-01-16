<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter()

const randomNum = () => {
  return Math.floor(Math.random() * 9) + 1 + ''
}

const handleBack = () => {
  router.go(-1)
}

onMounted(() => {
  var i = 0, time = 30
  var selector1 = document.querySelector('.firstDigit')
  var selector2 = document.querySelector('.secondDigit')
  var selector3 = document.querySelector('.thirdDigit')
  var loop3 = setInterval(function() {
    if(i > 40) {
      clearInterval(loop3)
      selector3 && (selector3.textContent = '4')
    } else {
      selector3 && (selector3.textContent = randomNum())
      i++
    }
  }, time);
  var loop2 = setInterval(function() {
    if(i > 80) {
      clearInterval(loop2)
      selector2 && (selector2.textContent = '0')
    } else {
      selector2 && (selector2.textContent = randomNum())
      i++
    }
  }, time);
  var loop1 = setInterval(function() {
    if(i > 100) {
      clearInterval(loop1)
      selector1 && (selector1.textContent = '3')
    } else {
      selector1 && (selector1.textContent = randomNum())
      i++
    }
  }, time)
})
</script>

<template>
  <div class="error flex-center">
    <div class="container-floud">
      <div class="col-xs-12 ground-color text-center">
        <div class="container-error">
          <div class="clip"><div class="shadow"><span class="digit thirdDigit"></span></div></div>
          <div class="clip"><div class="shadow"><span class="digit secondDigit"></span></div></div>
          <div class="clip"><div class="shadow"><span class="digit firstDigit"></span></div></div>
          <div class="msg fs-pointer" @click="handleBack">返回<span class="triangle"></span></div>
        </div>
        <h2 class="h1">抱歉，您无权访问该页面</h2>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.error {
  height: 100%;
  text-align: center;
}
.error .clip .shadow {
  height: 180px;
}
.error .clip:nth-of-type(2) .shadow {
  width: 130px;
}
.error .clip:nth-of-type(1) .shadow, .error .clip:nth-of-type(3) .shadow {
  width: 250px;
}
.error .digit {
  width: 150px;
  height: 150px;
  line-height: 150px;
  font-size: 120px;
  font-weight: bold;
}
.error h2 {
  font-size: 32px;
}
.error .msg {
  top: -190px;
  width: 80px;
  height: 80px;
  line-height: 80px;
  font-size: 26px;
}
.error span.triangle {
  top: 70%;
  right: 0%;
  border-left: 20px solid #535353;
  border-top: 15px solid transparent;
  border-bottom: 15px solid transparent;
}
.error .container-error {
  position: relative;
  height: 250px;
}
.error .container-error .clip {
  display: inline-block;
  transform: skew(-45deg);
}
.error .clip .shadow {
  overflow: hidden;
}
.error .clip:nth-of-type(2) .shadow {
  overflow: hidden;
  position: relative;
  box-shadow: inset 20px 0px 20px -15px rgba(150, 150, 150, 0.8), 20px 0px 20px -15px rgba(150, 150, 150, 0.8);
}

.error .clip:nth-of-type(3) .shadow:after, .error .clip:nth-of-type(1) .shadow:after {
  content: "";
  position: absolute;
  right: -8px;
  bottom: 0px;
  z-index: 9999;
  height: 100%;
  width: 10px;
  background: linear-gradient(90deg, transparent, rgba(173, 173, 173, 0.8), transparent);
  border-radius: 50%;
}
.error .clip:nth-of-type(3) .shadow:after {
  left: -8px;
}
.error .digit {
  position: relative;
  top: 8%;
  color: white;
  background: #07B3F9;
  border-radius: 50%;
  display: inline-block;
  transform: skew(45deg);
}
.error .clip:nth-of-type(2) .digit {
  left: -10%;
}
.error .clip:nth-of-type(1) .digit {
  right: -20%;
}
.error .clip:nth-of-type(3) .digit {
  left: -20%;
}
.error h2 {
  color: #A2A2A2;
  font-weight: bold;
  padding-bottom: 20px;
}
.error .msg {
  position: relative;
  z-index: 9999;
  display: block;
  background: #535353;
  color: #A2A2A2;
  border-radius: 50%;
  font-style: italic;
}
.error .triangle {
  position: absolute;
  z-index: 999;
  transform: rotate(45deg);
  content: "";
  width: 0;
  height: 0;
}
@media(max-width: 767px) {
  .error .clip .shadow {
    height: 100px;
  }
  .error .clip:nth-of-type(2) .shadow {
    width: 80px;
  }
  .error .clip:nth-of-type(1) .shadow,
  .error .clip:nth-of-type(3) .shadow {
    width: 100px;
  }
  .error .digit {
    width: 80px;
    height: 80px;
    line-height: 80px;
    font-size: 52px;
  }
  .error h2 {
    font-size: 24px;
  }
  .error .msg {
    top: -110px;
    left: -10px;
    width: 40px;
    height: 40px;
    line-height: 40px;
    font-size: 14px;
  }
  .error span.triangle {
    top: 70%;
    right: -3%;
    border-left: 10px solid #535353;
    border-top: 8px solid transparent;
    border-bottom: 8px solid transparent;
  }
  .error .container-error {
    height: 150px;
  }
}
.overlay {
  position: relative;
  z-index: 20;
}
.ground-color {
  background: white;
}
.item-bg-color {
  background: #EAEAEA
}
.padding-top {
  padding-top: 10px;
}
.padding-bottom {
  padding-bottom: 10px;
}
.padding-vertical {
  padding-top: 10px;
  padding-bottom: 10px;
}
.padding-horizontal {
  padding-left: 10px;
  padding-right: 10px;
}
.padding-all {
  padding: 10px;
}
.no-padding-left {
  padding-left: 0px;
}
.no-padding-right {
  padding-right: 0px;
}
.no-vertical-padding {
  padding-top: 0px;
  padding-bottom: 0px;
}
.no-horizontal-padding {
  padding-left: 0px;
  padding-right: 0px;
}
.no-padding {
  padding: 0px;
}
.margin-top {
  margin-top: 10px;
}
.margin-bottom {
  margin-bottom: 10px;
}
.margin-right {
  margin-right: 10px;
}
.margin-left {
  margin-left: 10px;
}
.margin-horizontal {
  margin-left: 10px;
  margin-right: 10px;
}
.margin-vertical {
  margin-top: 10px;
  margin-bottom: 10px;
}
.margin-all {
  margin: 10px;
}
.no-margin {
  margin: 0px;
}
.no-vertical-margin {
  margin-top: 0px;
  margin-bottom: 0px;
}
.no-horizontal-margin {
  margin-left: 0px;
  margin-right: 0px;
}
.inside-col-shrink {
  margin: 0px 20px;
}
hr {
  margin: 0px;
  padding: 0px;
  border-top: 1px dashed #999;
}
</style>
