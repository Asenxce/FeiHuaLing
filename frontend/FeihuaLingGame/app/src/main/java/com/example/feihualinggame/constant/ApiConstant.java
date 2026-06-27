package com.example.feihualinggame.constant;

// 后端接口地址常量（与后端对接）
public class ApiConstant {
    // ==================== 服务器配置 ====================
    // 本地开发服务器
    private static final String LOCAL_BASE_URL = "http://192.168.78.20:8081/api/";
    // 远程生产服务器
    private static final String REMOTE_BASE_URL = "http://101.200.121.63:8080/api/";
    
    // 切换后端
    public static final String BASE_URL = LOCAL_BASE_URL;   // 本地开发
    //public static final String BASE_URL = REMOTE_BASE_URL;  // 远程部署

    // WebSocket地址（与HTTP API保持同一服务器）
    private static final String LOCAL_WS_URL = "ws://192.168.78.20:8081/ws/room";
    private static final String REMOTE_WS_URL = "ws://101.200.121.63:8080/ws/room";
    public static final String WS_URL = LOCAL_WS_URL;   // 本地开发
    //public static final String WS_URL = REMOTE_WS_URL;  // 远程部署
    
    // ==================== 用户相关接口 ====================
    // 用户注册
    public static final String REGISTER = "user/register";
    // 用户登录
    public static final String LOGIN = "user/login";
    // 获取用户信息
    public static final String USER_INFO = "user/info";
    // 更新用户信息
    public static final String USER_UPDATE = "user/update";
    // 更新用户头像
    public static final String USER_AVATAR = "user/avatar";
    // 退出登录
    public static final String LOGOUT = "user/logout";
    // 心跳请求
    public static final String HEARTBEAT = "user/heartbeat";
    
    // ==================== OSS相关接口 ====================
    // 获取OSS上传凭证
    public static final String OSS_UPLOAD_TOKEN = "oss/upload-token";
    
    // ==================== 诗词相关接口 ====================
    // 关键字搜索（通用）
    public static final String POETRY_SEARCH = "poetry/search-line";
    // 验证诗句存在性
    public static final String POETRY_VALIDATE = "poetry/validate";
    // 检查关键字是否存在于关键字表
    public static final String KEYWORD_CHECK = "keyword/check";
    // 随机诗句查询
    public static final String POETRY_RANDOM = "poetry/random";
    // 获取用户收藏列表
    public static final String COLLECTION_LIST = "collection/list";
    // 添加收藏
    public static final String COLLECTION_ADD = "collection/add";
    // 取消收藏
    public static final String COLLECTION_REMOVE = "collection/remove";
    
    // ==================== 对战相关接口 ====================
    // 创建人机对战
    public static final String BATTLE_CREATE_AI = "battle/ai/create";
    // 提交答案（AI模式）
    public static final String BATTLE_SUBMIT = "battle/submit";
    // 提交答案（好友对战，需拼接 battleId）
    public static final String BATTLE_SUBMIT_FRIEND = "battle/";
    // AI作答（获取AI诗句）
    public static final String BATTLE_AI_ANSWER = "battle/ai/answer";
    // 获取对手答案（好友对战轮询）
    public static final String BATTLE_OPPONENT_ANSWER = "battle/opponent/answer";
    // 结束对战（需拼接 battleId）
    public static final String BATTLE_END = "battle/";
    
    // ==================== 好友相关接口 ====================
    // 添加好友
    public static final String FRIEND_ADD = "friend/add";
    // 删除好友
    public static final String FRIEND_DELETE = "friend/delete";
    // 获取好友列表
    public static final String FRIEND_LIST = "friend/list";
    // 获取待处理好友申请
    public static final String FRIEND_PENDING_REQUESTS = "friend/pending-requests";
    // 接受好友申请
    public static final String FRIEND_ACCEPT = "friend/accept";
    // 拒绝好友申请
    public static final String FRIEND_REJECT = "friend/reject";
    
    // ==================== 战绩相关接口 ====================
    // 获取个人战绩
    public static final String RECORD_PERSONAL = "record/personal";
    // 获取对战记录
    public static final String RECORD_BATTLE = "record/battle";
    // 获取排行榜
    public static final String RECORD_RANKING = "record/ranking";
    // 删除对战记录
    public static final String RECORD_DELETE = "record/delete";
    
    // ==================== 邮箱相关接口 ====================
    // 获取未读邮件数量
    public static final String MAIL_UNREAD_COUNT = "mail/unread-count";
    // 获取各类型未读邮件数量
    public static final String MAIL_UNREAD_COUNTS = "mail/unread-counts";
    // 获取邮件列表
    public static final String MAIL_LIST = "mail/list";
    // 标记邮件为已读
    public static final String MAIL_READ = "mail/read";
    // 删除邮件
    public static final String MAIL_DELETE = "mail/delete";
    // 清空已读邮件
    public static final String MAIL_CLEAR_READ = "mail/clear-read";
    // 标记全部已读
    public static final String MAIL_MARK_ALL_READ = "mail/mark-all-read";
    // 发送邮件（内部接口）
    public static final String MAIL_SEND = "mail/send";

    // ==================== 多人房间相关接口 ====================
    public static final String ROOM_CREATE = "room/create";
    public static final String ROOM_JOIN = "room/join";
    public static final String ROOM_DETAIL = "room/";
    public static final String ROOM_CODE = "room/code/";
    public static final String ROOM_READY = "room/";
    public static final String ROOM_KICK = "room/";
    public static final String ROOM_LEAVE = "room/";
    public static final String ROOM_START = "room/";
    public static final String ROOM_CONFIG = "room/";
    public static final String ROOM_SUBMIT = "room/";
    public static final String ROOM_TIMEOUT = "room/";
    public static final String ROOM_SURRENDER = "room/";
}
