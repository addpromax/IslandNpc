# TypeWriter 事件不触发问题修复指南

## 问题描述

你在 TypeWriter 中创建了 `island_npc_interact_event` 事件，但点击 NPC 没有反应。

## 问题原因

**TypeWriter Extension 缺少事件监听器**，虽然定义了 `island_npc_interact_event` 事件条目，但没有实际监听 NPC 交互并触发 TypeWriter 事件系统。

## 解决方案

### 步骤 1: 更新 TypeWriter Extension 代码

我已经为你创建了以下文件：

#### 1. 新增监听器 `IslandNpcInteractListener.kt`

**位置：** `参考代码/TypeWriter/extensions/IslandNpcExtension/src/main/kotlin/com/magicbili/islandnpc/typewriter/listeners/IslandNpcInteractListener.kt`

**功能：**
- 监听 Citizens NPC 右键点击事件
- 检查是否有匹配的 TypeWriter 事件
- 触发 TypeWriter 的对话/任务系统

#### 2. 更新服务类 `IslandNpcTypeWriterService.kt`

**功能：**
- 在初始化时注册监听器
- 在关闭时注销监听器

### 步骤 2: 重新构建 TypeWriter Extension

在项目根目录执行：

```powershell
cd "d:\系统数据\Desktop\服务器自定义插件\IslandNpc\参考代码\TypeWriter\extensions"
.\gradlew.bat clean build
```

构建后的文件位置：
```
IslandNpcExtension\build\libs\IslandNpcExtension-<version>.jar
```

### 步骤 3: 部署到服务器

1. **停止服务器**
   ```
   /stop
   ```

2. **复制 Extension 到服务器**
   ```powershell
   copy "IslandNpcExtension\build\libs\IslandNpcExtension-*.jar" "<服务器路径>\plugins\TypeWriter\extensions\"
   ```

3. **启动服务器**

### 步骤 4: 启用调试模式

编辑 `config.yml`：
```yaml
debug: true
```

然后重载配置：
```
/islandnpc reload
```

### 步骤 5: 验证安装

启动服务器后，检查日志中是否有以下信息：

```
[INFO] [IslandNpc Extension] 正在初始化...
[INFO] [IslandNpc-TypeWriter] 服务已初始化
[INFO] [IslandNpc-TypeWriter] 已注册 NPC 交互监听器
[INFO] [IslandNpc Extension] 初始化完成
```

## TypeWriter 事件配置检查清单

### 1. 创建 NPC 引用

在 TypeWriter 编辑器的 Manifest 页面：

```yaml
类型: island_npc_reference
ID: my_island_npc
名称: 岛屿 NPC
显示名称: "&6&l岛屿管理员"
```

### 2. 创建交互事件

```yaml
类型: island_npc_interact_event
ID: my_npc_interact
名称: 岛屿 NPC 交互
NPC 引用: my_island_npc  # 选择上面创建的引用
只响应本岛屿: true
触发器:
  - 选择你的对话或任务入口
```

**重要：** 必须配置 **触发器（Triggers）**，否则事件不会被触发！

### 3. 创建对话或任务

在触发器中选择一个对话条目或任务入口，例如：

```yaml
类型: dialogue
ID: welcome_dialogue
名称: 欢迎对话
对话内容:
  - "欢迎来到你的岛屿！"
  - "我是岛屿管理员。"
```

## 测试步骤

### 1. 创建测试岛屿
```
/is create
```

### 2. 确认 NPC 已生成

NPC 应该在岛屿中心附近生成。

### 3. 右键点击 NPC

观察日志输出（开启 debug 模式）：

**正常流程：**
```
[DEBUG] [交互] 玩家 xxx 与岛屿 NPC 交互
[IslandNpc-TypeWriter] [交互监听器] 找到 1 个匹配的 TypeWriter 事件
[IslandNpc-TypeWriter] [交互监听器] 触发事件: 岛屿 NPC 交互 (ID: my_npc_interact)
[IslandNpc-TypeWriter] [交互监听器] 成功触发 TypeWriter 事件: 岛屿 NPC 交互
[DEBUG] [交互] 检测到活跃的 TypeWriter 岛屿 NPC 事件
[DEBUG] [交互] 触发 TypeWriter 交互
```

**如果没有事件：**
```
[DEBUG] [交互] 玩家 xxx 与岛屿 NPC 交互
[IslandNpc-TypeWriter] [交互监听器] 没有找到匹配的 TypeWriter 事件
[DEBUG] [交互] 玩家没有活跃的任务
[DEBUG] [交互] 打开 FancyDialogs 菜单
```

## 常见问题排查

### 问题 1: 日志中没有 "[IslandNpc-TypeWriter]" 相关信息

**原因：** TypeWriter Extension 没有正确安装或加载

**解决：**
1. 检查文件是否存在：`plugins/TypeWriter/extensions/IslandNpcExtension-*.jar`
2. 检查 TypeWriter 插件是否正常加载
3. 重启服务器

### 问题 2: 显示 "没有找到匹配的 TypeWriter 事件"

**原因：** 事件配置不正确

**解决：**
1. 检查事件是否有配置触发器（Triggers）
2. 检查 NPC 引用是否正确
3. 检查 `ownIslandOnly` 设置

### 问题 3: 显示 "Citizens 插件未找到"

**原因：** Citizens 插件未安装

**解决：**
1. 确保 IslandNpc 配置使用 Citizens：`provider: "CITIZENS"`
2. 确保 Citizens 插件已安装并启用

### 问题 4: 事件触发了但对话没有显示

**原因：** 触发器配置或对话条目问题

**解决：**
1. 检查触发器是否正确关联到对话
2. 检查对话条目是否有错误
3. 查看 TypeWriter 的日志输出

## 工作流程图

```
玩家右键点击 NPC
    ↓
Citizens NPCRightClickEvent 触发
    ↓
IslandNpc 主插件监听器检查 (CitizensNpcInteractListener)
    ├─ 验证是否是岛屿 NPC ✓
    ├─ 验证玩家是否在自己的岛屿 ✓
    └─ 调用 NpcInteractionHandler
        ↓
    检查是否有 TypeWriter 任务
        ↓
TypeWriter Extension 监听器 (IslandNpcInteractListener)
    ├─ 查找匹配的 island_npc_interact_event
    ├─ 如果找到 → 触发 TypeWriter 事件系统
    └─ TypeWriter 显示对话/启动任务
        ↓
    NpcInteractionHandler 检测到有活跃任务
    └─ 返回 false（不取消事件，让 TypeWriter 处理）
```

## 调试技巧

### 1. 查看所有已注册的事件

在 TypeWriter 编辑器中，查看 Manifest 页面的所有条目：
- 确认 `island_npc_interact_event` 存在
- 确认事件有配置触发器

### 2. 使用命令测试

```
/typewriter entries list  # 列出所有条目
/typewriter entries info <event_id>  # 查看事件详情
```

### 3. 检查玩家是否满足触发条件

TypeWriter 事件可能有前置条件（Facts、Items 等），确保玩家满足所有条件。

## 快速构建命令

如果你只想构建 Extension：

```powershell
cd "d:\系统数据\Desktop\服务器自定义插件\IslandNpc\参考代码\TypeWriter\extensions\IslandNpcExtension"
..\..\gradlew.bat build
```

## 总结

修复包含两部分：

1. **代码修复（已完成）**
   - ✅ 添加 `IslandNpcInteractListener.kt` 监听器
   - ✅ 更新 `IslandNpcTypeWriterService.kt` 注册监听器

2. **配置检查（需要你做）**
   - ⬜ 重新构建 TypeWriter Extension
   - ⬜ 部署到服务器
   - ⬜ 确保事件配置了触发器
   - ⬜ 启用 debug 模式测试

---

**创建日期：** 2024-11-24  
**修复版本：** IslandNpcExtension v0.9.1+  
**适用于：** TypeWriter 0.6.x + IslandNpc 1.0.2+
