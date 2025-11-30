# IslandNpc Plugin

一个为SuperiorSkyblock2设计的岛屿NPC插件，支持FancyNpcs、FancyHolograms和FancyDialogs集成。

## 作者
**magicbili**

## 功能特性

- ✅ 自动为每个岛屿创建NPC
- ✅ 支持玩家隐藏/显示岛屿NPC
- ✅ 支持玩家移动岛屿NPC位置
- ✅ NPC上方显示全息文字（使用FancyHolograms，性能优异）
- ✅ 支持自定义全息图背景颜色（ARGB格式）
- ✅ NPC默认生成在岛屿出生点+可配置偏移量
- ✅ 右键NPC打开FancyDialogs菜单或触发TypeWriter对话
- ✅ 完整的权限系统
- ✅ 支持配置重载
- ✅ SlimeWorld动态世界支持

## 依赖插件

### 必需依赖
- **SuperiorSkyblock2** - 岛屿管理插件
- **FancyNpcs** - 现代化NPC管理插件（推荐）或 **Citizens** - 传统NPC管理插件

### 可选依赖
- **FancyHolograms** - 高性能全息图插件（强烈推荐，替代卡顿的TextDisplay）
- **FancyDialogs** - 对话框菜单系统
- **TypeWriter** - 任务对话系统

## 安装说明

1. 确保已安装所有必需的依赖插件
   - SuperiorSkyblock2
   - FancyNpcs（推荐）或 Citizens
   - **FancyHolograms**（强烈推荐，用于全息图显示）
2. 将编译好的 `IslandNpc-1.0.3.jar` 放入服务器的 `plugins` 文件夹
3. 重启服务器或使用 `/reload confirm`
4. 插件会自动生成配置文件

### 重要提示
- **v1.0.3+ 版本使用 FancyHolograms 替代了 TextDisplay 实体**
- FancyHolograms 性能更优，不会造成卡顿
- 支持自定义背景颜色（ARGB格式）
- 如果未安装 FancyHolograms，全息图功能将不可用

## 配置文件

### config.yml

```yaml
npc:
  provider: "FANCYNPCS"    # NPC提供者：CITIZENS 或 FANCYNPCS
  entity-type: "VILLAGER"  # NPC实体类型（VILLAGER、PLAYER、ZOMBIE等）
  skin: ""                 # NPC皮肤（仅PLAYER类型有效，玩家名或留空）
  dialog-id: "island_menu" # FancyDialogs对话框ID
  spawn-offset:            # 相对于岛屿中心的偏移量
    x: 0.0
    y: 0.0
    z: 5.0
  rotation:                # NPC朝向
    yaw: 180.0             # 水平旋转（0-360）
    pitch: 0.0             # 垂直旋转（-90到90）
  
  # 全息图设置（使用 FancyHolograms 插件）
  hologram:
    enabled: true          # 启用全息显示
    lines:                 # 全息文字行（支持颜色代码和占位符）
      - "&e&l⭐ Island NPC ⭐"
      - "&7Right click to interact"
      - "&6Welcome to your island!"
    position:
      y-offset: 2.8        # NPC头顶上方偏移量（格数）
      line-spacing: 0.3    # 行间距（已废弃，FancyHolograms自动管理）
    background:
      enabled: false       # 启用文本背景
      # 背景颜色（ARGB格式：透明度、红、绿、蓝）
      # 示例：
      #   0x00000000 = 无背景（完全透明）
      #   0x40000000 = 半透明黑色
      #   0x80000000 = 半透明黑色
      #   0xC0000000 = 大部分不透明黑色
      #   0xFF000000 = 完全不透明黑色
      color: 0x00000000
    view-range: 30         # 可视距离（方块）

permissions:
  default: true  # 是否默认给予所有玩家权限
```

## 指令

| 指令 | 描述 | 权限 |
|------|------|------|
| `/islandnpc hide` | 隐藏你的岛屿NPC | `islandnpc.hide` |
| `/islandnpc show` | 显示你的岛屿NPC | `islandnpc.show` |
| `/islandnpc toggle` | 切换你的岛屿NPC可见性 | `islandnpc.toggle` |
| `/islandnpc move` | 将NPC移动到你的位置 | `islandnpc.move` |
| `/islandnpc fixall` | 修复所有在线玩家缺失的NPC（管理员） | `islandnpc.admin` |
| `/islandnpc create` | 创建岛屿NPC（管理员） | `islandnpc.admin` |
| `/islandnpc delete` | 删除岛屿NPC（管理员） | `islandnpc.admin` |
| `/islandnpc reload` | 重载配置并更新所有NPC（管理员） | `islandnpc.admin` |
| `/islandnpc help` | 显示帮助信息 | 无 |

## 权限节点

- `islandnpc.hide` - 允许隐藏NPC（默认：true）
- `islandnpc.show` - 允许显示NPC（默认：true）
- `islandnpc.toggle` - 允许切换NPC可见性（默认：true）
- `islandnpc.move` - 允许移动NPC（默认：true）
- `islandnpc.admin` - 管理员权限（默认：op）

## 构建项目

使用Maven构建项目：

```bash
mvn clean package
```

编译后的jar文件将位于 `target` 目录中。

## 使用示例

### 基本使用
1. 玩家创建岛屿后，NPC会自动在岛屿中心偏移位置生成
2. 右键点击NPC打开配置的FancyDialogs菜单
3. 使用 `/islandnpc move` 将NPC移动到你当前的位置
4. 使用 `/islandnpc toggle` 快速切换NPC的可见性，或使用 `/islandnpc hide` 隐藏NPC
5. 使用 `/islandnpc reload` 重载配置并更新所有NPC的全息显示和菜单
6. 玩家删除岛屿时，NPC和相关数据会自动清理

### NPC模型配置
- 默认模型为 `VILLAGER`（村民）
- 可配置为任何Bukkit支持的实体类型，如：
  - `PLAYER` - 玩家模型（可设置皮肤）
  - `ZOMBIE` - 僵尸
  - `SKELETON` - 骷髅
  - `CREEPER` - 苦力怕
  - `IRON_GOLEM` - 铁傀儡
  - 等等...

### 配置FancyDialogs
在FancyDialogs的配置中创建对话框，然后在本插件的 `config.yml` 中设置 `dialog-id`。

## 技术支持

如有问题或建议，请联系作者 **magicbili**

## 许可证

本插件为私有项目，版权归作者所有。

## 版本历史

### 1.0.3 (当前版本)
- **重大更新：使用 FancyHolograms 替代 TextDisplay 实体**
- 解决 TextDisplay 导致的严重卡顿问题
- 支持自定义全息图背景颜色（ARGB格式）
- 提升全息图性能和稳定性
- FancyHolograms 自动管理多行文本间距
- 添加 FancyHolograms 软依赖检查

### 1.0.2
- 添加 FancyNpcs 支持（可选择使用 Citizens 或 FancyNpcs）
- 优化 NPC 创建和管理流程
- 添加 TypeWriter 集成支持
- 改进配置文件版本管理

### 1.0.1
- 完全重构NPC持久化机制，不再依赖Citizens的saves.yml
- 添加SlimeWorld支持，使用ASP的LoadSlimeWorldEvent自动重新创建NPC
- 异步处理世界加载，避免大量玩家时卡服
- 添加岛屿删除时自动清理NPC数据功能
- 优化日志输出，移除调试信息
- 修复SlimeWorld环境下NPC丢失问题

### 1.0.0
- 初始版本发布
- 实现所有核心功能

## 常见问题

### Q: 为什么要使用 FancyHolograms 而不是 TextDisplay？
A: TextDisplay 实体在大量使用时会导致严重的服务器卡顿。FancyHolograms 使用优化的数据包发送机制，性能远超 TextDisplay，且支持更多自定义选项（如背景颜色）。

### Q: 如果不安装 FancyHolograms 会怎样？
A: 插件仍然可以正常运行，但全息图功能将不可用。NPC 本身的功能（交互、移动等）不受影响。

### Q: 如何自定义全息图背景颜色？
A: 在 `config.yml` 中设置 `npc.hologram.background.enabled: true`，然后修改 `color` 值。颜色格式为 ARGB（透明度-红-绿-蓝），例如 `0x80000000` 表示半透明黑色。

### Q: 支持哪些 NPC 提供者？
A: 支持 Citizens 和 FancyNpcs。推荐使用 FancyNpcs，它更现代化且性能更好。在 `config.yml` 中设置 `npc.provider` 来选择。
