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
        @Column(length = Constants.VARCHAR_SHORT)
        var phone: String,
        @Column(length = Constants.VARCHAR_SUPER_LONG)
        var email: String,
        var createAt: Long,
        var updateAt: Long
)