package com.example.feihualingbattle.service;

import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.repository.PoetryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PoetryCacheService {
    private static final Logger log = LoggerFactory.getLogger(PoetryCacheService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PoetryRepository poetryRepository;

    private final ObjectMapper objectMapper;

    public PoetryCacheService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    private static final String CACHE_PREFIX = "poetry:kw:";
    private static final String CACHE_DOUBLE_PREFIX = "poetry:dkw:";
    private static final long CACHE_TTL_HOURS = 2;

    public List<Poetry> getCandidatesByKeyword(String keyword, int limit) {
        String cacheKey = CACHE_PREFIX + keyword + ":" + limit;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return deserializeList(cached.toString());
            }
        } catch (Exception e) {
            log.warn("Redis 读取缓存失败 (keyword={}): {}", keyword, e.getMessage());
        }

        List<Poetry> results = poetryRepository.fulltextSearchByContent(keyword, limit);
        if (results.isEmpty()) {
            results = poetryRepository.findByKeywordInContentLimit(keyword, limit);
        }

        try {
            String json = objectMapper.writeValueAsString(results);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("Redis 写入缓存失败 (keyword={}): {}", keyword, e.getMessage());
        }

        return results;
    }

    public List<Poetry> getCandidatesByTwoKeywords(String keyword1, String keyword2, int limit) {
        String cacheKey = CACHE_DOUBLE_PREFIX + keyword1 + ":" + keyword2 + ":" + limit;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return deserializeList(cached.toString());
            }
        } catch (Exception e) {
            log.warn("Redis 读取缓存失败 (keywords={}/{}): {}", keyword1, keyword2, e.getMessage());
        }

        List<Poetry> results = poetryRepository.fulltextSearchByTwoKeywords(keyword1, keyword2, limit);
        if (results.isEmpty()) {
            results = poetryRepository.findByTwoKeywordsInContent(keyword1, keyword2, limit);
        }

        try {
            String json = objectMapper.writeValueAsString(results);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("Redis 写入缓存失败 (keywords={}/{}): {}", keyword1, keyword2, e.getMessage());
        }

        return results;
    }

    public void invalidateKeyword(String keyword) {
        String pattern = CACHE_PREFIX + keyword + ":*";
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis 清除缓存失败 (keyword={}): {}", keyword, e.getMessage());
        }
    }

    private List<Poetry> deserializeList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Poetry>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
