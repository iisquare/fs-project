# JSoup - Java的HTML解析器

## 表达式
- 声明解析器类型
```
jsoup {}
```
- 定义输出字段或取值方法
```
jsoup {
    key1: title(),
    key2: body().select("query").eq(0).text()
}
```
- 列表循环
```
jsoup {
    key: select("query") [
        key1: attr("attributeKey"),
        key2: text()
    ]
}
```
- 占位符：使用英文中划线“-”标识占位符，内部字段将替换该位置
```
jsoup {
    key1: title(),
    -: body() {
        key2: select("query").text(),
        key3: select("query").val()
    }
}
```

## 方法
- 获取标题：title()
- 获取Body体：body()
- 选择元素：select​(String query)
- 获取元素的属性：attr​(String attributeKey)
- 获取每个元素的属性：eachAttr​(String attributeKey)
- 获取元素内文本：text​()
- 获取每个元素内的文本：eachText​()
- 获取元素内的代码：html()
- 获取元素外的代码：outerHtml​()
- 获取元素的值：val​()
- 选择第N个元素：eq​(int index)
- 选择第一个元素：first​()
- 选择最后一个元素：last​()
- 选择上一个元素：prev​()
- 选择下一个元素：next​()
- 选择父级元素：parents​()

## 参考链接
- [Use selector-syntax to find elements](https://jsoup.org/cookbook/extracting-data/selector-syntax)
- [API Reference](https://jsoup.org/apidocs/index-all.html)
- [Try jsoup](https://try.jsoup.org/)
