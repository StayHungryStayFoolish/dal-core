package com.bootapp.dal.core.service
import com.bootapp.dal.core.domain.QUser
import io.grpc.stub.StreamObserver
import com.bootapp.dal.core.grpc.User
import com.bootapp.dal.core.domain.User as DBUser
import com.bootapp.dal.core.grpc.UserServiceGrpc
import com.bootapp.dal.core.repository.UserRepository
import com.bootapp.dal.core.utils.ChunkRequest
import com.bootapp.dal.core.utils.idgen.IDGenerator
import com.querydsl.core.types.dsl.BooleanExpression
import org.lognet.springboot.grpc.GRpcService
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.lang.Exception

@GRpcService
class UserService(@Autowired
                  private val userRepository: UserRepository,
                  @Autowired
                  private val idGenerator: IDGenerator)
    : UserServiceGrpc.UserServiceImplBase() {

    var logger = LoggerFactory.getLogger(this.javaClass)!!

    override fun invokeNewUser(request: User.UserInfo?, responseObserver: StreamObserver<User.UserQueryResp>?) {
        val resp = User.UserQueryResp.newBuilder()
        try {
            val time = System.currentTimeMillis()
            val dbUser = DBUser(0, time, time, User.UserServiceType.USER_STATUS_INACTIVATED_VALUE, 0)
            //------------ handle userId
            if (request?.id != 0L) {
                dbUser.id = request!!.id
            } else {
                dbUser.id = idGenerator.nextId()
            }
            if (dbUser.id == 0L) {
                logger.error("the new user's id is zero")
                resp.status = User.UserServiceType.USER_CREATE_STATUS_FAIL
                resp.message = "user id could not be 0"
            } else {
                //------------ fill other data
                dbUser.fromProto(request)
                if (request.password != "") {
                    dbUser.passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())
                }
                //------------ fill information
                userRepository.save(dbUser)
                logger.info("new user saved to db with id: {}", dbUser.id)
                resp.status = User.UserServiceType.USER_CREATE_STATUS_SUCCESS
                resp.user = dbUser.toProto()
            }
        } catch (e : Exception) {
            logger.error(e.toString())
            resp.status = User.UserServiceType.USER_CREATE_STATUS_FAIL
        } finally {
            responseObserver?.onNext(resp.build())
            responseObserver?.onCompleted()
        }
    }

    override fun invokeUpdateUser(request: User.UserInfo?, responseObserver: StreamObserver<User.UserQueryResp>?) {
        val resp = User.UserQueryResp.newBuilder()
        try {
            val dbUser = request?.id?.let { userRepository.findById(it).orElse(null) }
            if (dbUser != null) {
                dbUser.fromProto(request)
                if (request.password != "") {
                    dbUser.passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())
                }
                resp.status = User.UserServiceType.USER_UPDATE_STATUS_SUCCESS
                resp.user = dbUser.toProto()
            } else {
                resp.status = User.UserServiceType.USER_UPDATE_STATUS_FAIL
                resp.message = "user not found"
                logger.error("user not found: {}", request?.id ?: 0)
            }
        } catch (e : Exception) {
            logger.error(e.toString())
            resp.status = User.UserServiceType.USER_UPDATE_STATUS_FAIL
        } finally {
            responseObserver?.onNext(resp.build())
            responseObserver?.onCompleted()
        }
    }

    override fun queryUser(request: User.UserQueryReq?, responseObserver: StreamObserver<User.UserQueryResp>?) {
        val resp = User.UserQueryResp.newBuilder()
        try {
            val dbUser = DBUser();request?.user?.let { dbUser.fromProto(it) }
            val userDsl = QUser.user; var queryExpressions : BooleanExpression? = null

            if (dbUser.id != null && dbUser.id != 0L) {
                queryExpressions = userDsl.id.eq(dbUser.id)
            } else {
                dbUser.username?.let { queryExpressions = (userDsl.username.eq(dbUser.username)).or(queryExpressions) }
                dbUser.email?.let { queryExpressions = (userDsl.email.eq(dbUser.email)).or(queryExpressions) }
                dbUser.phone?.let { queryExpressions = (userDsl.phone.eq(dbUser.phone)).or(queryExpressions) }
            }

            val res = userRepository.findOne(queryExpressions!!).get()
            if (request?.user?.password == "" || BCrypt.checkpw(request?.user?.password, res.passwordHash)) {
                resp.status = User.UserServiceType.USER_QUERY_STATUS_SUCCESS
                resp.user = res.toProto()
            } else {
                resp.status = User.UserServiceType.USER_QUERY_STATUS_WRONG_PASS
            }

        } catch (e: Exception) {
            logger.error(e.toString())
            resp.status = User.UserServiceType.USER_QUERY_STATUS_FAIL
        } finally {
            responseObserver?.onNext(resp.build())
            responseObserver?.onCompleted()
        }
    }

    override fun queryUsers(request: User.UsersQueryReq?, responseObserver: StreamObserver<User.UsersQueryResp>?) {
        val resp = User.UsersQueryResp.newBuilder()
        try {
            val dbUser = DBUser();request?.user?.let { dbUser.fromProto(it) }
            val userDsl = QUser.user; var queryExpressions : BooleanExpression? = null

            dbUser.username?.let { queryExpressions = (userDsl.username.like("%" + dbUser.username)).or(queryExpressions) }
            dbUser.email?.let { queryExpressions = (userDsl.email.like("%" + dbUser.email)).or(queryExpressions) }
            dbUser.phone?.let { queryExpressions = (userDsl.phone.like("%" + dbUser.phone)).or(queryExpressions) }

            val res = userRepository.findAll(
                    userDsl.id.gt(request?.offsetId ?: 0).and(queryExpressions),
                    ChunkRequest(request?.limit ?: 0)
            )

            res.forEach { resp.addUsers(it.toProto()) }
            resp.status = User.UserServiceType.USER_QUERY_STATUS_SUCCESS
        } catch (e: Exception) {
            logger.error(e.toString())
            resp.status = User.UserServiceType.USER_QUERY_STATUS_FAIL
        } finally {
            responseObserver?.onNext(resp.build())
            responseObserver?.onCompleted()
        }
    }
}