package com.bootapp.dal.core.repository

import com.bootapp.dal.core.domain.OrgAuthority
import com.bootapp.dal.core.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor


interface OrgAuthRepository:JpaRepository<OrgAuthority, Long>
