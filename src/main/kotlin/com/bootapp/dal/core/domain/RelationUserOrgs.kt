package com.bootapp.dal.core.domain

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "r_user_orgs")
data class RelationUserOrgs (
        @Id
        @Column(nullable = false)
        var user_id: Long,
        @Column(nullable = false)
        var org_id: Long,
        var status: Int? = null,
        var createAt: Long,
        var updateAt: Long
)