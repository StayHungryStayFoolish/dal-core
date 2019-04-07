package com.bootapp.dal.core

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder

@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class])
@SpringBootApplication
class DalCoreApplication

fun main(args: Array<String>) {
	SpringApplicationBuilder(DalCoreApplication::class.java).web(WebApplicationType.NONE).run(*args)
}