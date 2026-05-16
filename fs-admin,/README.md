# JAVA代码

## 运行环境
- JDK17
- Gradle7.6.5

### 指令
- compileOnly：自java插件，适用于编译期需要而不需要打包的情况。
- runtimeOnly(runtime)：自java插件,只在运行期有效,编译时不需要。
- implementation(compile)：自java插件,在编译、运行时都有效，无传递性。
- testImplementation：自java插件,用于编译测试的依赖项，运行时不需要。
- testRuntimeOnly(testRuntime)：自java插件,只在测试运行时需要。
- testImplementation(testCompile)：自java插件,针对测试代码。
- providedCompile：自war插件，引入web容器依赖，但不打包到war中。
- api：自java-library插件,这些依赖项可以传递性地导出给使用者，用于编译时和运行时。
- compileOnlyApi：自java-library插件,在声明模块和使用者在编译时需要的依赖项，但在运行时不需要。


### IDEA Preferences
- File > Settings > Editor > File Types > Ignore files and folders > add fronted
- File > Settings > Plugins > Browse repositories > add lombok
- Settings > Build > Compiler > Annotation Processors > Enable annotation processing
- Settings > Build > Compiler > Build project automatically
- Settings > Build > Build Tools > Gradle > Build and run using:IntelliJ IDEA
- Run/Debug Configurations > Running Application Update Policies > * > Update classes and resources


## 参考
- [Gradle Build Language Reference](https://docs.gradle.org/current/dsl/index.html)
