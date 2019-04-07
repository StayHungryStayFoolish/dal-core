package com.bootapp.dal.core.domain

import com.bootapp.dal.core.grpc.User
import com.bootapp.dal.core.utils.Constants.VARCHAR_NORMAL
import com.bootapp.dal.core.utils.Constants.VARCHAR_PASSWORD_HASH
import com.bootapp.dal.core.utils.Constants.VARCHAR_SHORT
import com.bootapp.dal.core.utils.Constants.VARCHAR_SUPER_LONG
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "users")
data class User (
    @Id
    @Column(nullable = false, updatable = false)
    var id: Long? = null,
    var createAt: Long? = null,
    var updateAt: Long? = null,
    var status: Int? = null,
    @Column(nullable = false, updatable = false)
    var userRoleId: Long? = null,
    @Column(length = VARCHAR_SHORT, unique = true)
    var phone: String? = null,
    @Column(length = VARCHAR_SUPER_LONG, unique = true)
    var email: String? = null,
    @Column(length = VARCHAR_NORMAL, unique = true)
    var username: String? = null,
    @Column(length = VARCHAR_PASSWORD_HASH)
    var passwordHash: String? = null
) {
    fun toProto() :User.UserInfo {
        val proto = User.UserInfo.newBuilder()
        proto.id = this.id ?: 0
        proto.statusValue = this.status ?: 0
        proto.userRoleId = this.userRoleId ?: 0
        proto.phone = this.phone?: ""
        proto.email = this.email?: ""
        proto.username = this.username?: ""
        proto.createAt = this.createAt ?: 0
        proto.updateAt = this.updateAt ?: 0
        return proto.buildPartial()
    }
    fun fromProto(info :User.UserInfo) {
        if (info.id != 0L) id = info.id
        if (info.statusValue != 0) status = info.statusValue
        if (info.userRoleId != 0L) userRoleId = info.userRoleId
        if (info.phone != "") phone = info.phone
        if (info.email != "") email = info.email
        if (info.username != "") username = info.username
        if (info.createAt != 0L) createAt = info.createAt
        if (info.updateAt != 0L) updateAt = info.updateAt
    }
}

