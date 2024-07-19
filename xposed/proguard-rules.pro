#==================================【基本配置】==================================
# 代码混淆压缩比，在0~7之间，默认为5,一般不下需要修改
-optimizationpasses 5
# 混淆时不使用大小写混合，混淆后的类名为小写
# windows下的同学还是加入这个选项吧(windows大小写不敏感)
-dontusemixedcaseclassnames
# 指定不去忽略非公共的库的类
# 默认跳过，有些情况下编写的代码与类库中的类在同一个包下，并且持有包中内容的引用，此时就需要加入此条声明
-dontskipnonpubliclibraryclasses
# 指定不去忽略非公共的库的类的成员
-dontskipnonpubliclibraryclassmembers
# 不做预检验，preverify是proguard的四个步骤之一
# Android不需要preverify，去掉这一步可以加快混淆速度
-dontpreverify
# 有了verbose这句话，混淆后就会生成映射文件
-verbose
#apk 包内所有 class 的内部结构
-dump class_files.txt
#未混淆的类和成员
-printseeds seeds.txt
#列出从 apk 中删除的代码
-printusage unused.txt
#混淆前后的映射
-printmapping mapping.txt
# 指定混淆时采用的算法，后面的参数是一个过滤器
# 这个过滤器是谷歌推荐的算法，一般不改变
-optimizations !code/simplification/artithmetic,!field/*,!class/merging/*
# 保护代码中的Annotation不被混淆
# 这在JSON实体映射时非常重要，比如fastJson
-keepattributes *Annotation*
# 避免混淆泛型
# 这在JSON实体映射时非常重要，比如fastJson
-keepattributes Signature
# 抛出异常时保留代码行号
-keepattributes SourceFile,LineNumberTable
#忽略警告
#-ignorewarning
#==================================【项目配置】==================================
# 对R文件下的所有类及其方法，都不能被混淆
#-keepclassmembers class **.R$* {
#    *;
#}
## 对于带有回调函数onXXEvent的，不能混淆
#-keepclassmembers class * {
#    void *(**On*Event);
#}
#内部方法
-keepattributes EnclosingMethod
#eventbus
-keepattributes *Annotation*
#-dontwarn javax.annotation.**
#保留混淆mapping文件
-printmapping build/outputs/mapping/release/mapping.txt

#-keep class kotlin.** { *; }
#-keep class org.jetbrains.** { *; }

#FastJson反混淆
#-dontwarn com.alibaba.fastjson2.**
#-keepclasseswithmembers class com.alibaba.fastjson2.**{*; }

################################################################
# 一些missing_rules                                             #
################################################################
-dontwarn java.lang.invoke.StringConcatFactory

################################################################
# 自定义规则                                                     #
################################################################
# Hook的入口
-keep class com.venus.backgroundopt.xposed.MainHook

# 原生实体适配规则接口
# -keepclasseswithmembers class * implements com.venus.backgroundopt.xposed.entity.base.IEntityCompatRule
# -keepclasseswithmembers interface * extends com.venus.backgroundopt.xposed.entity.base.IEntityCompatRule


#-keepclasseswithmembers class * {
#    @com.venus.backgroundopt.xposed.entity.base.IEntityCompatMethod static <methods>;
#}

-keepclassmembers class * implements com.venus.backgroundopt.xposed.entity.base.IEntityCompatFlag {
    # @com.venus.backgroundopt.xposed.entity.base.IEntityCompatMethod <methods>;
    @com.venus.backgroundopt.xposed.entity.base.IEntityCompatMethod static <methods>;
}

