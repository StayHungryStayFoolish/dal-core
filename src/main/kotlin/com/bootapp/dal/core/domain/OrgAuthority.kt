package com.bootapp.dal.core.domain

import com.bootapp.dal.core.grpc.Auth
import com.bootapp.dal.core.utils.Constants
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "org_authorities")
data class OrgAuthority (
        @Id
        @Column(nullable = false, updatable = false, length = Constants.VARCHAR_LONG)
        var orgAuthKey: String,
        var orgAuthValue: Long,
        var status: Int,
        @Column(length = Constants.VARCHAR_LONG)
        var name: String,
        var createAt: Long,
        var updateAt: Long
) {
        fun toProto(): Auth.OrgAuthority {
                val proto = Auth.OrgAuthority.newBuilder()
                proto.orgAuthKey = orgAuthKey
                proto.orgAuthValue = orgAuthValue
                proto.name = name
                return proto.buildPartial()
        }
}