package com.bootapp.dal.core.service

import com.bootapp.dal.core.grpc.User
import com.bootapp.dal.core.grpc.UserServiceGrpc
import com.bootapp.dal.core.repository.IDGenInstanceRepository
import com.bootapp.dal.core.repository.UserRepository
import com.bootapp.dal.core.utils.idgen.impl.SnowFlakeGenerator
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import spock.lang.Shared
import spock.lang.Specification

import javax.annotation.Resource

@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DataJpaTest
class UserServiceTest extends Specification {
    String serverName = InProcessServerBuilder.generateName()
    @Resource
    private UserRepository userRepository
    @Resource
    private IDGenInstanceRepository repository
    @Shared
    boolean sharedSetupDone = false
    @Shared
    UserServiceGrpc.UserServiceBlockingStub blockingStub
    def setup() {
        if (!sharedSetupDone) {
            InProcessServerBuilder.forName(serverName).directExecutor().addService(new UserService(userRepository, new SnowFlakeGenerator(repository))).build().start()
            def channel = InProcessChannelBuilder.forName(serverName).directExecutor().build()
            blockingStub = UserServiceGrpc.newBlockingStub(channel)
            sharedSetupDone = true
        }
    }
    def getUserInfo(String username, String email, String phone, String pass) {
        def user = User.UserInfo.newBuilder()
        if(username != null) user.setUsername(username)
        if(email != null) user.setEmail(email)
        if(phone != null) user.setPhone(phone)
        if(pass != null) user.setPassword(pass)
        return user.build()
    }

    def "InvokeNewUser.1 basic creation of a user"() {
        given: "create a user"
        def user = User.UserInfo.newBuilder().build()
        def userCreateResp = blockingStub.invokeNewUser(user)
        def userResp = blockingStub.queryUser(User.UserInfo.newBuilder().setId(userCreateResp.user.id).build())
        expect: "the created user exists"
        userResp.status == User.UserServiceType.USER_QUERY_STATUS_SUCCESS && userResp.user.id == userCreateResp.user.id
        cleanup:
        userRepository.deleteAll()
    }
    def "InvokeNewUser.2 the username & email & phone should be all unique"() {
        when:
        def userCreateResp = blockingStub.invokeNewUser(userReq)
        then:
        (isSuccess && userCreateResp.status == User.UserServiceType.USER_CREATE_STATUS_SUCCESS) ||
                (!isSuccess && userCreateResp.status == User.UserServiceType.USER_CREATE_STATUS_FAIL)
        where: "a user with same username|email|phone couldn't be created twice"
        userReq                                               || isSuccess
        getUserInfo("a0",null,null,"Abcd1234")                || true
        getUserInfo("a0",null,null,"Abcd1234")                || false
        getUserInfo(null,"a1@test.com",null,"Abcd1234")       || true
        getUserInfo(null,"a1@test.com",null,"Abcd1234")       || false
        getUserInfo(null,null,"+1-13800138000","Abcd1234")    || true
        getUserInfo(null,null,"+1-13800138000","Abcd1234")    || false

    }
    def "QueryUser.1 querying a user by username/phone/email"() {
        when:
        def userCreateResp = blockingStub.invokeNewUser(user)
        def userResp = blockingStub.queryUser(userReq)
        then: "the result user's id should be the same"
        userResp.status == User.UserServiceType.USER_QUERY_STATUS_SUCCESS &&
                userResp.user.id == userCreateResp.user.id
        where:
        user | userReq
        getUserInfo("b0",null,null,null) | getUserInfo("b0",null,null,null)
        getUserInfo(null,"b1@test.com",null,null) | getUserInfo(null,"b1@test.com",null,null)
        getUserInfo(null,null,"+2-13800138000",null) | getUserInfo(null,null,"+2-13800138000",null)
    }

    def "QueryUser.2 querying a user by username/phone/email & pass"() {
        when:
        blockingStub.invokeNewUser(user)
        def userResp = blockingStub.queryUser(userReq)
        then:
        (type == 1 && userResp.status == User.UserServiceType.USER_QUERY_STATUS_SUCCESS) ||
                (type == 2 && userResp.status == User.UserServiceType.USER_QUERY_STATUS_WRONG_PASS) ||
                (type == 3 && userResp.status == User.UserServiceType.USER_QUERY_STATUS_FAIL)
        where: "be able to test password, and be able to login by username|email|phone"
        user                                                  | userReq || type
        getUserInfo("c0",null,null,"Abcd1234")                | getUserInfo("c0",null,null,"Abcd1234") || 1
        getUserInfo(null, null, null, null)                   | getUserInfo("c0", null, null, "Abcd12342")||2
        getUserInfo(null, "c2@test.com", null, "Abcd1234")    | getUserInfo(null, "c2@test.com", null, "Abcd1234") || 1
        getUserInfo(null, null, null, null)                   | getUserInfo(null, "c2@test.com", null, "Abcd12342")|| 2
        getUserInfo(null, null, "+3-13800138000", "Abcd1234") | getUserInfo(null, null, "+3-13800138000", "Abcd1234") || 1
        getUserInfo(null, null, null, null)                   | getUserInfo(null, null, "+3-13800138000", "Abcd12342") || 2
        getUserInfo(null, null, null, null) | getUserInfo("c0", "c0", "c0", "Abcd1234") || 1
        getUserInfo(null, null, null, null) | getUserInfo("c0", "c0", "c0", "Abcd12324") || 2
        getUserInfo(null, null, null, null) | getUserInfo("c2@test.com", "c2@test.com", "c2@test.com", "Abcd1234") || 1
        getUserInfo(null, null, null, null) | getUserInfo("c2@test.com", "c2@test.com", "c2@test.com", "Abcd12324") || 2
        getUserInfo(null, null, null, null) | getUserInfo("+3-13800138000", "+3-13800138000", "+3-13800138000", "Abcd1234") || 1
        getUserInfo(null, null, null, null) | getUserInfo("+3-13800138000", "+3-13800138000", "+3-13800138000", "Abcd12324") || 2
        getUserInfo(null, null, null, null) | getUserInfo("c3", "c3", "c3", "Abcd1234") || 3
    }

    def "QueryUsers.1 offset&limit test"() {
        given:
        User.UserQueryResp resp2 = blockingStub.invokeNewUser(User.UserInfo.newBuilder().build())
        for(int i = 0; i < 2; i++)
            blockingStub.invokeNewUser(User.UserInfo.newBuilder().build())
        User.UsersQueryResp resp = blockingStub.queryUsers(User.UsersQueryReq.newBuilder().setOffsetId(resp2.user.id).build())
        expect:
        resp.status == User.UserServiceType.USER_QUERY_STATUS_SUCCESS && resp.getUsersCount() == 2
        cleanup:
        userRepository.deleteAll()
        for (int i = 0; i < 5; i++) {
            blockingStub.invokeNewUser(getUserInfo(String.format("test%d", i), null, null, null))
            blockingStub.invokeNewUser(getUserInfo(null, String.format("test%d@abc.com", i), null, null))
            blockingStub.invokeNewUser(getUserInfo(null, null, String.format("test%d", i), null))
        }
    }
    def "QueryUsers.2 like query"() {
        when:
        def resp1 = blockingStub.queryUsers(User.UsersQueryReq.newBuilder().setUser(user).build())
        then:
        resp1.status == User.UserServiceType.USER_QUERY_STATUS_SUCCESS && resp1.usersCount == count
        where:
        user                                   || count
        getUserInfo("test", null, null, null)  || 5
        getUserInfo("test2", null, null, null) || 1
        getUserInfo("testa", null, null, null) || 0
        getUserInfo(null, "test", null, null)  || 5
        getUserInfo(null, "test2", null, null) || 1
        getUserInfo(null, "testa", null, null) || 0
        getUserInfo(null, null, "test", null)  || 5
        getUserInfo(null, null, "test2", null) || 1
        getUserInfo(null, null, "testa", null) || 0
        getUserInfo("test", null, "test", null) || 10
        getUserInfo("test", "test", "test", null) || 10
    }

    def "InvokeUpdateUser.1 update user info by id"() {
        given:
        def resp1 = blockingStub.invokeNewUser(User.UserInfo.newBuilder().build())
        def userInfo = User.UserInfo.newBuilder().setId(resp1.user.id).setUsername("d0").setEmail("d1@test.com").setPhone("+86-13800138000").setPassword("test1234").build()
        blockingStub.invokeUpdateUserById(userInfo)
        def resp2 = blockingStub.queryUser(User.UserInfo.newBuilder().setId(resp1.user.id).build())
        def resp3 = blockingStub.queryUser(User.UserInfo.newBuilder().setEmail("d1@test.com").setPassword("test1234").build())
        expect:
        resp2.status == User.UserServiceType.USER_QUERY_STATUS_SUCCESS
        resp2.user.username == userInfo.username
        resp2.user.email == userInfo.email
        resp2.user.phone == userInfo.phone
        resp2.user.id == userInfo.id
        resp3.status == User.UserServiceType.USER_QUERY_STATUS_SUCCESS
        cleanup:
        userRepository.deleteAll()
    }
    def "InvokeDelInactiveUsers"() {

    }

}