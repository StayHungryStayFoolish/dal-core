package com.bootapp.dal.core.domain

import com.bootapp.dal.core.utils.Constants
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "user_authorities")
data class UserAuthority (
        @Id
        @Column(nullable = false, updatable = false, length = Constants.VARCHAR_LONG)
        var indexKey: String,
        var binaryValue: Long,
        @Column(nullable = false, length = Constants.VARCHAR_LONG)
        var orgAuthorityKey: String,
        var status: Int,
        @Column(length = Constants.VARCHAR_LONG)
        var name: String,
        var createAt: Long,
        var updateAt: Long
)