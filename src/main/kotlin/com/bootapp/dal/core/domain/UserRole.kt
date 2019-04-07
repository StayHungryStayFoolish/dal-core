package com.bootapp.dal.core.domain

import com.bootapp.dal.core.utils.Constants
import javax.persistence.*

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
        @Lob
        @Column(columnDefinition="text")
        var authorities: String,
        var createAt: Long,
        var updateAt: Long
)