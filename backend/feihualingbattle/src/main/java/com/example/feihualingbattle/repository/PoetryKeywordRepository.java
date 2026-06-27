package com.example.feihualingbattle.repository;

import com.example.feihualingbattle.entity.PoetryKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PoetryKeywordRepository extends JpaRepository<PoetryKeyword, Long> {

    /**
     * 根据关键字查找所有包含该关键字的记录
     */
    List<PoetryKeyword> findByKeyword(String keyword);

    /**
     * 根据关键字和位置查找
     */
    List<PoetryKeyword> findByKeywordAndPosition(String keyword, Integer position);

    /**
     * 根据诗词ID查找所有关键字
     */
    List<PoetryKeyword> findByPoetryId(Long poetryId);

    /**
     * 统计某个关键字的使用频率
     */
    @Query("SELECT COUNT(pk) FROM PoetryKeyword pk WHERE pk.keyword = :keyword")
    long countByKeyword(@Param("keyword") String keyword);
}
