package com.example.feihualingbattle.service;

import com.example.feihualingbattle.dto.CollectionDTO;
import com.example.feihualingbattle.entity.UserCollection;
import com.example.feihualingbattle.repository.UserCollectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CollectionService {
    private static final Logger log = LoggerFactory.getLogger(CollectionService.class);

    @Autowired
    private UserCollectionRepository collectionRepository;

    /**
     * 获取用户收藏列表（包含诗词信息）
     */
    public List<CollectionDTO> getUserCollections(Long userId) {
        return collectionRepository.findCollectionsWithPoetry(userId);
    }

    /**
     * 添加收藏（只保存poetryId，不保存冗余字段）
     */
    @Transactional
    public boolean addCollection(Long userId, Long poetryId) {
        log.info("添加收藏请求 - userId: {}, poetryId: {}", userId, poetryId);

        if (poetryId == null || poetryId <= 0) {
            throw new RuntimeException("诗词ID不能为空");
        }

        // 检查是否已收藏
        Optional<UserCollection> existing = collectionRepository.findByUserIdAndPoetryId(userId, poetryId);
        if (existing.isPresent()) {
            log.info("已收藏: poetryId={}", poetryId);
            return false;
        }

        UserCollection collection = new UserCollection();
        collection.setUserId(userId);
        collection.setPoetryId(poetryId);

        UserCollection saved = collectionRepository.save(collection);
        log.info("收藏成功: {}", saved);
        return true;
    }

    /**
     * @deprecated 使用 {@link #addCollection(Long, Long)}
     */
    @Deprecated
    @Transactional
    public boolean addCollection(Long userId, Long poetryId, String title, String author, 
                                  String dynasty, String content, String fullContent) {
        return addCollection(userId, poetryId);
    }

    @Transactional
    public void removeCollection(Long userId, Long poetryId) {
        collectionRepository.deleteByUserIdAndPoetryId(userId, poetryId);
    }

    public long countCollections(Long userId) {
        return collectionRepository.countByUserId(userId);
    }
}
