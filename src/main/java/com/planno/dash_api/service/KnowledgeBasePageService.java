package com.planno.dash_api.service;

import com.planno.dash_api.dto.mapper.KnowledgeBasePageMapper;
import com.planno.dash_api.dto.request.KnowledgeBasePageRequestDTO;
import com.planno.dash_api.dto.response.KnowledgeBasePageResponseDTO;
import com.planno.dash_api.entity.KnowledgeBasePage;
import com.planno.dash_api.infra.exception.ResourceNotFoundException;
import com.planno.dash_api.repository.KnowledgeBasePageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class KnowledgeBasePageService {

    private final KnowledgeBasePageRepository repository;
    private final CurrentUserService currentUserService;
    private final KnowledgeBasePageMapper mapper;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<KnowledgeBasePageResponseDTO> findAll(String search) {
        Long tenantId = currentUserService.getCurrentTenantId();
        return repository.findAllByTenantIdOrderByPinnedDescUpdatedAtDesc(tenantId).stream()
                .filter(page -> matchesSearch(page, search))
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeBasePageResponseDTO findById(Long id) {
        Long tenantId = currentUserService.getCurrentTenantId();
        return mapper.toResponse(getPage(id, tenantId));
    }

    @Transactional
    public KnowledgeBasePageResponseDTO save(KnowledgeBasePageRequestDTO dto) {
        var currentUser = currentUserService.getCurrentUser();

        KnowledgeBasePage page = mapper.toEntity(withSummary(dto));
        page.setSlug(generateUniqueSlug(dto.title(), currentUser.getTenant().getId(), null));
        page.setCreatedBy(currentUser);
        page.setUpdatedBy(currentUser);
        page.setTenant(currentUser.getTenant());

        KnowledgeBasePage saved = repository.save(page);
        storageService.ensureFolder("/KnowledgeBase/" + saved.getSlug());
        return mapper.toResponse(saved);
    }

    @Transactional
    public KnowledgeBasePageResponseDTO update(Long id, KnowledgeBasePageRequestDTO dto) {
        Long tenantId = currentUserService.getCurrentTenantId();
        var currentUser = currentUserService.getCurrentUser();

        KnowledgeBasePage page = getPage(id, tenantId);
        mapper.applyUpdates(page, withSummary(dto));
        page.setSlug(generateUniqueSlug(dto.title(), tenantId, page.getId()));
        page.setUpdatedBy(currentUser);

        KnowledgeBasePage saved = repository.save(page);
        storageService.ensureFolder("/KnowledgeBase/" + saved.getSlug());
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = currentUserService.getCurrentTenantId();
        repository.delete(getPage(id, tenantId));
    }

    private KnowledgeBasePage getPage(Long id, Long tenantId) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagina da base de conhecimento nao encontrada."));
    }

    private boolean matchesSearch(KnowledgeBasePage page, String search) {
        if (!StringUtils.hasText(search)) {
            return true;
        }

        String normalizedSearch = search.toLowerCase(Locale.ROOT);
        return contains(page.getTitle(), normalizedSearch)
                || contains(page.getSummary(), normalizedSearch)
                || contains(page.getContent(), normalizedSearch);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private KnowledgeBasePageRequestDTO withSummary(KnowledgeBasePageRequestDTO dto) {
        if (StringUtils.hasText(dto.summary())) {
            return dto;
        }

        String normalizedContent = dto.content()
                .replace("#", "")
                .replace("*", "")
                .replace("`", "")
                .replace("_", " ")
                .trim();

        String summary = normalizedContent.length() <= 180 ? normalizedContent : normalizedContent.substring(0, 177) + "...";
        return new KnowledgeBasePageRequestDTO(dto.title(), summary, dto.content(), dto.pinned());
    }

    private String generateUniqueSlug(String title, Long tenantId, Long currentId) {
        String baseSlug = slugify(title);
        String candidate = baseSlug;
        int index = 2;

        while (existsSlug(tenantId, candidate, currentId)) {
            candidate = baseSlug + "-" + index++;
        }

        return candidate;
    }

    private boolean existsSlug(Long tenantId, String slug, Long currentId) {
        return currentId == null
                ? repository.existsByTenantIdAndSlug(tenantId, slug)
                : repository.existsByTenantIdAndSlugAndIdNot(tenantId, slug, currentId);
    }

    private String slugify(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "page" : normalized;
    }
}
