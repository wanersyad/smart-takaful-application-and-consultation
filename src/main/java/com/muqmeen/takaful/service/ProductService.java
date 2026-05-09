package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.Product;
import com.muqmeen.takaful.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> listAllForAdmin() {
        return productRepository.findAllByOrderByNameAsc();
    }

    public List<Product> listActiveForLanding() {
        return productRepository.findAllByActiveTrueOrderByFeaturedDescNameAsc();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public Optional<Product> findByName(String name) {
        return productRepository.findByName(name);
    }

    public Optional<Product> findActiveById(Long id) {
        return productRepository.findById(id).filter(Product::isActive);
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    public long count() {
        return productRepository.count();
    }
}
