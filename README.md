![BackgroundOpt](https://socialify.git.ci/XingC123/BackgroundOpt/image?description=1&language=1&name=1&owner=1&theme=Light)



![LICENSE](https://img.shields.io/github/license/XingC123/BackgroundOpt)![GitHub repo size](https://img.shields.io/github/repo-size/XingC123/BackgroundOpt)![GitHub release (with filter)](https://img.shields.io/github/v/release/XingC123/BackgroundOpt)![GitHub language count](https://img.shields.io/github/languages/count/XingC123/BackgroundOpt)![GitHub top language](https://img.shields.io/github/languages/top/XingC123/BackgroundOpt)



## 🔮 本模块

> **模块名称：** 后台优化
>
> **模块介绍：** 这是一个通过调整进程oom_score来骗过lmk从而实现保后台的模块。(一定程度上，帮助了国内毒瘤完成曾费尽心思想要实现的保持前台运行的效果。因此，在使用此模块的同时，强烈推荐使用可以控制后台进程cpu使用的另一些模块或是软件)



## ✨ 特性

> 1. **oom管理**：app进入后台以后，设置其主进程oom_score = 0(最大为100)，子进程oom_score >= 101(即子进程min_score = 主进程max_score+1)
>
> 2. **内存紧张:** app进入前/后台后，被加入内存紧张列表。该列表以10min为间隔进行轮循，每次运行，会向列表中的app发送特定内存紧张级别。
>
>    **Tip：** 这个功能如果当初研究过qq/微信优化的同学，应该接触过一个叫"QQ/微信负优化"的模块。本模块这个功能便是来自于此
>
> 3. **内存压缩：** app进入后台以后，当进程实际oom_score达到指定规则后，进行内存full压缩



## 🔍 安卓版本要求

- 友好支持安卓12-13(Api 31-33)
- 1.6.1(161)初步支持安卓14

> **测试机：**
>
> - Redmi Note 5 pro(whyred): Android13, nusantara rom
> - Redmi K30 pro(lmi): Android12, MIUI 13 22.7.8
> - Android studio自带虚拟机: Android 14



## 📕 食用指南

1. 安装模块
2. 在Lsposed管理器中启用本模块
3. 重启手机



## 💖 致谢

- [Don-t-Kill](https://github.com/UISSD/Don-t-Kill)
- [NoActive-UI](https://github.com/myflavor/NoActive-UI)



## 💫 Q & A

> 1. Q: MIUI夜间杀后台等做适配了吗？
>
>    A: 已hook
>
> 2. Q: 掉卡片有解决吗？
>
>    A: 按照dont kill作者的教程已hook。我的机器暂时没出现。
>
> 3. Q: 与"scene附加模块(二)"冲突吗？
>
>    A: 不冲突。我也同时在使用。但正如前言所说，当下新机型内核已上zstd+mglru，且第三方内核对内存管理已做优化，并不推荐过于追求保后台，除非你的机器杀后台杀红眼。
>
> 4. Q: 与noactive冲突吗？
>
>    A: 不冲突。
>
>    - 在 **Redmi note5p(whyred), Nusantara, Android 13 + Noactive 2.9**
>
>      - 墓碑效果、app列表、对bili的设置(默认等级为前台服务)、noa的设置页面分别如下: 
>
>      <img src="BackgroundOpt/raw/master/resources/images/墓碑效果.png" width="180" height="360">
>      <img src="BackgroundOpt/raw/master/resources/images/Noactive的app列表.png" width="150" height="360">
>      <img src="BackgroundOpt/raw/master/resources/images/Noactive对某app的设置.png" width="150" height="360">
>      <img src="BackgroundOpt/raw/master/resources/images/Noactive设置.png" width="150" height="360">
>
> 5. Q: 如何知道我又没有食用上呢？
>
>    A: 进入模块主界面, 点击 **待压缩app列表/后台任务列表** , 均可以看到adj信息。
>
>    - 主进程adj = [0, 100]
>
>    - 子进程adj >= 101
>
>      <img src="BackgroundOpt/raw/master/resources/images/待压缩列表app的adj展示.jpg" width="200" height="360">
>
> 6. Q: 我用了以后特别卡怎么解决呢？
>
>    A: 首先，排除所有与本模块无关因素的干扰。然后，由于xp模块的工作原理，在hook点位方法执行前后执行特定代码(不讨论replace)，因此势必会对性能产生一定影响。
>
>    **因此:** 
>
>    - 如果你是因为 **保后台进程过多** 造成卡顿，这个 **不归模块管** (模块也不推荐你这么做)。
>    - 切换前后台掉帧。可以忍受则继续使用，不可忍受则卸载模块。
>    - 信息流滑动掉帧。 **信息流滑动** 的任何事件模块都 **没有进行hook** ，掉帧不关模块的事。



## 💥 注意事项

> 1. 本模块目前(截至2023/10/16), 仅对安卓12/13(初步适配安卓14)做了适配。
> 2. 模块在 **"红米Note5 pro(whyred)，安卓13，nusantara"、"红米K30 pro(lmi)，安卓12，MIUI 13 22.7.8"、"Android studio自带虚拟机, 安卓14"** 测试无问题。
> 3. 任何有关对 **oom_score_adj** 进行调整的模块均与此模块 **冲突**。
> 4. 任何时刻，搞机都要做好砖的准备。如果本模块造成砖机，概不负责(不会真的能砖吧？)，请卸载模块，自行救砖。



## 📌 下载地址

- [Release](https://github.com/XingC123/BackgroundOpt/releases)
- [蓝奏](https://wwok.lanzoub.com/b0fb3n5cf) 密码: 87qt
- [123云盘](https://www.123pan.com/s/EDCTjv-KBa93.html) 提取码:oo1V