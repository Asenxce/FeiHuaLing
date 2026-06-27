package com.example.feihualingbattle.service.strategy;

import com.example.feihualingbattle.entity.Battle;
import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.repository.PoetryRepository;
import com.example.feihualingbattle.service.PoetryCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ForbiddenStrategy implements GameModeStrategy {

    @Autowired
    private PoetryRepository poetryRepository;

    @Autowired
    private PoetryCacheService poetryCacheService;

    private static final int SEARCH_LIMIT = 500;

    @Override
    public String getModeName() { return "forbidden"; }

    @Override
    public String validate(Battle battle, String normalizedAnswer) {
        String forbiddenWord = battle.getForbiddenWord();
        if (forbiddenWord == null || forbiddenWord.isEmpty()) {
            return "禁止词未设置";
        }
        if (normalizedAnswer.contains(forbiddenWord)) {
            return "答案不能包含禁止词：" + forbiddenWord;
        }
        return null;
    }

    @Override
    public List<Poetry> searchPoetry(GameModeContext ctx) {
        String normalizedAnswer = BattleServiceHelper.normalize(ctx.getAnswer());
        List<Poetry> candidates = poetryCacheService.getCandidatesByKeyword(ctx.getAnswer(), SEARCH_LIMIT);
        return candidates.stream()
            .filter(p -> BattleServiceHelper.normalize(p.getContent()).equals(normalizedAnswer))
            .collect(Collectors.toList());
    }

    @Override
    public List<Poetry> getAIAnswer(GameModeContext ctx, List<String> usedPoems) {
        // 使用禁忌词搜索不包含该词的诗句
        String forbiddenWord = ctx .getForbiddenWord() != null && !ctx.getForbiddenWord().isEmpty()
                ? ctx.getForbiddenWord() : ctx.getKeyword();
        List<Poetry> candidates = poetryRepository.findByContentNotContaining(forbiddenWord, 200);
        if (usedPoems != null && !usedPoems.isEmpty()) {
            java.util.Set<String> usedSet = new java.util.HashSet<>(usedPoems);
            candidates = candidates.stream()
                .filter(p -> !usedSet.contains(p.getContent()))
                .collect(java.util.stream.Collectors.toList());
        }
        return candidates;
    }
}
