package com.hiz.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.datetime
import org.ktorm.schema.uuid
import org.ktorm.schema.varchar
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@JvmInline
value class UserId(val value: UUID)

data class User(
    var id: UserId,
    var firstName: String,
    var lastName: String,
    var email: String,
    var password: String,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
) {
    fun updatePassword(newPassword: String) {
        password = BCrypt.withDefaults().hashToString(14, newPassword.toCharArray())
    }

    fun checkPassword(otherPassword: String): Boolean {
        return BCrypt.verifyer().verify(otherPassword.toCharArray(), password.toCharArray()).verified
    }
}

interface UserEntity : Entity<UserEntity> {
    companion object : Entity.Factory<UserEntity>()

    var id: UUID
    var firstName: String
    var lastName: String
    var email: String
    var password: String
    var createdAt: Instant
    var updatedAt: Instant

    fun toModel(): User {
        return User(
            id = UserId(id),
            firstName = firstName,
            lastName = lastName,
            email = email,
            password = password,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun fromModel(user: User) {
        id = user.id.value
        firstName = user.firstName
        lastName = user.lastName
        email = user.email
        password = user.password
        updatedAt = user.updatedAt
        createdAt = user.createdAt
    }
}


object UserTable : Table<UserEntity>("users") {
    val id = uuid("id").primaryKey().bindTo { it.id }
    val firstName = varchar("first_name").bindTo { it.firstName }
    val lastName = varchar("last_name").bindTo { it.lastName }
    val email = varchar("email").bindTo { it.email }
    val password = varchar("password").bindTo { it.password }
    var createdAt = datetime("created_at").transform({ it.toInstant(ZoneOffset.UTC) },
        { LocalDateTime.ofInstant(it, ZoneOffset.UTC) }).bindTo { it.createdAt }
    var updatedAt = datetime("updated_at").transform({ it.toInstant(ZoneOffset.UTC) },
        { LocalDateTime.ofInstant(it, ZoneOffset.UTC) }).bindTo { it.updatedAt }
}

val Database.users get() = this.sequenceOf(UserTable)