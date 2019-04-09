package com.bootapp.dal.core.domain

import com.bootapp.dal.core.grpc.Auth
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
        var userAuthKey: String,
        var userAuthValue: Long,
        @Column(nullable = false, length = Constants.VARCHAR_LONG)
        var orgAuthKey: String,
        var status: Int,
        @Column(length = Constants.VARCHAR_LONG)
        var name: String,
        var createAt: Long,
        var updateAt: Long
) {
        fun toProto(): Auth.UserAuthority {
                val proto = Auth.UserAuthority.newBuilder()
                proto.orgAuthKey = orgAuthKey
                proto.userAuthKey = userAuthKey
                proto.userAuthValue = userAuthValue
                proto.name = name
                return proto.buildPartial()
        }
}