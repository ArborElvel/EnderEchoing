# 末影回响对外扩展 API

面向依赖 `enderechoing` 做联动的模组开发者。对外契约只有 `com.unddefined.enderechoing.api`
及其子包；`server`、`blocks`、`items`、`network`、`client`、`compat`、`mixin` 等包都标了
`@ApiStatus.Internal`，属于实现细节，随时可能改动。

## 术语

| 中文 | 代码 | 说明 |
| --- | --- | --- |
| 末影回响锚点 | `anchors` | 可被绑定为传送目的地的装置（谐振器、折跃平台等） |
| 路径点 | `waypoints` | 玩家路径点列表里的一条坐标记录 |
| 末影回响水晶 | Ender Echo Crystal | 与「回响晶簇」（Echo Druse）是两个不同的东西 |
| 仪器 | device | 调谐器与折跃平台，它们各自保存一个目的地 |

## 引入依赖

```toml
[[dependencies.yourmod]]
modId = "enderechoing"
type = "required"        # 也可以先用 optional，运行时用 ModList 判断
versionRange = "[1.0.0,)"
ordering = "AFTER"
side = "BOTH"
```

编译期依赖：本仓库目前只把 artifact 发布到本地 `repo/` 目录，没有公共 Maven 坐标；
外部工程可以用 `compileOnly files(".../enderechoing-<version>.jar")` 或自建 Maven 引入。

## API 结构

| 包 | 入口 | 用途 |
| --- | --- | --- |
| `api` | `EnderEchoingApi` | `MODID`、`API_VERSION`、几个转发入口 |
| `api.anchor` | `EnderEchoAnchorProvider` / `EnderEchoAnchorRegistry` | 注册「某位置算不算锚点」的判定器 |
| `api.anchor` | `EnderEchoAnchors` | 玩家名下锚点的读取与增删 |
| `api.waypoint` | `EnderEchoWaypoint` / `EnderEchoWaypoints` | 路径点值类型与读写 |
| `api.crystal` | `EnderEchoCrystals`（`Entry`） | 某维度水晶的读取与增删改名 |
| `api.pearl` | `EnderEchoPearls` | 玩家回响珍珠数量的读写 |
| `api.team` | `EnderEchoTeams`（`EnderEchoTeam` 快照） | 队伍查询、邀请、离队、队长票选、分享 |
| `api.teleport` | `EnderEchoTeleports` | 传送前后事件的派发入口 |
| `api.event` | 8 族事件 | 见下表 |
| `client.api` | `EnderEchoClientData` | **客户端专有**：读取已同步的锚点、路径点名称与当前预览目的地 |
| `client.api.event` | `EnderEchoClientEvent` | **客户端专有**：`AnchorSync` / `WaypointNamesSync` / `ResonatorNamesSync` / `TargetChanged` |
| `client.api.render` | `EnderEchoClientRender` | **客户端专有**：`drawBoneOutline`（Geo 模型描边）、`drawEchoResponse`（回响响应波纹） |

模组内部也走同一套入口，所以这些方法的可用性有自家代码兜底。

`client.api` 与服务端 api 分开的原因：仓库约定客户端独有代码一律放在 `client/` 包下，
这些类在 Dedicated Server 上不会被加载，依赖方必须放在 `Dist.CLIENT` 分支里引用。

## 事件总览

所有事件都派发在 `NeoForge.EVENT_BUS` 上，用 `@SubscribeEvent` 监听；
`@EventBusSubscriber(modid = "yourmod")` 默认就是游戏总线。
**事件总线不允许监听抽象基类**，请监听具体子类（例如 `EnderEchoTeleportEvent.Pre`）。

| 族 | 事件 | 可取消 | 说明 |
| --- | --- | --- | --- |
| 传送 | `EnderEchoTeleportEvent.Pre` | 是 | 传送执行前；取消后不传送，也不扣珍珠、不消耗重生锚充能、不进冷却 |
| 传送 | `EnderEchoTeleportEvent.Post` | 否 | 传送成功后；一次动作只派发一次，只针对发起者 |
| 锚点 | `EnderEchoAnchorEvent.Pre` | 是 | 锚点建立前；取消后不放方块、不消耗物品、不登记 |
| 锚点 | `EnderEchoAnchorEvent.Added` / `.Removed` | 否 | 锚点登记进 / 移出某玩家列表后 |
| 路径点 | `EnderEchoWaypointEvent.Pre` | 是 | 路径点写入前；被拦下时不扣经验或珍珠，界面同步里的新增点会被丢弃 |
| 路径点 | `EnderEchoWaypointEvent.Added` / `.Removed` | 否 | 新增 / 移除 |
| 路径点 | `EnderEchoWaypointEvent.Modified` | 否 | 同坐标内容变化；`getPrevious()` 给出旧值 |
| 水晶 | `EnderEchoCrystalEvent.Pre` | 是 | 放置并登记前；取消后不放方块、不生成实体、不消耗物品 |
| 水晶 | `EnderEchoCrystalEvent.Added` / `.Removed` / `.Renamed` | 否 | 登记 / 移除 / 改名 |
| 结构 | `EnderEchoStructureEvent.Visited` | 否 | 玩家首次进入可被回响之眼定位的结构 |
| 仪器 | `EnderEchoDeviceEvent.Pre` | 是 | 玩家通过调谐界面切换目的地前；内部复位不经过本事件 |
| 仪器 | `EnderEchoDeviceEvent.PositionChanged` | 否 | 目的地变更后 |
| 珍珠 | `EnderEchoPearlEvent.Changed` | 否 | 数量变化后；带 `Cause`（TELEPORT / WAYPOINT / CONVERT / SHARE / DEATH / SYNC） |
| 队伍 | `EnderEchoTeamEvent.PreJoin` / `.PreLeave` / `.PreCaptainChanged` | 是 | 入队、离队、队长变更前 |
| 队伍 | `Joined` / `Left` / `CaptainChanged` / `VoteCast` / `WaypointShared` | 否 | 对应动作完成后 |

## 示例

```java
@EventBusSubscriber(modid = "yourmod")
public final class YourIntegration {
    // 保护区内禁止传送
    @SubscribeEvent
    public static void onTeleport(EnderEchoTeleportEvent.Pre event) {
        if (isProtected(event.getTarget())) event.setCanceled(true);
    }

    // 保护区内禁止放置锚点
    @SubscribeEvent
    public static void onAnchor(EnderEchoAnchorEvent.Pre event) {
        if (isProtected(GlobalPos.of(event.getDimension(), event.getBlockPos()))) event.setCanceled(true);
    }

    // 监听珍珠数量变化做 HUD
    @SubscribeEvent
    public static void onPearl(EnderEchoPearlEvent.Changed event) {
        updateHud(event.getPlayer(), event.getAmount(), event.getDelta(), event.getCause());
    }
}
```

注册自定义锚点类型（在 `@Mod` 构造函数或 `FMLCommonSetupEvent` 中；注册表会在加载完成后冻结）：

```java
EnderEchoingApi.registerAnchor((level, pos) -> level.getBlockState(pos).is(YourBlocks.MY_ANCHOR));
```

## 注意事项

1. **服务端权威**：`add` / `remove` / `replace` / `set` 这类写入只在服务端生效并派发事件；
   客户端调用不会派发，事件本身也只在服务端派发。
2. **不会自动同步**：玩家附件与存档数据（锚点、路径点、珍珠数量、队伍）都不会自动同步到客户端，
   需要显示就自己发包。
3. **事件回调要快**：`Pre` 位于交互或传送的主路径上，避免在回调里做重活或阻塞。
4. **注意副作用时机**：`Added` 类事件在数据写入后派发；`Pre` 在副作用之前派发，
   此时方块还没放、物品还没扣。
5. **客户端类不可用**：`api` 包不依赖任何 `client` 类，可以在 Dedicated Server 上安全加载。
6. **客户端事件**：`client.api.event` 下的事件只在客户端缓存被服务端更新时派发，
   属于「客户端数据变了」的通知，不代表权威状态；监听方需要自己放在 `Dist.CLIENT` 分支里。

## 外观：贴图、模型与颜色

### 资源包可以直接覆盖

以下内容都是固定路径、走资源管理器加载，资源包放同名文件即可替换：

| 类别 | 路径 |
| --- | --- |
| 方块状态 / 模型 | `assets/enderechoing/blockstates/`、`assets/enderechoing/models/` |
| 纹理 | `assets/enderechoing/textures/`（含 `textures/misc/wave.png`、`textures/misc/sonic_boom_<帧>.png`、`textures/misc/core_layer.png`、`textures/gui/`） |
| GeckoLib 模型与动画 | `assets/enderechoing/geo/`、`assets/enderechoing/animations/`（按模型类里写的 asset id 取同名文件） |
| 着色器 | `assets/enderechoing/shaders/`（`post/sculk_veil.json`、`program/sculk_veil.fsh`、`core/warp_core_portal.*`） |

两点坑：回响水晶方块、谐振器、调谐器、折跃平台**共用** `calibrated_sculk_shrieker` 这个 asset id，
换那张贴图会一起改；传送门绑定的是原版 `end_sky.png` / `end_portal.png`，覆盖它们也会影响原版末地传送门。

### 客户端配置可以改的颜色

`config/enderechoing-client.toml`，值写 ARGB 十六进制（例如 `0xFF8CF4E2`）：

| 配置项 | 作用 | 默认值 |
| --- | --- | --- |
| `echo_wave_color` | 回响波纹普通颜色 | `0xFF2CCDB1` |
| `echo_wave_highlight_color` | 回响波纹高亮颜色 | `0xFF8CF4E2` |
| `echo_response_color` | 回响响应波纹颜色 | `0xFF8CF4E2` |
| `waypoint_name_color` | 世界内路径点 / 谐振器名称文字颜色 | `0xFF8CF4E2` |
| `warp_core_outer_outline_color` | 折跃核心外层描边（同时给传送门星云染色） | `0xFF5A2A4D` |
| `warp_core_inner_outline_color` | 折跃核心内层描边 | `0xFF750AED` |

### 仍是代码内固定的部分

- 回响水晶的 13 通道调色板（与染料通道语义绑定）；
- 影匿遮罩的 `MASK_SIZE` / `MASK_SCALE`（属于缓冲区尺寸，改它要重建资源）；
- 界面里的文字与高亮颜色。

这些要改目前只能改代码或 mixin；如果哪天有明确需求，再按上面的方式抽成配置。

## 版本与兼容

- `EnderEchoingApi.API_VERSION` 表示对外 API 的版本，当前为 `1`。
- 只在发生**不兼容改动**（重命名、改签名、改语义、改存档或网络格式）时递增；
  新增事件或新增方法不递增。
- `gradle.properties` 里的 `mod_version` 是模组版本，与 `API_VERSION` 独立。
- 目前还没有历史发布版本，1.x 期间接口仍可能调整；有变更会在提交信息里用
  `Refactor` / `Fix` 注明范围。
