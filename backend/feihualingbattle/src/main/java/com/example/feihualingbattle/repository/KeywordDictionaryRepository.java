package com.example.feihualingbattle.repository;

import com.example.feihualingbattle.entity.KeywordDictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 关键字字典Repository
 * 提供对t_keyword_dictionary表的数据库操作
 */
@Repository
public interface KeywordDictionaryRepository extends JpaRepository<KeywordDictionary, Long> {
    
    /**
     * 根据关键字和类型查找(且必须启用)
     * @param keyword 关键字
     * @param type 类型(color或number)
     * @return 可选的关键字字典记录
     */
    Optional<KeywordDictionary> findByKeywordAndTypeAndIsActive(String keyword, String type, Boolean isActive);
    
    /**
     * 检查关键字是否存在且已启用
     * @param keyword 关键字
     * @param type 类型
     * @param isActive 是否启用
     * @return 是否存在
     */
    boolean existsByKeywordAndTypeAndIsActive(String keyword, String type, Boolean isActive);
    
    /**
     * 查询指定类型的所有已启用关键字(按排序权重)
     * @param type 类型
     * @param isActive 是否启用
     * @return 关键字列表
     */
    @Query("SELECT k FROM KeywordDictionary k WHERE k.type = :type AND k.isActive = :isActive ORDER BY k.sortOrder ASC")
    List<KeywordDictionary> findAllByTypeAndIsActiveOrderBySortOrder(@Param("type") String type, @Param("isActive") Boolean isActive);
    
    /**
     * 查询指定类型的所有关键字(不考虑启用状态)
     * @param type 类型
     * @return 关键字列表
     */
    List<KeywordDictionary> findByTypeOrderBySortOrder(String type);
    
    /**
     * 统计指定类型的关键字数量
     * @param type 类型
     * @return 数量
     */
    long countByType(String type);
    
    /**
     * 统计指定类型且已启用的关键字数量
     * @param type 类型
     * @param isActive 是否启用
     * @return 数量
     */
    long countByTypeAndIsActive(String type, Boolean isActive);
    
    /**
     * 增加使用次数
     * @param id 记录ID
     */
    @Query("UPDATE KeywordDictionary k SET k.usageCount = k.usageCount + 1 WHERE k.id = :id")
    void incrementUsageCount(@Param("id") Long id);
    
    /**
     * 根据分类查询关键字
     * @param category 分类
     * @param type 类型
     * @return 关键字列表
     */
    List<KeywordDictionary> findByCategoryAndTypeOrderBySortOrder(String category, String type);
    
    /**
     * 搜索关键字(模糊匹配描述)
     * @param keyword 关键字
     * @param type 类型
     * @return 关键字列表
     */
    @Query("SELECT k FROM KeywordDictionary k WHERE k.type = :type AND (k.keyword LIKE CONCAT('%', :keyword, '%') OR k.description LIKE CONCAT('%', :keyword, '%')) AND k.isActive = true ORDER BY k.sortOrder ASC")
    List<KeywordDictionary> searchByKeyword(@Param("keyword") String keyword, @Param("type") String type);
}
