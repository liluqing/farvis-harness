package com.example.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity 模板。
 *
 * 约定：
 *  1. 不暴露 Setter（状态变更通过专用方法）
 *  2. 静态工厂方法 > 构造函数
 *  3. 审计字段统一（createdAt, updatedAt）
 *  4. 使用 @Builder（但谨慎，避免 Builder 绕过业务规则）
 *  5. SQL 保留字规避：表名避免使用 user/order/group/key 等保留字，
 *     如必须使用则加双引号（H2）或反引号（MySQL），
 *     例如 @Table(name = "`order`") 或 @Table(name = "\"user\"")
 *
 * 使用方式：Phase 3 切片的实体类按此模板生成。
 */
@Entity
@Table(name = "resources")
public class ResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务主键（唯一约束） */
    @Column(nullable = false, unique = true, length = 100)
    private String businessKey;

    /** 状态 */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status;

    // ====== 审计字段 ======

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public enum Status {
        CREATED, ACTIVE, INACTIVE, DELETED
    }

    // ====== 工厂方法 ======

    public static ResourceEntity create(String businessKey) {
        ResourceEntity entity = new ResourceEntity();
        entity.businessKey = businessKey;
        entity.status = Status.CREATED;
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        return entity;
    }

    // ====== 状态变更（禁止直接 setStatus） ======

    public void activate() {
        if (this.status != Status.CREATED && this.status != Status.INACTIVE) {
            throw new IllegalStateException("Cannot activate from status: " + this.status);
        }
        this.status = Status.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = Status.INACTIVE;
        this.updatedAt = Instant.now();
    }

    // ====== Getters（只读） ======

    public Long getId() { return id; }
    public String getBusinessKey() { return businessKey; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
