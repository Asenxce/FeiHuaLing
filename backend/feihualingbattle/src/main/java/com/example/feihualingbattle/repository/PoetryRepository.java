package com.example.feihualingbattle.repository;

import com.example.feihualingbattle.entity.Poetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 诗词数据访问接口（单句诗词）
 * 优化：使用原生SQL + @QueryHints提升查询性能
 */
@Repository
public interface PoetryRepository extends JpaRepository<Poetry, Long> {

    /**
     * 根据关键字在诗句内容中查找（原生SQL优化）
     */
    @Query(value = "SELECT p.* FROM t_poetry p WHERE p.content LIKE CONCAT('%', :keyword, '%')", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> findByKeywordInContent(@Param("keyword") String keyword);

    /**
     * 根据关键字在诗句内容中查找，限制返回数量（推荐使用）
     */
    @Query(value = "SELECT p.* FROM t_poetry p WHERE p.content LIKE CONCAT('%', :keyword, '%') LIMIT :limit", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> findByKeywordInContentLimit(@Param("keyword") String keyword, @Param("limit") int limit);

    /**
     * 根据关键字在诗句内容中查找，限制返回数量（原生SQL优化）
     */
    @Query(value = "SELECT p.* FROM t_poetry p WHERE p.content LIKE CONCAT('%', :keyword, '%') ORDER BY p.usage_count DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM t_poetry p WHERE p.content LIKE CONCAT('%', :keyword, '%')")
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    org.springframework.data.domain.Page<Poetry> findByKeywordInContentWithLimit(@Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);

    /**
     * 根据两个关键字在诗句内容中查找（原生SQL优化）
     */
    @Query(value = "SELECT p.* FROM t_poetry p WHERE p.content LIKE CONCAT('%', :keyword1, '%') AND p.content LIKE CONCAT('%', :keyword2, '%') LIMIT :limit", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> findByTwoKeywordsInContent(@Param("keyword1") String keyword1, @Param("keyword2") String keyword2, @Param("limit") int limit);

    /**
     * 根据两个关键字在诗句内容中查找，限制返回数量（原生SQL优化）
     */
    @Query(value = "SELECT p.* FROM t_poetry p WHERE p.content LIKE CONCAT('%', :keyword1, '%') AND p.content LIKE CONCAT('%', :keyword2, '%') ORDER BY p.usage_count DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM t_poetry p WHERE p.content LIKE CONCAT('%', :keyword1, '%') AND p.content LIKE CONCAT('%', :keyword2, '%')")
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    org.springframework.data.domain.Page<Poetry> findByTwoKeywordsInContentWithLimit(@Param("keyword1") String keyword1, @Param("keyword2") String keyword2, org.springframework.data.domain.Pageable pageable);

    /**
     * 查找以指定字符开头的诗句（原生SQL优化）
     */
    @Query(value = "SELECT p.* FROM t_poetry p WHERE LEFT(p.content, 1) = :startChar LIMIT :limit", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> findByContentStartingWith(@Param("startChar") String startChar, @Param("limit") int limit);

    /**
     * 查找不包含指定关键字的诗句（原生SQL优化）
     */
    @Query(value = "SELECT p.* FROM t_poetry p WHERE p.content NOT LIKE CONCAT('%', :keyword, '%') LIMIT :limit", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> findByContentNotContaining(@Param("keyword") String keyword, @Param("limit") int limit);

    /**
     * 根据关键字搜索诗词（原生SQL优化，匹配内容、作者、标题）
     */
    @Query(value = "SELECT p.* FROM t_poetry p WHERE " +
                   "p.content LIKE CONCAT('%', :keyword, '%') OR " +
                   "p.author LIKE CONCAT('%', :keyword, '%') OR " +
                   "p.title LIKE CONCAT('%', :keyword, '%')",
           nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 根据作者查找（原生SQL优化）
     */
    @Query(value = "SELECT p.* FROM t_poetry p WHERE p.author = :author ORDER BY p.usage_count DESC", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> findByAuthor(@Param("author") String author);

    /**
     * 根据朝代查找（原生SQL优化）
     */
    @Query(value = "SELECT p.* FROM t_poetry p WHERE p.dynasty = :dynasty ORDER BY p.usage_count DESC", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> findByDynasty(@Param("dynasty") String dynasty);

    /**
     * 根据诗词类型查找（原生SQL优化）
     */
    @Query(value = "SELECT p.* FROM t_poetry p WHERE p.poetry_type = :poetryType ORDER BY p.usage_count DESC", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> findByPoetryType(@Param("poetryType") String poetryType);

    /**
     * 根据作者模糊查询（原生SQL优化）
     */
    @Query(value = "SELECT p.* FROM t_poetry p WHERE p.author LIKE CONCAT('%', :author, '%') ORDER BY p.usage_count DESC", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> findByAuthorContaining(@Param("author") String author);

    /**
     * 根据标题模糊查询（原生SQL优化）
     */
    @Query(value = "SELECT p.* FROM t_poetry p WHERE p.title LIKE CONCAT('%', :title, '%') ORDER BY p.usage_count DESC", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> findByTitleContaining(@Param("title") String title);

    // ==================== FULLTEXT 全文索引查询（ngram 分词） ====================

    @Query(value = "SELECT p.* FROM t_poetry p WHERE p.content = :content LIMIT 1", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> findByExactContent(@Param("content") String content);

    @Query(value = "SELECT p.* FROM t_poetry p WHERE MATCH(content) AGAINST(:keyword IN BOOLEAN MODE) LIMIT :limit", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> fulltextSearchByContent(@Param("keyword") String keyword, @Param("limit") int limit);

    @Query(value = "SELECT p.* FROM t_poetry p WHERE MATCH(content) AGAINST(:keyword IN BOOLEAN MODE) ORDER BY p.usage_count DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM t_poetry p WHERE MATCH(content) AGAINST(:keyword IN BOOLEAN MODE)")
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    Page<Poetry> fulltextSearchByContentWithPage(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT p.* FROM t_poetry p WHERE MATCH(content) AGAINST(:keyword1 IN BOOLEAN MODE) AND MATCH(content) AGAINST(:keyword2 IN BOOLEAN MODE) LIMIT :limit", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> fulltextSearchByTwoKeywords(@Param("keyword1") String keyword1, @Param("keyword2") String keyword2, @Param("limit") int limit);

    @Query(value = "SELECT p.* FROM t_poetry p WHERE MATCH(content, author, title) AGAINST(:keyword IN BOOLEAN MODE) ORDER BY p.usage_count DESC", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<Poetry> fulltextSearchAll(@Param("keyword") String keyword);
}