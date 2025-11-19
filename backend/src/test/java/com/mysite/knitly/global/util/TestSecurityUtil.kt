package com.mysite.knitly.global.util

import com.mysite.knitly.domain.user.entity.User
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.request.RequestPostProcessor

object TestSecurityUtil {

    fun createPrincipal(user: User): RequestPostProcessor? {
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val authentication: Authentication = UsernamePasswordAuthenticationToken(user, null, authorities)

        return authentication(authentication)
    }
}