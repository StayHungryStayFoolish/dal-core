package com.bootapp.dal.core.domain

import com.bootapp.dal.core.utils.Constants
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "user_roles")
data class UserRole (
        @Id
        @Column(nullable = false, updatable = false)
        var id: Long,
        @Column(nullable = false, updatable = false)
        var orgId: Long,
        var status: Int,
        @Column(length = Constants.VARCHAR_LONG)
        var name: String,
        @Column(length = Constants.VARCHAR_SUPER_LONG)
        var remark: String,
        @Column(columnDefinition="text")
        var authorities: String,
        var createAt: Long,
        var updateAt: Long
)