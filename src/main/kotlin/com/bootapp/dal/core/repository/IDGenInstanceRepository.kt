package com.bootapp.dal.core.repository

import com.bootapp.dal.core.domain.IDGenInstance
import org.springframework.data.jpa.repository.JpaRepository

interface IDGenInstanceRepository: JpaRepository<IDGenInstance, Long>