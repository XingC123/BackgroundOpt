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
# 保留所有的本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}
# 保留了继承自Activity、Application这些类的子类
#-keepclasseswithmembers public class * extends android.app.Activity
#-keepclasseswithmembers public class * extends android.app.Application
#-keepclasseswithmembers public class * extends android.app.Service
#-keepclasseswithmembers public class * extends android.content.BroadcastReceiver
#-keepclasseswithmembers public class * extends android.content.ContentProvider
#-keepclasseswithmembers public class * extends android.preference.Preference
#-keepclasseswithmembers public class * extends android.view.View
#-keepclasseswithmembers public class * extends android.database.sqlite.SQLiteOpenHelper{*;}
## 如果有引用android-support-v4.jar包，可以添加下面这行
#-keepclasseswithmembers public class com.null.test.ui.fragment.** {*;}
##如果引用了v4或者v7包
#-dontwarn android.support.**
## 保留Activity中的方法参数是view的方法，
## 从而我们在layout里面编写onClick就不会影响
#-keepclasseswithmembers class * extends android.app.Activity {
#    public void * (android.view.View);
#}
# 枚举类不能被混淆
-keepclasseswithmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
# 保留自定义控件(继承自View)不能被混淆
#-keepclasseswithmembers public class * extends android.view.View {
#    public <init>(android.content.Context);
#    public <init>(android.content.Context, android.util.AttributeSet);
#    public <init>(android.content.Context, android.util.AttributeSet, int);
#    public void set*(***);
#    *** get* ();
#}
# 保留Parcelable序列化的类不能被混淆
-keepclasseswithmembers class * implements android.os.Parcelable{
    public static final android.os.Parcelable$Creator *;
}
# 保留Serializable 序列化的类不被混淆
-keepclasseswithmembers class * implements java.io.Serializable {
   static final long serialVersionUID;
   private static final java.io.ObjectStreamField[] serialPersistentFields;
   !static !transient <fields>;
   private void writeObject(java.io.ObjectOutputStream);
   private void readObject(java.io.ObjectInputStream);
   java.lang.Object writeReplace();
   java.lang.Object readResolve();
}
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

#-keepnames class * extends android.view.View
#-keepclasseswithmembers class * extends android.app.Fragment {
#    public void setUserVisibleHint(boolean);
#    public void onHiddenChanged(boolean);
#    public void onResume();
#    public void onPause();
#}
#-keepclasseswithmembers class android.support.v4.app.Fragment {
#    public void setUserVisibleHint(boolean);
#    public void onHiddenChanged(boolean);
#    public void onResume();
#    public void onPause();
#}
#-keepclasseswithmembers class * extends android.support.v4.app.Fragment {
#    public void setUserVisibleHint(boolean);
#    public void onHiddenChanged(boolean);
#    public void onResume();
#    public void onPause();
#}

-dontwarn java.awt.Color
-dontwarn java.awt.Font
-dontwarn java.awt.Point

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
# 不混淆消息实体
# 当 前后端版本不一致时也可以进行通信
-keepclasseswithmembers class * implements com.venus.backgroundopt.common.util.message.MessageFlag {
    *;
}
# 不混淆需要持久化的实体
-keepclasseswithmembers class * implements com.venus.backgroundopt.common.entity.preference.JsonPreferenceFlag {
    *;
}
# 模块激活状态的检测
-keepclassmembers class com.venus.backgroundopt.common.environment.CommonProperties {
    boolean isModuleActive();
}
