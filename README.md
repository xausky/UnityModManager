# Unity模组管理器

> ⼀个便捷的程序可以给Unity游戏添加模组，修改游戏内模型。

[![Crowdin](https://d322cqt584bo4o.cloudfront.net/unitymodmanager/localized.svg)](https://crowdin.com/project/unitymodmanager)
[![Build Status](https://travis-ci.org/xausky/UnityModManager.svg?branch=master)](https://travis-ci.org/xausky/UnityModManager)
[![GPL Licence](https://badges.frapsoft.com/os/gpl/gpl.svg?v=103)](https://opensource.org/licenses/GPL-3.0/)

# 经过测试的游戏

* 崩坏3

# 使用指引

[2.x版本使用演示视频](https://www.bilibili.com/video/av21793565/)

# 常见问题

* Q：这个管理器是否需要Root权限？
  A：不需要

* Q：是否支持整合包的安装？
  A：2.x版本支持整合包安装。

* Q：是否支持渠道服？
  A：2.x版本将尽力支持渠道服，目前经过测试可以使用的渠道服有：B服(和官服一样使用)，应用宝服/九游服([参考视频](https://www.bilibili.com/video/av24407757/))

* Q：一直显示密码不正确，但是我确定密码是正确的。
  A：有可能是压缩文件损坏，另外请确保压缩文件格式是支持的格式。

* Q：支持的压缩文件格式有哪些？
  A：可以在文件浏览里面看到的压缩格式都是支持的，包括：zip，rar，7z格式。

* Q：我有一个模组是支持压缩文件格式，但是模组导入里面看不到，怎么办？
  A：请确定文件后缀名正确，且保证是半角小写英文字母。

* Q：安卓8/8.1使用虚拟环境模式出现点击输入框导致游戏崩溃，怎么办？
  A：暂时关闭Android系统的自动填充服务，在安卓原版的操作为：设置->系统->语言和输入法->高级->自动填充服务（改为无即可）。

## 实现功能
- [x] 虚拟环境的游戏安装，更新，启动功能
- [x] 附加组件的安装，卸载，启动功能
- [x] 外部应用可见性功能
- [x] 虚拟环境安装应用免签名验证功能
- [x] 内建的文件管理器进行文件选择
- [x] 模组的添加，删除，应用
- [x] zip模组文件的支持
- [x] 目录格式模组文件的支持
- [x] 模组的批量添加功能
- [x] 检测并显示导入模组的有效文件数量
- [x] bundle文件细粒度的资源修改
- [x] rar,7z模组格式的支持
- [x] 模组预览图片的支持
- [x] 往bundle文件里面添加资源文件的支持
- [x] 外部目录模组和自动修改监控功能（主要提供模组制作者使用）
- [x] 持久化目录和OBB目录资源文件的支持。