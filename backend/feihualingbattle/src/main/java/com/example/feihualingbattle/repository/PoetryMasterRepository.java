package com.example.feihualingbattle.repository;

import com.example.feihualingbattle.entity.PoetryMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 诗词主表数据访问接口
 * 优化：使用原生SQL提升查询性能 + 查询提示优化
 */
@Repository
public interface PoetryMasterRepository extends JpaRepository<PoetryMaster, Long> {

    /**
     * 根据关键字搜索诗词（原生SQL，性能更优）
     */
    @Query(value = "SELECT p.* FROM t_poetry_master p WHERE " +
                   "p.title LIKE CONCAT('%', :keyword, '%') OR " +
                   "p.author LIKE CONCAT('%', :keyword, '%') OR " +
                   "p.dynasty LIKE CONCAT('%', :keyword, '%') OR " +
                   "p.full_content_simplified LIKE CONCAT('%', :keyword, '%') " +
                   "ORDER BY " +
                   "CASE WHEN p.title LIKE CONCAT('%', :keyword, '%') THEN 0 " +
                   "     WHEN p.author LIKE CONCAT('%', :keyword, '%') THEN 1 " +
                   "     ELSE 2 END, " +
                   "p.usage_count DESC",
           nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<PoetryMaster> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 根据关键字搜索诗词，带分页和排序（原生SQL优化）
     */
    @Query(value = "SELECT p.* FROM t_poetry_master p WHERE " +
                   "p.title LIKE CONCAT('%', :keyword, '%') OR " +
                   "p.author LIKE CONCAT('%', :keyword, '%') OR " +
                   "p.dynasty LIKE CONCAT('%', :keyword, '%') OR " +
                   "p.full_content_simplified LIKE CONCAT('%', :keyword, '%')",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM t_poetry_master p WHERE " +
                        "p.title LIKE CONCAT('%', :keyword, '%') OR " +
                        "p.author LIKE CONCAT('%', :keyword, '%') OR " +
                        "p.dynasty LIKE CONCAT('%', :keyword, '%') OR " +
                        "p.full_content_simplified LIKE CONCAT('%', :keyword, '%')")
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    org.springframework.data.domain.Page<PoetryMaster> searchByKeywordWithPage(@Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);

    /**
     * 根据两个关键字搜索诗词（原生SQL优化）
     */
    @Query(value = "SELECT p.* FROM t_poetry_master p WHERE " +
                   "(p.title LIKE CONCAT('%', :keyword1, '%') OR p.author LIKE CONCAT('%', :keyword1, '%') OR p.dynasty LIKE CONCAT('%', :keyword1, '%') OR p.full_content_simplified LIKE CONCAT('%', :keyword1, '%')) " +
                   "AND " +
                   "(p.title LIKE CONCAT('%', :keyword2, '%') OR p.author LIKE CONCAT('%', :keyword2, '%') OR p.dynasty LIKE CONCAT('%', :keyword2, '%') OR p.full_content_simplified LIKE CONCAT('%', :keyword2, '%')) " +
                   "ORDER BY p.usage_count DESC",
           nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<PoetryMaster> searchByTwoKeywords(@Param("keyword1") String keyword1, @Param("keyword2") String keyword2);

    /**
     * 根据两个关键字搜索诗词，带分页和排序（原生SQL优化）
     */
    @Query(value = "SELECT p.* FROM t_poetry_master p WHERE " +
                   "(p.title LIKE CONCAT('%', :keyword1, '%') OR p.author LIKE CONCAT('%', :keyword1, '%') OR p.dynasty LIKE CONCAT('%', :keyword1, '%') OR p.full_content_simplified LIKE CONCAT('%', :keyword1, '%')) " +
                   "AND " +
                   "(p.title LIKE CONCAT('%', :keyword2, '%') OR p.author LIKE CONCAT('%', :keyword2, '%') OR p.dynasty LIKE CONCAT('%', :keyword2, '%') OR p.full_content_simplified LIKE CONCAT('%', :keyword2, '%'))",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM t_poetry_master p WHERE " +
                        "(p.title LIKE CONCAT('%', :keyword1, '%') OR p.author LIKE CONCAT('%', :keyword1, '%') OR p.dynasty LIKE CONCAT('%', :keyword1, '%') OR p.full_content_simplified LIKE CONCAT('%', :keyword1, '%')) " +
                        "AND " +
                        "(p.title LIKE CONCAT('%', :keyword2, '%') OR p.author LIKE CONCAT('%', :keyword2, '%') OR p.dynasty LIKE CONCAT('%', :keyword2, '%') OR p.full_content_simplified LIKE CONCAT('%', :keyword2, '%'))")
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    org.springframework.data.domain.Page<PoetryMaster> searchByTwoKeywordsWithPage(@Param("keyword1") String keyword1, @Param("keyword2") String keyword2, org.springframework.data.domain.Pageable pageable);

    /**
     * 根据作者查找（使用原生SQL）
     */
    @Query(value = "SELECT p.* FROM t_poetry_master p WHERE p.author = :author ORDER BY p.usage_count DESC", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<PoetryMaster> findByAuthor(@Param("author") String author);

    /**
     * 根据朝代查找（使用原生SQL）
     */
    @Query(value = "SELECT p.* FROM t_poetry_master p WHERE p.dynasty = :dynasty ORDER BY p.usage_count DESC", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<PoetryMaster> findByDynasty(@Param("dynasty") String dynasty);

    /**
     * 根据诗词类型查找（使用原生SQL）
     */
    @Query(value = "SELECT p.* FROM t_poetry_master p WHERE p.poetry_type = :poetryType ORDER BY p.usage_count DESC", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<PoetryMaster> findByPoetryType(@Param("poetryType") String poetryType);
    
    /**
     * 根据作者模糊查询（使用原生SQL）
     */
    @Query(value = "SELECT p.* FROM t_poetry_master p WHERE p.author LIKE CONCAT('%', :author, '%') ORDER BY p.usage_count DESC", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<PoetryMaster> findByAuthorContaining(@Param("author") String author);
    
    /**
     * 根据标题模糊查询（使用原生SQL）
     */
    @Query(value = "SELECT p.* FROM t_poetry_master p WHERE p.title LIKE CONCAT('%', :title, '%') ORDER BY p.usage_count DESC", nativeQuery = true)
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    List<PoetryMaster> findByTitleContaining(@Param("title") String title);

    @Query(value = "SELECT p.* FROM t_poetry_master p WHERE p.dynasty = :dynasty AND (" +
                   "p.title LIKE CONCAT('%', :keyword, '%') OR " +
                   "p.author LIKE CONCAT('%', :keyword, '%') OR " +
                   "p.full_content_simplified LIKE CONCAT('%', :keyword, '%'))",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM t_poetry_master p WHERE p.dynasty = :dynasty AND (" +
                        "p.title LIKE CONCAT('%', :keyword, '%') OR " +
                        "p.author LIKE CONCAT('%', :keyword, '%') OR " +
                        "p.full_content_simplified LIKE CONCAT('%', :keyword, '%'))")
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    org.springframework.data.domain.Page<PoetryMaster> searchByKeywordAndDynasty(@Param("keyword") String keyword, @Param("dynasty") String dynasty, org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT p.* FROM t_poetry_master p WHERE p.poetry_type = :poetryType AND (" +
                   "p.title LIKE CONCAT('%', :keyword, '%') OR " +
                   "p.author LIKE CONCAT('%', :keyword, '%') OR " +
                   "p.full_content_simplified LIKE CONCAT('%', :keyword, '%'))",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM t_poetry_master p WHERE p.poetry_type = :poetryType AND (" +
                        "p.title LIKE CONCAT('%', :keyword, '%') OR " +
                        "p.author LIKE CONCAT('%', :keyword, '%') OR " +
                        "p.full_content_simplified LIKE CONCAT('%', :keyword, '%'))")
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    org.springframework.data.domain.Page<PoetryMaster> searchByKeywordAndType(@Param("keyword") String keyword, @Param("poetryType") String poetryType, org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT p.* FROM t_poetry_master p WHERE p.author LIKE CONCAT('%', :author, '%') ORDER BY p.usage_count DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM t_poetry_master p WHERE p.author LIKE CONCAT('%', :author, '%')")
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    org.springframework.data.domain.Page<PoetryMaster> findByAuthorContainingWithPage(@Param("author") String author, org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT p.* FROM t_poetry_master p WHERE p.title LIKE CONCAT('%', :title, '%') ORDER BY p.usage_count DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM t_poetry_master p WHERE p.title LIKE CONCAT('%', :title, '%')")
    @QueryHints({@jakarta.persistence.QueryHint(name = "org.hibernate.readOnly", value = "true")})
    org.springframework.data.domain.Page<PoetryMaster> findByTitleContainingWithPage(@Param("title") String title, org.springframework.data.domain.Pageable pageable);
}
