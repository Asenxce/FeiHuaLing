package com.example.feihualingbattle.service.strategy.rule;

import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.repository.PoetryRepository;
import com.github.houbb.opencc4j.util.ZhConverterUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BattleRuleHelper {

    public String normalize(String poetry) {
        if (poetry == null) return "";
        return ZhConverterUtil.toSimple(poetry)
                .replaceAll("[\\p{P}\\s]", "").trim();
    }

    public List<Poetry> searchAndFilter(PoetryRepository repo, String keyword,
                                         String answer, int limit) {
        List<Poetry> candidates = repo.findByKeywordInContentLimit(keyword, limit);
        String normalizedAnswer = normalize(answer);
        return candidates.stream()
            .filter(p -> normalize(p.getContent()).equals(normalizedAnswer))
            .collect(Collectors.toList());
    }

    public List<Poetry> searchByStartingChar(PoetryRepository repo, String startChar, int limit) {
        return repo.findByContentStartingWith(startChar, limit);
    }

    public List<Poetry> searchByKeyword(PoetryRepository repo, String keyword, int limit) {
        return repo.findByKeywordInContentLimit(keyword, limit);
    }
}
