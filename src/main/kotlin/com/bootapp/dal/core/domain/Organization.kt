package com.bootapp.dal.core.domain

import com.bootapp.dal.core.utils.Constants
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "organizations")
data class Organization (
        @Id
        @Column(nullable = false, updatable = false)
        var id: Long,
        @Column(nullable = false, updatable = false)
        var orgRoleId: Long,
        var status: Int,
        @Column(length = Constants.VARCHAR_SHORT)
        var name: String,
        var createAt: Long,
        var updateAt: Long
)

