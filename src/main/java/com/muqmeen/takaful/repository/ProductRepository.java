package com.muqmeen.takaful.repository;

import com.muqmeen.takaful.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByActiveTrueOrderByFeaturedDescNameAsc();

    List<Product> findAllByOrderByNameAsc();

    Optional<Product> findByName(String name);
}
