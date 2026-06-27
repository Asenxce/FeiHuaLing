package com.example.feihualingbattle.service;

import com.example.feihualingbattle.entity.PoetryMaster;
import com.example.feihualingbattle.repository.PoetryMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 诗词主表业务服务类
 * 优化：使用Spring Cache注解简化缓存代码，合并重复逻辑
 */
@Service
public class PoetryMasterService {
    
    @Autowired
    private PoetryMasterRepository poetryMasterRepository;
    
    private static final String CACHE_NAME = "poetryMaster";
    
    /**
     * 根据关键字搜索诗词（同时匹配标题、作者、朝代、完整内容）- 带缓存和排序
     */
    @Cacheable(value = CACHE_NAME, key = "'search:' + #keyword + ':' + #page + ':' + #size")
    public Page<PoetryMaster> searchByKeyword(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return poetryMasterRepository.searchByKeywordWithPage(keyword, pageable);
    }
    
    /**
     * 根据关键字搜索诗词（返回List）
     */
    @Cacheable(value = CACHE_NAME, key = "'search_list:' + #keyword + ':' + #limit")
    public List<PoetryMaster> searchByKeyword(String keyword, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return poetryMasterRepository.searchByKeywordWithPage(keyword, pageable).getContent();
    }
    
    /**
     * 根据两个关键字搜索诗词（同时包含两个关键字）- 用于双字飞花令
     */
    @Cacheable(value = CACHE_NAME, key = "'two_keywords:' + #keyword1 + ':' + #keyword2 + ':' + #page + ':' + #size")
    public Page<PoetryMaster> searchByTwoKeywords(String keyword1, String keyword2, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return poetryMasterRepository.searchByTwoKeywordsWithPage(keyword1, keyword2, pageable);
    }
    
    /**
     * 根据ID获取诗词详情 - 带缓存
     */
    @Cacheable(value = CACHE_NAME, key = "'detail:' + #id")
    public PoetryMaster getById(Long id) {
        return poetryMasterRepository.findById(id).orElse(null);
    }
    
    /**
     * 根据作者查找 - 带缓存
     */
    @Cacheable(value = CACHE_NAME, key = "'author:' + #author")
    public List<PoetryMaster> findByAuthor(String author) {
        return poetryMasterRepository.findByAuthor(author);
    }
    
    /**
     * 根据朝代查找 - 带缓存
     */
    @Cacheable(value = CACHE_NAME, key = "'dynasty:' + #dynasty")
    public List<PoetryMaster> findByDynasty(String dynasty) {
        return poetryMasterRepository.findByDynasty(dynasty);
    }
    
    /**
     * 根据诗词类型查找 - 带缓存
     */
    @Cacheable(value = CACHE_NAME, key = "'type:' + #poetryType")
    public List<PoetryMaster> findByPoetryType(String poetryType) {
        return poetryMasterRepository.findByPoetryType(poetryType);
    }
    
    /**
     * 获取诗词总数
     */
    public long getTotalCount() {
        return poetryMasterRepository.count();
    }
    
    public PoetryMaster getRandomPoetry() {
        long count = poetryMasterRepository.count();
        if (count == 0) {
            return null;
        }
        
        int randomOffset = ThreadLocalRandom.current().nextInt((int) count);
        Pageable pageable = PageRequest.of(randomOffset, 1);
        Page<PoetryMaster> page = poetryMasterRepository.findAll(pageable);
        
        return page.hasContent() ? page.getContent().get(0) : null;
    }
    
    /**
     * 根据作者模糊查询 - 带缓存
     */
    @Cacheable(value = CACHE_NAME, key = "'author_contains:' + #author")
    public List<PoetryMaster> findByAuthorContaining(String author) {
        return poetryMasterRepository.findByAuthorContaining(author);
    }
    
    /**
     * 根据标题模糊查询 - 带缓存
     */
    @Cacheable(value = CACHE_NAME, key = "'title_contains:' + #title")
    public List<PoetryMaster> findByTitleContaining(String title) {
        return poetryMasterRepository.findByTitleContaining(title);
    }

    @Cacheable(value = CACHE_NAME, key = "'kw_dynasty:' + #keyword + ':' + #dynasty + ':' + #page + ':' + #size")
    public Page<PoetryMaster> searchByKeywordAndDynasty(String keyword, String dynasty, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return poetryMasterRepository.searchByKeywordAndDynasty(keyword, dynasty, pageable);
    }

    @Cacheable(value = CACHE_NAME, key = "'kw_type:' + #keyword + ':' + #poetryType + ':' + #page + ':' + #size")
    public Page<PoetryMaster> searchByKeywordAndType(String keyword, String poetryType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return poetryMasterRepository.searchByKeywordAndType(keyword, poetryType, pageable);
    }

    @Cacheable(value = CACHE_NAME, key = "'author_page:' + #author + ':' + #page + ':' + #size")
    public Page<PoetryMaster> findByAuthorContainingWithPage(String author, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return poetryMasterRepository.findByAuthorContainingWithPage(author, pageable);
    }

    @Cacheable(value = CACHE_NAME, key = "'title_page:' + #title + ':' + #page + ':' + #size")
    public Page<PoetryMaster> findByTitleContainingWithPage(String title, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return poetryMasterRepository.findByTitleContainingWithPage(title, pageable);
    }
    
    /**
     * 清除搜索缓存（用于数据更新后）
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearCache() {
        // 清除所有缓存
    }
}
