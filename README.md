# tc-anywhere
可以在任何地方添加try catch 操作；目前只是实现了在无返回值的情况下添加try catch操作
# 使用
```groovy
 //project
 repositories {
        maven {
            url "https://dl.bintray.com/ooftf/maven"
        }
    }
    dependencies {
        classpath 'com.ooftf:tc-anywhere:0.0.1'
    }
 //app
 apply plugin: 'tc-anywhere'
 // 第一个参数为要插入tryCatch的类名，后面的为该类的方法名（只支持无返回值的方法）
 tca {
    tc 'com.ooftf.tca.MainActivity', 'test(Lcom/ooftf/tca/MainActivity;)V', 'onCreate(Landroid/os/Bundle;)V'
 }   
```