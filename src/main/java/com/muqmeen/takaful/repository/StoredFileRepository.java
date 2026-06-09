package com.muqmeen.takaful.repository;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    List<StoredFile> findByCustomer(Customer customer);
}
