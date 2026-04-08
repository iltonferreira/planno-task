package com.planno.dash_api.dto.mapper;

import com.planno.dash_api.dto.request.KnowledgeBasePageRequestDTO;
import com.planno.dash_api.dto.response.KnowledgeBasePageResponseDTO;
import com.planno.dash_api.entity.KnowledgeBasePage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KnowledgeBasePageMapper {

    private final UserMapper userMapper;

    public KnowledgeBasePage toEntity(KnowledgeBasePageRequestDTO dto) {
        KnowledgeBasePage page = new KnowledgeBasePage();
        applyUpdates(page, dto);
        return page;
    }

    public void applyUpdates(KnowledgeBasePage page, KnowledgeBasePageRequestDTO dto) {
        page.setTitle(dto.title());
        page.setSummary(dto.summary());
        page.setContent(dto.content());
        page.setPinned(Boolean.TRUE.equals(dto.pinned()));
    }

    public KnowledgeBasePageResponseDTO toResponse(KnowledgeBasePage page) {
        return new KnowledgeBasePageResponseDTO(
                page.getId(),
                page.getTitle(),
                page.getSlug(),
                page.getSummary(),
                page.getContent(),
                page.isPinned(),
                countWords(page.getContent()),
                userMapper.toSummary(page.getCreatedBy()),
                userMapper.toSummary(page.getUpdatedBy()),
                page.getCreatedAt(),
                page.getUpdatedAt()
        );
    }

    private int countWords(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }

        return content.trim().split("\\s+").length;
    }
}
