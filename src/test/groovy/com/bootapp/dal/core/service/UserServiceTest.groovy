package com.bootapp.dal.core.service

import com.bootapp.dal.core.grpc.User
import com.bootapp.dal.core.grpc.UserServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest
class UserServiceTest extends Specification {
    ManagedChannel channel = ((ManagedChannelBuilder)ManagedChannelBuilder.
            forAddress("localhost", 9090).usePlaintext()).build()
    UserServiceGrpc.UserServiceBlockingStub blockingStub = UserServiceGrpc.newBlockingStub(channel)
    def "basic creation of a user"() {
        given: "create a user"
        def user = User.UserInfo.newBuilder().build()
        def userCreateResp = blockingStub.invokeNewUser(user)
        def userResp = blockingStub.queryUser(User.UserQueryReq.newBuilder()
                .setUser(User.UserInfo.newBuilder().setId(userCreateResp.user.id).build()).build())
        expect: "the created user exists"
        userResp.status == User.UserServiceType.USER_QUERY_STATUS_SUCCESS
        userResp.user.id == userCreateResp.user.id
    }
    def "the username & email & phone should be all unique"() {
        when:
        def userCreateResp = blockingStub.invokeNewUser(userReq)
        then:
        if(isSuccess) {
            userCreateResp.status == User.UserServiceType.USER_CREATE_STATUS_SUCCESS
        } else {
            userCreateResp.status == User.UserServiceType.USER_CREATE_STATUS_FAIL
        }
        where: "a user with same username|email|phone couldn't be created twice"
        userReq                                               || isSuccess
        getUserInfo("a0",null,null,"Abcd1234")                || true
        getUserInfo("a0",null,null,"Abcd1234")                || false
        getUserInfo(null,"a1@test.com",null,"Abcd1234")       || true
        getUserInfo(null,"a1@test.com",null,"Abcd1234")       || false
        getUserInfo(null,null,"+1-13800138000","Abcd1234")    || true
        getUserInfo(null,null,"+1-13800138000","Abcd1234")    || false

    }
    def "creating users by username/phone/email"() {
        when:
        def userCreateResp = blockingStub.invokeNewUser(user)
        def userResp = blockingStub.queryUser(User.UserQueryReq.newBuilder().setUser(userReq).build())
        then: "the result user's id should be the same"
        userResp.status == User.UserServiceType.USER_QUERY_STATUS_SUCCESS
        userResp.user.id == userCreateResp.user.id
        where:
        user | userReq
        getUserInfo("b0",null,null,null) | getUserInfo("b0",null,null,null)
        getUserInfo(null,"b1@test.com",null,null) | getUserInfo(null,"b1@test.com",null,null)
        getUserInfo(null,null,"+2-13800138000",null) | getUserInfo(null,null,"+2-13800138000",null)
    }

    def "creating users by username/phone/email & pass"() {
        when:
        def userCreateResp = blockingStub.invokeNewUser(user)
        def userResp = blockingStub.queryUser(User.UserQueryReq.newBuilder().setUser(userReq).build())
        then:
        if(isSuccess) {
            (userResp.status == User.UserServiceType.USER_QUERY_STATUS_SUCCESS && userResp.user.id == userCreateResp.user.id)
        } else {
            userResp.status == User.UserServiceType.USER_QUERY_STATUS_WRONG_PASS
        }
        where: "be able to test password, and be able to login by username|email|phone"
        user                                                  | userReq || isSuccess
        getUserInfo("c0",null,null,"Abcd1234")                | getUserInfo("c0",null,null,"Abcd1234") || true
        getUserInfo(null, null, null, null)                   | getUserInfo("c0", null, null, "Abcd12342")||false
        getUserInfo(null, "c2@test.com", null, "Abcd1234")    | getUserInfo(null, "c2@test.com", null, "Abcd1234") || true
        getUserInfo(null, null, null, null)                   | getUserInfo(null, "c2@test.com", null, "Abcd12342")|| false
        getUserInfo(null, null, "+3-13800138000", "Abcd1234") | getUserInfo(null, null, "+3-13800138000", "Abcd1234") || true
        getUserInfo(null, null, null, null)                   | getUserInfo(null, null, "+3-13800138000", "Abcd12342") || false
        getUserInfo(null, null, null, null) | getUserInfo("c0", "c0", "c0", "Abcd1234") || true
        getUserInfo(null, null, null, null) | getUserInfo("c0", "c0", "c0", "Abcd12324") || false
        getUserInfo(null, null, null, null) | getUserInfo("c2@test.com", "c2@test.com", "c2@test.com", "Abcd1234") || true
        getUserInfo(null, null, null, null) | getUserInfo("c2@test.com", "c2@test.com", "c2@test.com", "Abcd12324") || false
        getUserInfo(null, null, null, null) | getUserInfo("+3-13800138000", "+3-13800138000", "+3-13800138000", "Abcd1234") || true
        getUserInfo(null, null, null, null) | getUserInfo("+3-13800138000", "+3-13800138000", "+3-13800138000", "Abcd12324") || false
    }

    def "query users"() {
        given:
        when:
        for(int i = 0; i < 15; i++)
            blockingStub.invokeNewUser(User.UserInfo.newBuilder().build())
        User.UserQueryResp resp2 = blockingStub.invokeNewUser(User.UserInfo.newBuilder().build())
        for(int i = 0; i < 2; i++)
            blockingStub.invokeNewUser(User.UserInfo.newBuilder().build())
        User.UsersQueryResp resp = blockingStub.queryUsers(User.UsersQueryReq.newBuilder().setOffsetId(resp2.user.id).build())
        then:
        resp.status == User.UserServiceType.USER_QUERY_STATUS_SUCCESS
        resp.getUsersCount() == 2

        channel.shutdownNow()
    }

    def getUserInfo(String username, String email, String phone, String pass) {
        def user = User.UserInfo.newBuilder()
        if(username != null) user.setUsername(username)
        if(email != null) user.setEmail(email)
        if(phone != null) user.setPhone(phone)
        if(pass != null) user.setPassword(pass)
        return user.build()
    }

}