# IslandNpc Plugin

一个为SuperiorSkyblock2设计的岛屿NPC插件，支持Citizens2和FancyDialogs集成。

## 作者
**magicbili**

## 功能特性

- ✅ 自动为每个岛屿创建NPC
- ✅ 支持玩家隐藏/显示岛屿NPC
- ✅ 支持玩家移动岛屿NPC位置
- ✅ NPC上方显示全息文字（使用Citizens2的HologramTrait）
- ✅ NPC默认生成在岛屿出生点+可配置偏移量
- ✅ 右键NPC打开FancyDialogs菜单
- ✅ 完整的权限系统
- ✅ 支持配置重载

## 依赖插件

### 必需依赖
- **SuperiorSkyblock2** - 岛屿管理插件
- **Citizens** - NPC管理插件

### 可选依赖
- **FancyDialogs** - 对话框菜单系统

## 安装说明

1. 确保已安装所有必需的依赖插件
2. 将编译好的 `IslandNpc-1.0.0.jar` 放入服务器的 `plugins` 文件夹
3. 重启服务器或使用 `/reload confirm`
4. 插件会自动生成配置文件

## 配置文件

### config.yml

```yaml
npc:
  entity-type: "VILLAGER" # NPC实体类型（VILLAGER、PLAYER、ZOMBIE等）
  skin: ""                 # NPC皮肤（仅PLAYER类型有效，玩家名或留空）
  dialog-id: "island_menu" # FancyDialogs对话框ID
  spawn-offset:            # 相对于岛屿中心的偏移量
    x: 0.0
    y: 0.0
    z: 5.0
  hologram:                # 全息显示设置
    enabled: true          # 启用全息显示
    lines:                 # 全息文字行（支持颜色代码）
      - "&e&l⭐ Island NPC ⭐"
      - "&7Right click to interact"
      - "&6Welcome to your island!"
    line-height: -1        # 行高（-1为自动）
    view-range: 30         # 可视距离（方块）

permissions:
  default: true  # 是否默认给予所有玩家权限
```

## 指令

| 指令 | 描述 | 权限 |
|------|------|------|
| `/islandnpc hide` | 隐藏你的岛屿NPC | `islandnpc.hide` |
| `/islandnpc show` | 显示你的岛屿NPC | `islandnpc.show` |
| `/islandnpc move` | 将NPC移动到你的位置 | `islandnpc.move` |
| `/islandnpc fixall` | 修复所有在线玩家缺失的NPC（管理员） | `islandnpc.admin` |
| `/islandnpc create` | 创建岛屿NPC（管理员） | `islandnpc.admin` |
| `/islandnpc delete` | 删除岛屿NPC（管理员） | `islandnpc.admin` |
| `/islandnpc reload` | 重载配置并更新所有NPC（管理员） | `islandnpc.admin` |
| `/islandnpc help` | 显示帮助信息 | 无 |

## 权限节点

- `islandnpc.hide` - 允许隐藏NPC（默认：true）
- `islandnpc.show` - 允许显示NPC（默认：true）
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
4. 使用 `/islandnpc hide` 隐藏NPC
5. 使用 `/islandnpc reload` 重载配置并更新所有NPC的全息显示和菜单

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

### 1.0.0
- 初始版本发布
- 实现所有核心功能
