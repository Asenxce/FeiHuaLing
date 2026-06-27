package com.example.feihualingbattle.service;

import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.repository.PoetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 诗词业务服务类（单句诗词）
 * 优化：使用Spring Cache注解简化缓存代码
 */
@Service
public class PoetryService {
    
    @Autowired
    private PoetryRepository poetryRepository;
    
    private static final String CACHE_NAME = "poetry";
    
    /**
     * 根据关键字在诗句内容、作者、标题中搜索包含该关键字的诗词(带缓存)
     */
    @Cacheable(value = CACHE_NAME, key = "'search:' + #keyword")
    public List<Poetry> searchByKeyword(String keyword) {
        return poetryRepository.searchByKeyword(keyword);
    }
    
    /**
     * 根据关键字搜索，支持分页和排序（按使用次数降序）
     */
    @Cacheable(value = CACHE_NAME, key = "'search_page:' + #keyword + ':' + #page + ':' + #size")
    public Page<Poetry> searchByKeywordWithPage(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return poetryRepository.findByKeywordInContentWithLimit(keyword, pageable);
    }
    
    /**
     * 根据关键字和指定位置搜索诗词(用于经典模式的位置飞花令)
     * 注意：位置过滤需要在内存中进行，不适合缓存
     */
    public List<Poetry> searchByKeywordAtPosition(String keyword, int position) {
        List<Poetry> allPoems = poetryRepository.findByKeywordInContent(keyword);
        
        return allPoems.stream()
            .filter(poetry -> {
                if (poetry.getContent() == null || poetry.getContent().length() < position) {
                    return false;
                }
                char charAtPosition = poetry.getContent().charAt(position - 1);
                return String.valueOf(charAtPosition).equals(keyword);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 根据两个关键字搜索诗词(用于双字飞花令) - 带缓存
     */
    @Cacheable(value = CACHE_NAME, key = "'two_keywords:' + #keyword1 + ':' + #keyword2")
    public List<Poetry> searchByTwoKeywords(String keyword1, String keyword2) {
        return poetryRepository.findByTwoKeywordsInContent(keyword1, keyword2, 500);
    }
    
    /**
     * 根据两个关键字搜索，支持分页和排序
     */
    @Cacheable(value = CACHE_NAME, key = "'two_keywords_page:' + #keyword1 + ':' + #keyword2 + ':' + #page + ':' + #size")
    public Page<Poetry> searchByTwoKeywordsWithPage(String keyword1, String keyword2, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return poetryRepository.findByTwoKeywordsInContentWithLimit(keyword1, keyword2, pageable);
    }
    
    /**
     * 验证诗句是否正确(简化版)
     */
    public boolean validatePoetry(String content, String keyword) {
        return content != null && keyword != null && content.contains(keyword);
    }
    
    /**
     * 随机获取包含关键字的诗词
     */
    public Optional<Poetry> getRandomPoetryByKeyword(String keyword) {
        List<Poetry> poems = searchByKeyword(keyword);
        if (poems.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(poems.get(ThreadLocalRandom.current().nextInt(poems.size())));
    }
    
    /**
     * 根据两个关键字搜索诗词(用于双字飞花令) - 别名方法
     */
    public List<Poetry> searchByDoubleKeywords(String keyword1, String keyword2) {
        return searchByTwoKeywords(keyword1, keyword2);
    }
    
    /**
     * 查找以指定字符开头的诗句（用于接龙模式）
     */
    @Cacheable(value = CACHE_NAME, key = "'starts_with:' + #startChar + ':' + #limit")
    public List<Poetry> findByContentStartingWith(String startChar, int limit) {
        return poetryRepository.findByContentStartingWith(startChar, limit);
    }
    
    /**
     * 查找不包含指定关键字的诗句（用于反飞花令模式）
     */
    @Cacheable(value = CACHE_NAME, key = "'not_contains:' + #keyword + ':' + #limit")
    public List<Poetry> findByContentNotContaining(String keyword, int limit) {
        return poetryRepository.findByContentNotContaining(keyword, limit);
    }
}