package com.bootapp.dal.core.service

import com.bootapp.dal.core.grpc.Auth
import com.bootapp.dal.core.grpc.DalCoreAuthServiceGrpc
import com.bootapp.dal.core.repository.OrgAuthRepository
import com.bootapp.dal.core.repository.UserAuthRepository
import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import org.lognet.springboot.grpc.GRpcService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@GRpcService
class AuthService(@Autowired
                  private val userAuthRepository: UserAuthRepository,
                  @Autowired
                  private val orgAuthRepository: OrgAuthRepository)
    :DalCoreAuthServiceGrpc.DalCoreAuthServiceImplBase() {
    var logger = LoggerFactory.getLogger(this.javaClass)!!
    override fun getAuthorities(request: Empty?, responseObserver: StreamObserver<Auth.Authorities>?) {
        val authorities = Auth.Authorities.newBuilder()
        try {
            val userAuthorities = userAuthRepository.findAll()
            val orgAuthorities = orgAuthRepository.findAll()
            authorities.addAllOrgAuthorities(orgAuthorities.map { it.toProto() })
            authorities.addAllUserAuthorities(userAuthorities.map { it.toProto() })
            responseObserver?.onNext(authorities.build())
            responseObserver?.onCompleted()
        } catch (e: Throwable) {
            logger.error(e.toString())
            responseObserver?.onError(e)
            responseObserver?.onCompleted()
        }

    }
}
