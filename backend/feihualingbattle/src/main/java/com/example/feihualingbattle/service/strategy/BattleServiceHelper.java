package com.example.feihualingbattle.service.strategy;

import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.repository.PoetryRepository;
import com.github.houbb.opencc4j.util.ZhConverterUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BattleServiceHelper {

    public String normalizePoetry(String poetry) {
        return normalize(poetry);
    }

    public static String normalize(String poetry) {
        if (poetry == null) {
            return "";
        }
        return ZhConverterUtil.toSimple(poetry)
                .replaceAll("[\\p{P}\\s]", "")
                .trim();
    }

    public static List<Poetry> searchByKeywordAndFilter(PoetryRepository repo, String keyword,
                                                         String answer, int limit) {
        List<Poetry> candidates = repo.findByKeywordInContentLimit(keyword, limit);
        return candidates.stream()
            .filter(p -> normalize(p.getContent()).contains(answer))
            .collect(Collectors.toList());
    }
}
