package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository 模板。
 *
 * 约定：
 *  1. 只做数据访问，不包含业务逻辑
 *  2. 查询方法命名遵循 Spring Data JPA 约定
 *  3. 批量操作注意分页
 *
 * 使用方式：Phase 3 切片的持久层按此模板生成。
 */
@Repository
public interface ResourceRepository extends JpaRepository<ResourceEntity, Long> {

    Optional<ResourceEntity> findByBusinessKey(String businessKey);

    boolean existsByBusinessKey(String businessKey);

    // 批量查询：加 Pageable 参数
    // List<ResourceEntity> findByStatus(Status status, Pageable pageable);
}
