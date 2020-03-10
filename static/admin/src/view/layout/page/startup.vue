<template>
  <div id="main">
    <div id="level">
      <div id="content">
        <div id="gears">
          <div id="gears-static"></div>
          <div id="gear-system-1">
            <div class="shadow" id="shadow15"></div>
            <div id="gear15"></div>
            <div class="shadow" id="shadow14"></div>
            <div id="gear14"></div>
            <div class="shadow" id="shadow13"></div>
            <div id="gear13"></div>
          </div>
          <div id="gear-system-2">
            <div class="shadow" id="shadow10"></div>
            <div id="gear10"></div>
            <div class="shadow" id="shadow3"></div>
            <div id="gear3"></div>
          </div>
          <div id="gear-system-3">
            <div class="shadow" id="shadow9"></div>
            <div id="gear9"></div>
            <div class="shadow" id="shadow7"></div>
            <div id="gear7"></div>
          </div>
          <div id="gear-system-4">
            <div class="shadow" id="shadow6"></div>
            <div id="gear6"></div>
            <div id="gear4"></div>
          </div>
          <div id="gear-system-5">
            <div class="shadow" id="shadow12"></div>
            <div id="gear12"></div>
            <div class="shadow" id="shadow11"></div>
            <div id="gear11"></div>
            <div class="shadow" id="shadow8"></div>
            <div id="gear8"></div>
          </div>
          <div class="shadow" id="shadow1"></div>
          <div id="gear1"></div>
          <div id="gear-system-6">
            <div class="shadow" id="shadow5"></div>
            <div id="gear5"></div>
            <div id="gear2"></div>
          </div>
          <div class="shadow" id="shadowweight"></div>
          <div id="chain-circle"></div>
          <div id="chain">{{ tip }}</div>
          <div id="weight"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import DataUtil from '@/utils/data'

export default {
  data () {
    return {
      tip: '载入中'
    }
  },
  computed: {
    ready () {
      return this.$store.state.user.ready
    }
  },
  methods: {
    completed () {
      this.tip = this.ready ? '载入成功' : '载入失败'
      let url = this.$router.currentRoute.query.redirect
      if (DataUtil.empty(url)) url = '/'
      this.$router.push(url)
    }
  },
  watch: {
    ready (newValue, oldValue) {
      if (newValue) this.completed()
    }
  },
  mounted () {
    if (this.ready) this.completed()
  }
}
</script>

<style scoped>
#main {
  margin: 0px;
  padding: 0px;
  background: #196a73; /* Old browsers */
  background-image: url(/bg.gif);
  width: 100%;
  height: 100%;
}
#level {
  width: 100%;
  height: 1px;
  position: absolute;
  top: 50%;
}
#content {
  text-align: center;
  margin-top: -327px;
}
#gears {
  width: 478px;
  height: 655px;
  position: relative;
  display: inline-block;
  vertical-align: middle;
}
#gears-static {
  background: url(/FgFnjks.png) no-repeat -363px -903px;
  width: 329px;
  height: 602px;
  position: absolute;
  bottom: 5px;
  right: 0px;
  opacity: 0.4;
}
#title {
  vertical-align: middle;
  color: #9eb7b5;
  width: 43%;
  display: inline-block;
}
#title h1 {
  font-family: 'PTSansNarrowBold', sans-serif;
  font-size: 3.6em;
  text-shadow: rgba(0, 0, 0, 0.36) 7px 7px 10px;
}
#title p {
  font-family: sans-serif;
  font-size: 1.2em;
  line-height: 148%;
  text-shadow: rgba(0, 0, 0, 0.36) 1px 1px 0px;
}

.shadow {
  -webkit-box-shadow: 4px 7px 25px 10px rgba(43, 36, 0, 0.36);
  -moz-box-shadow: 4px 7px 25px 10px rgba(43, 36, 0, 0.36);
  box-shadow: 4px 7px 25px 10px rgba(43, 36, 0, 0.36);
}

/*gear-system-1*/
#gear15 {
  background: url(/FgFnjks.png) no-repeat 0 -993px;
  width: 321px;
  height: 321px;
  position: absolute;
  left: 45px;
  top: 179px;

  -webkit-animation: rotate-back 24000ms linear infinite;
  -moz-animation: rotate-back 24000ms linear infinite;
  -ms-animation: rotate-back 24000ms linear infinite;
  animation: rotate-back 24000ms linear infinite;
}
#shadow15 {
  width: 306px;
  height: 306px;
  -webkit-border-radius: 153px;
  -moz-border-radius: 153px;
  border-radius: 153px;
  position: absolute;
  left: 52px;
  top: 186px;
}
#gear14 {
  background: url(/FgFnjks.png) no-repeat 0 -856px;
  width: 87px;
  height: 87px;
  position: absolute;
  left: 162px;
  top: 296px;
}
#shadow14 {
  width: 70px;
  height: 70px;
  -webkit-border-radius: 35px;
  -moz-border-radius: 35px;
  border-radius: 35px;
  position: absolute;
  left: 171px;
  top: 304px;
}
#gear13 {
  background: url(/FgFnjks.png) no-repeat 0 -744px;
  width: 62px;
  height: 62px;
  position: absolute;
  left: 174px;
  top: 309px;

  -webkit-animation: rotate 8000ms linear infinite;
  -moz-animation: rotate 8000ms linear infinite;
  -ms-animation: rotate 8000ms linear infinite;
  animation: rotate 8000ms linear infinite;
}
#shadow13 {
  width: 36px;
  height: 36px;
  -webkit-border-radius: 18px;
  -moz-border-radius: 18px;
  border-radius: 18px;
  position: absolute;
  left: 187px;
  top: 322px;
}

/*gear-system-2*/
#gear10 {
  background: url(/FgFnjks.png) no-repeat 0 -184px;
  width: 122px;
  height: 122px;
  position: absolute;
  left: 175px;
  top: 0;

  -webkit-animation: rotate-back 8000ms linear infinite;
  -moz-animation: rotate-back 8000ms linear infinite;
  -ms-animation: rotate-back 8000ms linear infinite;
  animation: rotate-back 8000ms linear infinite;
}
#shadow10 {
  width: 86px;
  height: 86px;
  -webkit-border-radius: 43px;
  -moz-border-radius: 43px;
  border-radius: 43px;
  position: absolute;
  left: 193px;
  top: 18px;
}
#gear3 {
  background: url(/FgFnjks.png) no-repeat 0 -1493px;
  width: 85px;
  height: 84px;
  position: absolute;
  left: 194px;
  top: 19px;

  -webkit-animation: rotate 10000ms linear infinite;
  -moz-animation: rotate 10000ms linear infinite;
  -ms-animation: rotate 10000ms linear infinite;
  animation: rotate 10000ms linear infinite;
}
#shadow3 {
  width: 60px;
  height: 60px;
  -webkit-border-radius: 30px;
  -moz-border-radius: 30px;
  border-radius: 30px;
  position: absolute;
  left: 206px;
  top: 31px;
}

/*gear-system-3*/
#gear9 {
  background: url(/FgFnjks.png) no-repeat -371px -280px;
  width: 234px;
  height: 234px;
  position: absolute;
  left: 197px;
  top: 96px;

  -webkit-animation: rotate 12000ms linear infinite;
  -moz-animation: rotate 12000ms linear infinite;
  -ms-animation: rotate 12000ms linear infinite;
  animation: rotate 12000ms linear infinite;
}
#shadow9 {
  width: 200px;
  height: 200px;
  -webkit-border-radius: 100px;
  -moz-border-radius: 100px;
  border-radius: 100px;
  position: absolute;
  left: 214px;
  top: 113px;
}
#gear7 {
  background: url(/FgFnjks.png) no-repeat -371px 0;
  width: 108px;
  height: 108px;
  position: absolute;
  left: 260px;
  top: 159px;

  -webkit-animation: rotate-back 10000ms linear infinite;
  -moz-animation: rotate-back 10000ms linear infinite;
  -ms-animation: rotate-back 10000ms linear infinite;
  animation: rotate-back 10000ms linear infinite;
}
#shadow7 {
  width: 76px;
  height: 76px;
  -webkit-border-radius: 38px;
  -moz-border-radius: 38px;
  border-radius: 38px;
  position: absolute;
  left: 276px;
  top: 175px;
}

/*gear-system-4*/
#gear6 {
  background: url(/FgFnjks.png) no-repeat 0 -1931px;
  width: 134px;
  height: 134px;
  position: absolute;
  left: 305px;
  bottom: 212px;

  -webkit-animation: rotate-back 10000ms linear infinite;
  -moz-animation: rotate-back 10000ms linear infinite;
  -ms-animation: rotate-back 10000ms linear infinite;
  animation: rotate-back 10000ms linear infinite;
}
#shadow6 {
  width: 98px;
  height: 98px;
  -webkit-border-radius: 49px;
  -moz-border-radius: 49px;
  border-radius: 49px;
  position: absolute;
  left: 323px;
  bottom: 230px;
}
#gear4 {
  background: url(/FgFnjks.png) no-repeat 0 -1627px;
  width: 69px;
  height: 69px;
  position: absolute;
  left: 337px;
  bottom: 245px;

  -webkit-animation: rotate-back 10000ms linear infinite;
  -moz-animation: rotate-back 10000ms linear infinite;
  -ms-animation: rotate-back 10000ms linear infinite;
  animation: rotate-back 10000ms linear infinite;
}

/*gear-system-5*/
#gear12 {
  background: url(/FgFnjks.png) no-repeat 0 -530px;
  width: 164px;
  height: 164px;
  position: absolute;
  left: 208px;
  bottom: 85px;
  -webkit-animation: rotate 8000ms linear infinite;
  -moz-animation: rotate 8000ms linear infinite;
  -ms-animation: rotate 8000ms linear infinite;
  animation: rotate 8000ms linear infinite;
}
#shadow12 {
  width: 124px;
  height: 124px;
  -webkit-border-radius: 62px;
  -moz-border-radius: 62px;
  border-radius: 62px;
  position: absolute;
  left: 225px;
  bottom: 107px;
}
#gear11 {
  background: url(/FgFnjks.png) no-repeat 0 -356px;
  width: 125px;
  height: 124px;
  position: absolute;
  left: 228px;
  bottom: 105px;

  -webkit-animation: rotate-back 10000ms linear infinite;
  -moz-animation: rotate-back 10000ms linear infinite;
  -ms-animation: rotate-back 10000ms linear infinite;
  animation: rotate-back 10000ms linear infinite;
}
#shadow11 {
  width: 88px;
  height: 88px;
  -webkit-border-radius: 44px;
  -moz-border-radius: 44px;
  border-radius: 44px;
  position: absolute;
  left: 247px;
  bottom: 123px;
}
#gear8 {
  background: url(/FgFnjks.png) no-repeat -371px -158px;
  width: 72px;
  height: 72px;
  position: absolute;
  left: 254px;
  bottom: 131px;
  -webkit-animation: rotate 6000ms linear infinite;
  -moz-animation: rotate 6000ms linear infinite;
  -ms-animation: rotate 6000ms linear infinite;
  animation: rotate 6000ms linear infinite;
}
#shadow8 {
  width: 42px;
  height: 42px;
  -webkit-border-radius: 21px;
  -moz-border-radius: 21px;
  border-radius: 21px;
  position: absolute;
  left: 269px;
  bottom: 146px;
}

/*gear1*/
#gear1 {
  background: url(/FgFnjks.png) no-repeat 0 0;
  width: 135px;
  height: 134px;
  position: absolute;
  left: 83px;
  bottom: 111px;

  -webkit-animation: rotate-back 10000ms linear infinite;
  -moz-animation: rotate-back 10000ms linear infinite;
  -ms-animation: rotate-back 10000ms linear infinite;
  animation: rotate-back 10000ms linear infinite;
}
#shadow1 {
  width: 96px;
  height: 96px;
  -webkit-border-radius: 48px;
  -moz-border-radius: 48px;
  border-radius: 48px;
  position: absolute;
  left: 103px;
  bottom: 130px;
}

/*gear-system-6*/
#gear5 {
  background: url(/FgFnjks.png) no-repeat 0 -1746px;
  width: 134px;
  height: 135px;
  position: absolute;
  left: 22px;
  top: 108px;
  -webkit-animation: rotate 10000ms linear infinite alternate;
  -moz-animation: rotate 10000ms linear infinite alternate;
  -ms-animation: rotate 10000ms linear infinite alternate;
  animation: rotate 10000ms linear infinite alternate;
}
#shadow5 {
  width: 96px;
  height: 96px;
  -webkit-border-radius: 48px;
  -moz-border-radius: 48px;
  border-radius: 48px;
  position: absolute;
  left: 41px;
  top: 127px;
}
#gear2 {
  background: url(/FgFnjks.png) no-repeat 0 -1364px;
  width: 80px;
  height: 79px;
  position: absolute;
  left: 49px;
  top: 136px;
  -webkit-animation: rotate-back 10000ms linear infinite alternate;
  -moz-animation: rotate-back 10000ms linear infinite alternate;
  -ms-animation: rotate-back 10000ms linear infinite alternate;
  animation: rotate-back 10000ms linear infinite alternate;
}

/*weight*/
#weight {
  background: url(/FgFnjks.png) no-repeat -371px -564px;
  width: 34px;
  height: 92px;
  position: absolute;
  left: 1px;
  bottom: 0;
  -webkit-animation: up 10000ms linear infinite alternate;
  -moz-animation: up 10000ms linear infinite alternate;
  -ms-animation: up 10000ms linear infinite alternate;
  animation: up 10000ms linear infinite alternate;
}
#shadowweight {
  width: 10px;
  height: 80px;
  position: absolute;
  left: 12px;
  bottom: 0px;
  -webkit-animation: up 10000ms linear infinite alternate;
  -moz-animation: up 10000ms linear infinite alternate;
  -ms-animation: up 10000ms linear infinite alternate;
  animation: up 10000ms linear infinite alternate;
}

/*chain*/
#chain-circle {
  background: url(/FgFnjks.png) no-repeat -371px -706px;
  width: 146px;
  height: 147px;
  position: absolute;
  left: 17px;
  top: 102px;
  -webkit-animation: rotate 10000ms linear infinite alternate;
  -moz-animation: rotate 10000ms linear infinite alternate;
  -ms-animation: rotate 10000ms linear infinite alternate;
  animation: rotate 10000ms linear infinite alternate;
}
#chain {
  width: 1px;
  height: 380px;
  border-left: 2px dotted #c8d94a;
  position: absolute;
  left: 17px;
  top: 175px;
  opacity: 0.7;
  -webkit-animation: collapse 10000ms linear infinite alternate;
  -moz-animation: collapse 10000ms linear infinite alternate;
  -ms-animation: collapse 10000ms linear infinite alternate;
  animation: collapse 10000ms linear infinite alternate;
}

/*ANIMATIONS*/
/*rotate*/
@keyframes rotate {
  from {
    -webkit-transform: rotate(0deg);
    -moz-transform: rotate(0deg);
    -o-transform: rotate(0deg);
    -ms-transform: rotate(0deg);
    transform: rotate(0deg);
  }
  to {
    -webkit-transform: rotate(360deg);
    -moz-transform: rotate(360deg);
    -o-transform: rotate(360deg);
    -ms-transform: rotate(360deg);
    transform: rotate(360deg);
  }
}

@-moz-keyframes rotate {
  from {
    -moz-transform: rotate(0deg);
    transform: rotate(0deg);
  }
  to {
    -moz-transform: rotate(360deg);
    transform: rotate(360deg);
  }
}

@-webkit-keyframes rotate {
  from {
    -webkit-transform: rotate(0deg);
    transform: rotate(0deg);
  }
  to {
    -webkit-transform: rotate(360deg);
    transform: rotate(360deg);
  }
}

@-ms-keyframes rotate {
  from {
    -ms-transform: rotate(0deg);
    transform: rotate(0deg);
  }
  to {
    -ms-transform: rotate(360deg);
    transform: rotate(360deg);
  }
}

@-o-keyframes rotate {
  from {
    -o-transform: rotate(0deg);
    transform: rotate(0deg);
  }
  to {
    -o-transform: rotate(360deg);
    transform: rotate(360deg);
  }
}
/*rotate-back*/
@keyframes rotate-back {
  from {
    -webkit-transform: rotate(0deg);
    -moz-transform: rotate(0deg);
    -o-transform: rotate(0deg);
    -ms-transform: rotate(0deg);
    transform: rotate(0deg);
  }
  to {
    -webkit-transform: rotate(-360deg);
    -moz-transform: rotate(-360deg);
    -o-transform: rotate(-360deg);
    -ms-transform: rotate(-360deg);
    transform: rotate(-360deg);
  }
}

@-moz-keyframes rotate-back {
  from {
    -moz-transform: rotate(0deg);
    transform: rotate(0deg);
  }
  to {
    -moz-transform: rotate(-360deg);
    transform: rotate(-360deg);
  }
}

@-webkit-keyframes rotate-back {
  from {
    -webkit-transform: rotate(0deg);
    transform: rotate(0deg);
  }
  to {
    -webkit-transform: rotate(-360deg);
    transform: rotate(-360deg);
  }
}

@-ms-keyframes rotate-back {
  from {
    -ms-transform: rotate(0deg);
    transform: rotate(0deg);
  }
  to {
    -ms-transform: rotate(-360deg);
    transform: rotate(-360deg);
  }
}

@-o-keyframes rotate-back {
  from {
    -o-transform: rotate(0deg);
    transform: rotate(0deg);
  }
  to {
    -o-transform: rotate(-360deg);
    transform: rotate(-360deg);
  }
}
/*weight up*/
@keyframes up {
  from {
    bottom: 0px;
  }
  to {
    bottom: 340px;
  }
}

@-moz-keyframes up {
  from {
    bottom: 0px;
  }
  to {
    bottom: 340px;
  }
}

@-webkit-keyframes up {
  from {
    bottom: 0px;
  }
  to {
    bottom: 340px;
  }
}

@-ms-keyframes up {
  from {
    bottom: 0px;
  }
  to {
    bottom: 340px;
  }
}

@-o-keyframes up {
  from {
    bottom: 0px;
  }
  to {
    bottom: 340px;
  }
}
/*chain up and down*/
@keyframes collapse {
  from {
    height: 387px;
  }
  to {
    height: 48px;
  }
}

@-moz-keyframes collapse {
  from {
    height: 387px;
  }
  to {
    height: 48px;
  }
}

@-webkit-keyframes collapse {
  from {
    height: 387px;
  }
  to {
    height: 48px;
  }
}

@-ms-keyframes collapse {
  from {
    height: 387px;
  }
  to {
    height: 48px;
  }
}

@-o-keyframes collapse {
  from {
    height: 387px;
  }
  to {
    height: 48px;
  }
}
</style>
