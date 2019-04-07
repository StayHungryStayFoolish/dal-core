package com.bootapp.dal.core.config

import org.springframework.context.annotation.Configuration
import javax.annotation.Resource
import javax.sql.DataSource

@Configuration
open class DataSourceConfiguration(
    @Resource
    var dataSource: DataSource
) {
}