# Unity模组管理器

> 一个便捷的程序可以修改游戏内资源包括：Unity游戏资源，Wwise音频资源

[![Crowdin](https://d322cqt584bo4o.cloudfront.net/unitymodmanager/localized.svg)](https://crowdin.com/project/unitymodmanager)
[![Build Status](https://travis-ci.org/xausky/UnityModManager.svg?branch=master)](https://travis-ci.org/xausky/UnityModManager)
[![GPL Licence](https://badges.frapsoft.com/os/gpl/gpl.svg?v=103)](https://opensource.org/licenses/GPL-3.0/)

# 经过测试的游戏

* 崩坏3

# 使用指引

[2.x版本使用演示视频](https://www.bilibili.com/video/av21793565/)

## 基本使用流程

1. 打开设置页面根据需要进行设置，主要是按照需要修改资源的位置进行设置。
2. 打开主页安装游戏客户端，进入游戏下载附加资源，生成映射文件。
3. 导入模组并且启用，然后点击手柄图标启动游戏。

# 常见问题

* Q：这个管理器是否需要Root权限？  
  A：不需要Root权限可以在虚拟模式运行，Root模式需要Root权限支持。
  
* Q：如何确认我的游戏应该使用怎样的配置？  
  A：配置是按照需要修改的资源路径来的，需要修改APK内资源，那么APK内资源修改方式就要选择虚拟环境模式或者Root模式（需要Root权限），需要修改持久化资源，一般为游戏启动后热更新的资源，那么就要启用持久化资源支持，以上两项如果你不确定可以直接选择虚拟环境模式和启用持久化资源支持，如果你的游戏是从Google Play 商店下载的，那么可能需要启用OBB资源文件支持。
  
* Q：我安装模组后模组没有效果，怎么处理？  
  A：首先先看安装的模组下面的文件图标旁边的数字，如果是0大概率是配置和生成映射问题，返回上一步重新设置和生成映射，当然，也有小概率是模组本身问题，如果是-1，那么大概率是模组问题，小概率是应用没有存储权限，如果大于0那么可以尝试将模组移除（左右滑动删除）再导入试试。
  
* Q：启用游戏时候显示安装模组失败怎么办？  
  A：这个问题有三个可能，大概率为没有OBB资源文件的游戏启用了OBB资源支持，中概率为没有Root权限的设备使用了Root模式，小概率为模组本身问题。
