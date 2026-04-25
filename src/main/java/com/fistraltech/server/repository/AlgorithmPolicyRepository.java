package com.fistraltech.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fistraltech.server.model.AlgorithmPolicyEntity;

/**
 * Repository for persisted runtime algorithm policies.
 */
@Repository
public interface AlgorithmPolicyRepository extends JpaRepository<AlgorithmPolicyEntity, String> {
}