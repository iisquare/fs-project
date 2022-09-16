const RulerUtil = {
  extend () {
    for (let i = 1; i < arguments.length; i++) {
      for (const key in arguments[i]) {
        if (arguments[i].hasOwnProperty(key)) {
          arguments[0][key] = arguments[i][key]
        }
      }
    }
    return arguments[0]
  },
  pixelize (val) {
    return val + 'px'
  },
  prependChild (container, element) {
    return container.insertBefore(element, container.firstChild)
  },
  addClasss (element, classNames) {
    if (!(classNames instanceof Array)) {
      classNames = [classNames]
    }
    classNames.forEach(name => {
      element.className += ' ' + name
    })
    return element
  },
  removeClasss (element, classNames) {
    let curCalsss = element.className
    if (!(classNames instanceof Array)) {
      classNames = [classNames]
    }
    classNames.forEach(name => {
      curCalsss = curCalsss.replace(name, '')
    })
    element.className = curCalsss
    return element
  }
}

export default RulerUtil
