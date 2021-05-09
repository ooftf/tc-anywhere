# tc-anywhere
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.ooftf/tc-anywhere/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.ooftf/tc-anywhere)

要解决的问题：有一些第三方包中存在一些崩溃问题，如果能try catch处理并不会影响程序运行，这时候就可以使用tc-anywhere来处理  
目标：可以在任何地方添加try catch 操作；（目前只是实现了在方法无返回值的情况下添加try catch操作）
# 使用
```groovy
 //project
 repositories {
        mavenCentral()
 }
 dependencies {
       classpath 'com.github.ooftf:tc-anywhere:0.0.1'
 }
 //app
 apply plugin: 'tc-anywhere'
 // 第一个参数为要插入tryCatch的类名，后面的为该类的方法名（只支持无返回值的方法）
 tca {
    tc 'com.ooftf.tca.MainActivity', 'test(I)V', 'onCreate(Landroid/os/Bundle;)V'
 }   
```
