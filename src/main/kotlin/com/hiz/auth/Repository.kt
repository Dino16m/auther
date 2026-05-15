package com.hiz.auth

import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.ktorm.entity.update
import java.time.Instant
import java.util.*


interface UserRepository {
    fun add(user: User)
    fun save(user: User): User
    fun findById(id: UUID): User?
    fun findByEmail(email: String): User?
}

class InMemoryUserRepository : UserRepository {
    private val store: MutableMap<UserId, User> = mutableMapOf()
    override fun add(user: User) {
        save(user)
    }

    override fun save(user: User): User {
        store[user.id] = user
        return user
    }

    override fun findById(id: UUID): User? {
        return store[UserId(id)]
    }

    override fun findByEmail(email: String): User? {
        return store.values.find { it.email == email }
    }
}

class DBUserRepository(private val database: Database) : UserRepository {
    override fun add(user: User) {
        val entity = UserEntity {
            fromModel(user)
        }
        database.users.add(entity)
    }

    override fun save(user: User): User {
        val entity = UserEntity {
            fromModel(user)
            updatedAt = Instant.now()
        }
        database.users.update(entity)
        return entity.toModel()
    }

    override fun findById(id: UUID): User? {
        val entity = database.users.find { it.id eq id } ?: return null
        return entity.toModel()
    }

    override fun findByEmail(email: String): User? {
        val entity = database.users.find { it.email eq email } ?: return null
        return entity.toModel()
    }

}