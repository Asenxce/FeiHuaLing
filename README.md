# 飞花令对战系统（FeiHuaLing Battle）

基于中国传统诗词文化的一款多人在线飞花令对战游戏，包含 **Android 客户端** 和 **Spring Boot 后端服务**。

支持 8 种飞花令玩法模式，涵盖人机对战、好友对战和多人房间对战三种对战形式。

---

## 功能特性

### 游戏模式（8 种）

| 模式 | 说明 |
|------|------|
| 单关键字飞花令 | 诗句中包含指定关键字 |
| 位置飞花令 | 关键字必须出现在诗句的指定位置（如第 1、3、5 字） |
| 双关键字飞花令 | 诗句中同时包含两个关键字 |
| 首尾接龙飞花令 | 上一句的末字是下一句的首字 |
| 颜色飞花令 | 诗句中包含颜色词 |
| 数字飞花令 | 诗句中包含数字 |
| 反飞花令 | 诗句中不能包含指定关键字 |
| 自定义飞花令 | 玩家自由设定规则组合 |

### 对战形式

- **人机对战**：与 AI 诗词库对战，支持所有游戏模式
- **好友对战**：通过身份码或邀请链接 1v1 对战
- **多人房间**：创建房间，2-8 人同时对战，支持踢人、准备、超时自动淘汰

### 其他功能

- 诗词数据库：收录全唐诗 + 全宋词，支持全文检索
- 诗词收藏：收藏喜欢的诗词
- 好友系统：添加好友、查看在线状态
- 邮件通知：系统通知、好友申请、对战邀请
- 战绩统计：胜负平记录、准确率、积分排行
- 头像上传：阿里云 OSS 存储
- 断线重连：WebSocket 实时通信，15 秒延迟认输窗口
- 顶号检测：同一账号多设备登录自动踢出

---

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.5.12 | Web 框架 |
| Spring Data JPA | - | ORM 持久层 |
| Spring Security | - | JWT 无状态认证 |
| MySQL | 8.x | 主数据库 |
| Redis | 6.x+ | 缓存、WebSocket 会话管理 |
| WebSocket | Jakarta | 实时通信（原生 Endpoint） |
| Lombok | 1.18.44 | 代码简化 |
| OpenCC4J | 1.8.0 | 繁简转换（繁体诗词 → 简体） |
| 阿里云 OSS | 3.17.4 | 头像文件存储 |

### 前端

| 技术 | 说明 |
|------|------|
| Java | Android SDK 36（minSdk 24） |
| OkHttp | HTTP 请求 |
| Gson | JSON 解析 |
| Material Design | UI 组件 |
| RecyclerView | 列表展示 |
| ConstraintLayout | 响应式布局 |

### 数据库

- 共 14 张表，包含用户、诗词、对战、房间、好友、邮件等模块
- 诗词表含 FULLTEXT 全文索引（ngram 分词器），支持中文搜索
- 完整建表 SQL 见 `backend/feihualingbattle/src/main/resources/db/schema.sql`

---

## 项目结构

```
all_project/
├── LICENSE                              # MIT 开源许可证
├── README.md                            # 项目说明文档
├── .gitignore
│
├── backend/
│   └── feihualingbattle/                # Spring Boot 后端
│       ├── src/main/java/com/example/feihualingbattle/
│       │   ├── config/                  # 配置（WebSocket、Redis、OSS）
│       │   ├── controller/              # 接口层（13 个 Controller）
│       │   ├── entity/                  # 实体类（14 个）
│       │   ├── repository/              # JPA Repository
│       │   ├── service/                 # 业务逻辑（19 个 Service）
│       │   │   └── strategy/            # 策略模式（8 种游戏模式）
│       │   ├── security/                # JWT 认证
│       │   ├── dto/                     # 数据传输对象
│       │   └── enums/                   # 枚举
│       ├── src/main/resources/
│       │   ├── application.properties   # 应用配置
│       │   └── db/
│       │       ├── schema.sql           # 建表 SQL
│       │       └── migration/           # 数据库迁移脚本
│       ├── import_poetry.py             # 诗词数据导入脚本
│       ├── .env.example                 # 环境变量模板
│       └── pom.xml
│
├── frontend/
│   └── FeihuaLingGame/                  # Android 客户端
│       └── app/src/main/java/com/example/feihualinggame/
│           ├── activity/                # Activity 页面
│           ├── fragment/                # Fragment（底部导航）
│           ├── adapter/                 # 列表适配器
│           ├── bean/                    # 数据模型
│           ├── validator/               # 前端规则引擎（校验层）
│           ├── constant/                # 常量配置
│           └── utils/                   # 工具类
│
└── backend/feihualingbattle/chinese-poetry-data/  # 诗词 JSON 数据（未上传）
    ├── tang/                            # 全唐诗
    └── song/                            # 全宋词
```

---

## 环境要求

### 后端

- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+
- 阿里云 OSS 账号（用于头像上传，可选）

### 前端

- Android Studio Hedgehog+
- JDK 17（编译用）
- Android SDK 36
- 真机或模拟器（minSdk 24，即 Android 7.0+）

---

## 快速开始

### 1. 数据库初始化

```sql
-- 执行建表脚本
source backend/feihualingbattle/src/main/resources/db/schema.sql
```

### 2. 导入诗词数据（可选）

```bash
# 将 chinese-poetry-data 目录放到 backend/feihualingbattle/ 下
# 目录结构：
#   chinese-poetry-data/
#   ├── tang/    # 全唐诗 JSON
#   └── song/    # 全宋词 JSON

# 安装 Python 依赖
pip install pymysql opencc-python-reimplemented

# 运行导入脚本
cd backend/feihualingbattle
python import_poetry.py
```

> 注意：诗词原始数据约 137MB（338 个 JSON 文件），包含数百万条诗词记录。数据来源为 [chinese-poetry](https://github.com/chinese-poetry/chinese-poetry) 开源项目。

### 3. 启动后端

```bash
cd backend/feihualingbattle

# 方式一：设置环境变量后启动
export DB_URL="jdbc:mysql://localhost:3306/feihualingbattle?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
export DB_USERNAME="root"
export DB_PASSWORD="你的数据库密码"
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export JWT_SECRET="至少256位的随机密钥用于JWT签名"
export OSS_ENDPOINT="oss-cn-beijing.aliyuncs.com"
export OSS_ACCESS_KEY="你的阿里云AccessKey ID"
export OSS_ACCESS_SECRET="你的阿里云AccessKey Secret"
export OSS_BUCKET="你的OSS Bucket名称"
export OSS_DOMAIN="https://你的Bucket.oss-cn-beijing.aliyuncs.com"

mvn spring-boot:run

# 方式二：在 IDEA 的 Run Configuration → Environment Variables 中配置上述变量
```

### 4. 配置并启动前端

1. 用 Android Studio 打开 `frontend/FeihuaLingGame/`
2. 修改后端地址（位于 `app/src/main/java/.../constant/ApiConstant.java`）：
   - `LOCAL_API_URL`：后端 HTTP 接口地址
   - `LOCAL_WS_URL`：后端 WebSocket 地址
3. 连接设备或启动模拟器，点击 Run

---

## API 接口概览

### 用户模块 `/api/user`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/register` | 用户注册 |
| POST | `/login` | 登录（返回 JWT） |
| GET | `/info` | 获取用户信息 |
| POST | `/update` | 更新资料 |
| POST | `/avatar` | 更新头像 |
| POST | `/heartbeat` | 心跳保活 |
| POST | `/logout` | 退出登录 |

### 对战模块 `/api/battle`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ai/create` | 创建人机对战 |
| POST | `/friend/create` | 创建好友对战 |
| POST | `/friend/create-by-code` | 通过身份码创建对战 |
| POST | `/submit` | 提交答案验证 |
| POST | `/ai/answer` | 获取 AI 答案 |
| POST | `/{battleId}/end` | 结束对战并结算 |
| POST | `/{battleId}/surrender` | 认输 |

### 多人房间 `/api/room`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/create` | 创建房间 |
| POST | `/join` | 加入房间 |
| POST | `/{roomId}/ready` | 切换准备状态 |
| POST | `/{roomId}/start` | 开始对战 |
| POST | `/{roomId}/kick` | 踢出玩家 |
| POST | `/{roomId}/submit` | 提交答案 |
| POST | `/{roomId}/timeout` | 超时处理 |
| POST | `/{roomId}/surrender` | 认输 |

### 诗词模块 `/api/poetry`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/search` | 智能搜索 |
| GET | `/search-line` | 诗句搜索（支持朝代/类型/双关键字/作者筛选） |
| POST | `/validate` | 诗句验证 |
| GET | `/random` | 随机诗句 |

### 其他模块

- **收藏** `/api/collection` - 列表、添加、移除
- **好友** `/api/friend` - 添加、接受/拒绝、删除、列表
- **邮件** `/api/mail` - 列表、已读、删除、发送、未读计数
- **战绩** `/api/record` - 查询、统计、详情、批量删除
- **邀请** `/api/battle/invite`、`/api/room/invite` - 创建/接受/拒绝邀请

---

## 数据库表结构

共 14 张表：

| 表名 | 说明 |
|------|------|
| `t_user` | 用户表（含积分、胜负平统计） |
| `t_poetry` | 诗句行表（简体无标点，含 FULLTEXT 全文索引） |
| `t_poetry_master` | 诗词完整主表（繁简体、结构 JSON、朝代） |
| `t_poetry_keyword` | 诗句关键字位置索引 |
| `t_keyword_dictionary` | 关键字字典（颜色/数字类型） |
| `t_battle` | 对战主表（ai/friend 类型） |
| `t_battle_round` | 对战回合记录 |
| `t_battle_invite` | 对战邀请表 |
| `t_room` | 多人房间表 |
| `t_room_player` | 房间玩家表 |
| `t_friend` | 好友关系表 |
| `t_user_record` | 用户战绩表 |
| `t_user_collection` | 用户诗词收藏 |
| `t_mail` | 邮件/通知表 |

完整建表 SQL：[schema.sql](backend/feihualingbattle/src/main/resources/db/schema.sql)

---

## 环境变量说明

所有敏感配置通过环境变量注入，**不要**将真实值硬编码到代码中：

| 变量名 | 必填 | 说明 |
|--------|------|------|
| `DB_URL` | 是 | MySQL 连接地址 |
| `DB_USERNAME` | 是 | 数据库用户名 |
| `DB_PASSWORD` | 是 | 数据库密码 |
| `REDIS_HOST` | 否 | Redis 地址（默认 localhost） |
| `REDIS_PORT` | 否 | Redis 端口（默认 6379） |
| `REDIS_PASSWORD` | 否 | Redis 密码 |
| `JWT_SECRET` | 是 | JWT 签名密钥（至少 256 位） |
| `OSS_ENDPOINT` | 否 | 阿里云 OSS 端点 |
| `OSS_ACCESS_KEY` | 否 | 阿里云 AccessKey ID |
| `OSS_ACCESS_SECRET` | 否 | 阿里云 AccessKey Secret |
| `OSS_BUCKET` | 否 | OSS Bucket 名称 |
| `OSS_DOMAIN` | 否 | OSS 自定义域名 |

> 本地开发可在 `backend/feihualingbattle/` 下创建 `.env` 文件，参考 `.env.example` 模板。

---

## 注意事项

1. **诗词数据**：`chinese-poetry-data/` 目录约 137MB，未上传到仓库。需要从 [chinese-poetry](https://github.com/chinese-poetry/chinese-poetry) 下载后放入对应目录，再运行 `import_poetry.py` 导入。

2. **数据库密码**：代码中所有密码均已替换为占位符，部署时需通过环境变量设置真实值。

3. **前端地址配置**：`ApiConstant.java` 中的 `LOCAL_API_URL` 和 `LOCAL_WS_URL` 需要修改为实际后端地址。

4. **Android 网络安全**：`network_security_config.xml` 配置了明文 HTTP 允许（用于开发环境），生产环境应使用 HTTPS。

5. **JWT 过期**：默认 7 天（604800 秒），可通过 `jwt.expiration` 配置。

6. **WebSocket 重连**：客户端断线后有 15 秒延迟认输窗口，超时后自动认输。90 秒心跳超时自动断开连接。

7. **Redis 依赖**：后端依赖 Redis 用于 WebSocket 会话管理和诗词缓存，启动前请确保 Redis 服务已运行。

---

## 开源许可

本项目采用 [MIT License](LICENSE) 开源许可证。

本项目中的诗词数据来源于 [chinese-poetry](https://github.com/chinese-poetry/chinese-poetry) 开源项目。
