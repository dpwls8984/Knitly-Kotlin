package com.mysite.knitly.domain.community.config

import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.service.UserService
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component

@Component
class CurrentUserResolver(
    private val userService: UserService
) {

    fun getCurrentUserOrNull(): User? {
        val auth: Authentication = SecurityContextHolder.getContext().authentication ?: return null
        if (!auth.isAuthenticated) return null

        val principal = auth.principal

        if (principal is User) return principal

        if (principal is UserDetails) {
            val email = principal.username
            return findByEmailIfExists(email)
        }

        if (principal is String) {
            if (principal.equals("anonymousUser", ignoreCase = true)) return null
            return findByEmailIfExists(principal)
        }

        return null
    }

    private fun findByEmailIfExists(email: String): User? {
        return try {
            val method = UserService::class.java.getMethod("findByEmail", String::class.java)
            val result = method.invoke(userService, email)
            if (result is User) result else null
        } catch (e: Exception) {
            null
        }
    }
}
