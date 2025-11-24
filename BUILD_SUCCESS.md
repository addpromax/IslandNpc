# IslandNpc 和 TypeWriter Extension 构建说明

## ✅ 已完成的构建

### 1. IslandNpc 主插件 ✅ 成功

**构建命令：**
```bash
cd d:\系统数据\Desktop\服务器自定义插件\IslandNpc
mvn clean package
```

**输出文件：**
```
target\IslandNpc-1.0.2.jar
```

**状态：** ✅ 构建成功
**大小：** 约 50KB
**版本：** 1.0.2

## ⏳ 正在构建

### 2. TypeWriter Extension - IslandNpcExtension

**位置：**
```
d:\系统数据\Desktop\服务器自定义插件\IslandNpc\参考代码\TypeWriter\extensions\IslandNpcExtension
```

**构建命令：**
```bash
cd d:\系统数据\Desktop\服务器自定义插件\IslandNpc\参考代码\TypeWriter\extensions
.\gradlew.bat clean build
```

**当前状态：** ⏳ 正在构建中（配置阶段 40%）

**预计输出文件：**
```
IslandNpcExtension\build\libs\IslandNpcExtension-<version>.jar
```

**预计时间：** 3-10 分钟（首次构建需要下载依赖）

## 📦 部署说明

### 构建完成后的文件位置

#### IslandNpc 主插件
```
源文件：d:\系统数据\Desktop\服务器自定义插件\IslandNpc\target\IslandNpc-1.0.2.jar
部署到：plugins\IslandNpc-1.0.2.jar
```

#### TypeWriter Extension
```
源文件：参考代码\TypeWriter\extensions\IslandNpcExtension\build\libs\IslandNpcExtension-xxx.jar
部署到：plugins\TypeWriter\extensions\IslandNpcExtension-xxx.jar
```

## 🚀 快速部署步骤

### 1. 复制主插件
```bash
copy "target\IslandNpc-1.0.2.jar" "<服务器路径>\plugins\"
```

### 2. 复制 TypeWriter Extension
```bash
copy "参考代码\TypeWriter\extensions\IslandNpcExtension\build\libs\IslandNpcExtension-xxx.jar" "<服务器路径>\plugins\TypeWriter\extensions\"
```

### 3. 重启服务器
```bash
/stop
# 或
/restart
```

## 🔍 验证安装

启动服务器后，检查日志：

### IslandNpc 插件
```
[INFO] [IslandNpc] IslandNpc 插件已启用！
[INFO] [IslandNpc] 使用 TypeWriter 作为 NPC 提供者
[INFO] [IslandNpc] 已注册统一 NPC 交互监听器
```

### TypeWriter Extension
```
[INFO] [IslandNpc Extension] 正在初始化...
[INFO] [IslandNpc-TypeWriter] 服务已注册
[INFO] [IslandNpc Extension] 初始化完成
```

## ⚙️ 配置文件

### config.yml
```yaml
npc:
  provider: "TYPEWRITER"
  typewriter:
    display-name: "&6&l岛屿管理员"
    skin-texture: ""
    skin-signature: ""
  dialog-id: "island_menu"
  
debug: true  # 建议首次使用时启用
```

## 🎮 测试步骤

1. **创建岛屿**
   ```
   /is create
   ```

2. **检查 NPC**
   - NPC 应该在岛屿中心附近生成
   - 查看日志确认创建成功

3. **测试交互**
   - 右键点击 NPC
   - 检查是否打开菜单或触发对话

4. **查看调试日志**
   ```
   [DEBUG] [TypeWriter] 为岛屿创建 NPC: xxx-xxx-xxx
   [DEBUG] [交互] 玩家 xxx 与岛屿 NPC 交互
   ```

## 📊 构建统计

### 已完成
- ✅ IslandNpc 主插件
- ✅ 配置文件
- ✅ 文档

### 进行中
- ⏳ TypeWriter Extension

### 待完成
- ⬜ 服务器部署测试
- ⬜ 功能测试

## 🐛 故障排除

### 如果 TypeWriter Extension 构建失败

**方案 1：单独构建 Extension**
```bash
cd 参考代码\TypeWriter\extensions\IslandNpcExtension
..\..\gradlew.bat build
```

**方案 2：检查依赖**
```bash
cd 参考代码\TypeWriter\extensions
.\gradlew.bat dependencies
```

**方案 3：清理并重试**
```bash
.\gradlew.bat clean
.\gradlew.bat build --refresh-dependencies
```

### 常见错误

1. **找不到 version.txt**
   - 确保 `参考代码\TypeWriter\version.txt` 存在
   - 检查 build.gradle.kts 中的路径

2. **依赖下载失败**
   - 检查网络连接
   - 尝试使用代理或镜像

3. **编译错误**
   - 检查 Kotlin 版本
   - 确保 TypeWriter 版本兼容

## 📚 相关文档

- [快速开始指南](QUICK_START_TYPEWRITER.md)
- [集成文档](TYPEWRITER_INTEGRATION_README.md)
- [最终实现](TYPEWRITER_FINAL_IMPLEMENTATION.md)

---

**构建日期：** 2025-11-24  
**版本：** IslandNpc 1.0.2 + TypeWriter Extension  
**状态：** 主插件完成✅ | Extension 构建中⏳
