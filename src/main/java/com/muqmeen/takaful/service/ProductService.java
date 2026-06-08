package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.Product;
import com.muqmeen.takaful.repository.ProductRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> listAllForAdmin() {
        List<Product> products = productRepository.findAllByOrderByNameAsc();
        products.forEach(this::initializeDetails);
        return products;
    }

    public List<Product> listActiveForLanding() {
        List<Product> products = productRepository.findAllByActiveTrueAndArchivedFalseOrderByFeaturedDescNameAsc();
        products.forEach(this::initializeDetails);
        return products;
    }

    public List<Product> listVisibleForAdmin() {
        List<Product> products = productRepository.findAllByArchivedFalseOrderByNameAsc();
        products.forEach(this::initializeDetails);
        return products;
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id).map(this::initializeDetails);
    }

    public Optional<Product> findByName(String name) {
        return productRepository.findByName(name);
    }

    public Optional<Product> findActiveById(Long id) {
        return productRepository.findById(id)
                .filter(product -> product.isActive() && !product.isArchived())
                .map(this::initializeDetails);
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public Product archiveById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.setActive(false);
        product.setArchived(true);
        return productRepository.save(product);
    }

    public long count() {
        return productRepository.count();
    }

    private Product initializeDetails(Product product) {
        Hibernate.initialize(product.getBenefits());
        Hibernate.initialize(product.getCoverageItems());
        Hibernate.initialize(product.getRequirements());
        Hibernate.initialize(product.getDocuments());
        return product;
    }
}
