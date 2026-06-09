package com.muqmeen.takaful.repository;

import com.muqmeen.takaful.domain.SiteContentBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SiteContentBlockRepository extends JpaRepository<SiteContentBlock, Long> {
    List<SiteContentBlock> findAllByOrderByDisplayOrderAscContentKeyAsc();
    List<SiteContentBlock> findAllByContentKeyIn(Collection<String> contentKeys);
    Optional<SiteContentBlock> findByContentKey(String contentKey);
}
