package com.bootapp.dal.core.domain

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "id_generators")
data class IDGenInstance(
        @Id
        var id:Long,
        var updateAt:Long
)