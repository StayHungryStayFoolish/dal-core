package com.bootapp.dal.core.utils

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class ChunkRequest(private var offset: Long = 0,
                   private var limit: Int = 0,
                   private var sort: Sort?) : Pageable {
    init {
        offset = 0
        if (limit <= 0) limit = 10
        if (sort == null) {
            sort = Sort(Sort.Direction.ASC, "id")
        }
    }
    constructor(limit: Int) : this(0, limit, null)

    override fun getPageNumber(): Int {
        return 0
    }

    override fun getPageSize(): Int {
        return limit
    }

    override fun getOffset(): Long {
        return offset
    }

    override fun getSort(): Sort {
        return this.sort!!
    }

    override fun next(): Pageable {
        return ChunkRequest(getOffset() + pageSize, pageSize, getSort())
    }

    private fun previous(): ChunkRequest {
        return if (hasPrevious()) ChunkRequest(getOffset() - pageSize, pageSize, getSort()) else this
    }

    override fun previousOrFirst(): Pageable {
        return if (hasPrevious()) previous() else first()
    }

    override fun first(): Pageable {
        return ChunkRequest(0, pageSize, getSort())
    }

    override fun hasPrevious(): Boolean {
        return offset > limit
    }
}