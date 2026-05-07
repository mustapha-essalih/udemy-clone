package com.dev.lms.user_service.repository;

import com.dev.lms.user_service.entity.Role;
import com.dev.lms.common.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(RoleName name);
}
