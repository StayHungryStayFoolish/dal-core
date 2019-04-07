package com.bootapp.dal.core.domain

import com.bootapp.dal.core.utils.Constants
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "admins")
data class Admin (
        @Id
        @Column(nullable = false, updatable = false)
        var id: Long,
        @Column(nullable = false, updatable = false)
        var userRoleId: Long,
        var status: Int,
        @Column(length = Constants.VARCHAR_SHORT, unique = true)
        var phone: String,
        @Column(length = Constants.VARCHAR_SUPER_LONG, unique = true)
        var email: String,
        @Column(length = Constants.VARCHAR_NORMAL, unique = true)
        var username: String? = null,
        @Column(length = Constants.VARCHAR_PASSWORD_HASH)
        var passwordHash: String? = null,
        var createAt: Long,
        var updateAt: Long
)