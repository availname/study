# 02 · MyBatis 快速上手：从 JdbcTemplate 到企业主流

你项目用的 JdbcTemplate（手写 SQL + 手动映射）——理解了它，MyBatis 就是"把 SQL 和映射搬进配置"。

## 三者对比（一分钟定位）

| | JdbcTemplate | MyBatis | MyBatis-Plus |
|--|-------------|---------|--------------|
| SQL | 手写 + 手动映射 | 手写 + **自动映射** | 内置通用 CRUD + 手写复杂 |
| 学习成本 | 低 | 中 | 低（会 MyBatis 即会） |
| 灵活度 | 最高 | 高 | 高 + 增强API |
| 企业使用 | 少 | **主流** | **主流（国内极广）** |

## MyBatis 两种写法

### 注解版（简单 SQL）

```java
@Mapper
public interface PostMapper {
    @Select("SELECT * FROM t_post WHERE id = #{id}")
    Post findById(Long id);

    @Insert("INSERT INTO t_post(title, content, author_id, status) " +
            "VALUES (#{title}, #{content}, #{authorId}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")   // 回填自增主键
    int insert(Post post);

    @Update("UPDATE t_post SET status = #{next} WHERE id = #{id} AND status = #{expect}")
    int casStatus(@Param("id") Long id, @Param("next") int next, @Param("expect") int expect);
}
```

`#{id}` = 预编译参数（防 SQL 注入，**永远用它**）；`${id}` = 字符串拼接（**SQL 注入入口**，只有动态表名/排序字段等少数场景且必须白名单校验）。

### XML 版（复杂 SQL/动态 SQL 的主场）

```xml
<!-- PostMapper.xml（与接口同包同名，resources 下） -->
<mapper namespace="com.example.community.mapper.PostMapper">

    <!-- resultMap：字段与属性映射（下划线→驼峰可全局开启 mapUnderscoreToCamelCase） -->
    <resultMap id="postMap" type="com.example.community.post.Post">
        <id property="id" column="id"/>
        <result property="title" column="title"/>
        <result property="status" column="status"/>
    </resultMap>

    <!-- 动态 SQL：WHERE 标签自动处理 AND 前缀；if 按条件拼接（多条件查询标配） -->
    <select id="search" resultMap="postMap">
        SELECT * FROM t_post
        <where>
            <if test="title != null and title != ''">
                AND title LIKE CONCAT('%', #{title}, '%')
            </if>
            <if test="authorId != null">
                AND author_id = #{authorId}
            </if>
            <if test="statusList != null">
                AND status IN
                <foreach collection="statusList" item="s" open="(" separator="," close=")">
                    #{s}
                </foreach>
            </if>
        </where>
        ORDER BY id DESC LIMIT #{offset}, #{size}
    </select>

    <!-- 批量插入（foreach 拼多 values——比循环单插快一个量级） -->
    <insert id="insertBatch">
        INSERT INTO t_post(title, author_id, status) VALUES
        <foreach collection="posts" item="p" separator=",">
            (#{p.title}, #{p.authorId}, #{p.status})
        </foreach>
    </insert>
</mapper>
```

## MyBatis-Plus：通用 CRUD 免写 SQL

```java
@Mapper
public interface PostMapper extends BaseMapper<Post> {}     // 继承即拥有单表全套 CRUD

// 用法：
postMapper.selectById(1L);
postMapper.insert(post);
postMapper.selectPage(new Page<>(1, 10),
        new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .like(StrUtil.isNotBlank(title), Post::getTitle, title)   // 条件判空内置
                .orderByDesc(Post::getCreatedAt));
postMapper.update(post, new LambdaUpdateWrapper<Post>().eq(Post::getId, 1L));
```

- 单表 90% 的 SQL 消失（通用 CRUD + Lambda 条件构造器——**类型安全**，字段改名编译期报错）
- **复杂 SQL 仍写 XML**（多表 join/复杂统计）——MP 的定位是"单表免写、复杂手写"
- 分页：`PaginationInnerInterceptor` 插件，一行 `selectPage` 物理分页

## 与 JdbcTemplate 的对照（你已经掌握的迁移过去）

| JdbcTemplate（你项目） | MyBatis |
|------------------------|---------|
| 手写 SQL + RowMapper 手动映射 | XML/注解 SQL + 自动映射 |
| `#{param}` → NamedParameter | `#{param}` 同款预编译 |
| 手写 try-catch 资源管理 | 框架托管 |
| 无动态 SQL | `<if>/<foreach>/<where>` 动态拼接 |

## 生产注意

| 事项 | 说明 |
|------|------|
| 分页插件 | PageHelper 或 MP 的 PaginationInnerInterceptor（记得配 DbType） |
| 大字段延迟加载 | association/collection 的 fetchType |
| 打印 SQL | 开发环境 `mybatis-plus.configuration.log-impl=StdOut` —上线关 |
| 逻辑删除 | `@TableLogic` + 全局配置（DELETE 变 UPDATE deleted=1） |
| 主键策略 | `@TableId(type = IdType.ASSIGN_ID)` 雪花（与 mq 教学全局 ID 呼应） |

## 面试问法

**Q: MyBatis 中 #{} 和 ${} 的区别？**
A: #{} 预编译占位符（PreparedStatement 参数化，防 SQL 注入）；${} 直接字符串拼接（有注入风险）——只用于动态表名/ORDER BY 列名等无法参数化的位置，且必须白名单校验。ORDER BY 动态列若必须拼接，用枚举映射或正则白名单。

**Q: MyBatis 一级/二级缓存？**
A: 一级缓存 SqlSession 级（默认开，同一 session 相同查询走缓存——生产警告：Spring 集成下每次请求新 session，一级缓存基本无效）；二级缓存 namespace 级（跨 session，需手动开，多表关联时易脏读）——**生产实践通常关闭二级缓存，用 Redis 显式缓存替代**（可控、可过期、可分布式）。

---

**测试与 MyBatis 模块完成** ✅。回 [java-core README](../README.md)。
