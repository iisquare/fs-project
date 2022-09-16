ruler.js

@see(https://github.com/MrFrankel/ruler)

======

### Demo
<a href="https://mrfrankel.github.io/ruler/">Demo Page</a>


ruler.js is an HTML5 ruler plugin that provides a set of 'photoshop' like rulers to surround the 'stage' of your authoring tools.
No jquery!
No dependencies!

###Installation
<hr>
<br/>

```terminal
npm install ruler.js --save
```

<br/>
Then
<br/>

```html
<link rel="stylesheet" href="node_modules/ruler.js/dist/ruler.min.css">
<script src="node_modules/ruler.js//dist/ruler.min.js"></script>
```

<br/>

##Getting Started

<hr>
<br/>

```javascript
var myRuler = new ruler({
        container: document.querySelector('#stage'),// reference to DOM element to apply rulers on
        rulerHeight: 15, // thickness of ruler
        fontFamily: 'arial',// font for points
        fontSize: '7px', 
        strokeStyle: 'black',
        lineWidth: 1,
        enableMouseTracking: true,
        enableToolTip: true
    });
```
### Usage 
```javascript
myRuler.api.setPos({x:100, y:100})
/*
change the left, top, positions of the rulers
*/
myRuler.api.setScale(1.5);
/*
change the scale of the points
*/
myRuler.api.toggleRulerVisibility(true);
/*
hide/show rulers
*/
myRuler.api.toggleGuideVisibility(true);
/*
hide/show guides
*/
myRuler.api.clearGuides(true);
/*
get list of guides to store or copy
*/
myRuler.api.getGuides(); // => [{dimension: number, poxX: number: posY: number}...]
/*
set guides from a pre stored list
*/
myRuler.api.setGuides([{dimension: number, poxX: number: posY: number}...]);
/*
clear all guides
*/
myRuler.api.destory();
/*
remove rulers, guides and references;
*/
```


You can also clear a guide line by double clicking on it or dragging it back to the ruler



### Version
1.0.0

### Support

All latest browsers


### Development

Want to contribute? Great!

ruler.js uses grunt.
```sh
$ git clone https://github.com/MrFrankel/ruler.git
$ npm install
$ npm run serve
```

### Todo's

Write Tests


License
----

MIT


