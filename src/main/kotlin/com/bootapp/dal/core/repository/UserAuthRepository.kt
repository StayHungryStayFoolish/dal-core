package com.bootapp.dal.core.repository

import com.bootapp.dal.core.domain.User
import com.bootapp.dal.core.domain.UserAuthority
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor


interface UserAuthRepository:JpaRepository<UserAuthority, Long>
