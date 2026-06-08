package com.muqmeen.takaful.repository;

import com.muqmeen.takaful.domain.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
