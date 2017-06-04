package com.ramsrib.test

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.hibernate.AbstractDAO
import io.dropwizard.hibernate.HibernateBundle
import io.dropwizard.hibernate.UnitOfWork
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.hibernate.SessionFactory
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType


fun main(args: Array<String>) {
    App().run(*args)
}


class App : Application<AppConfig>() {

    val hibernate = object : HibernateBundle<AppConfig>(User::class.java) {
        override fun getDataSourceFactory(configuration: AppConfig): DataSourceFactory {
            return configuration.database
        }
    }

    override fun run(config: AppConfig, env: Environment) {

        val sessionFactory = hibernate.sessionFactory
        val userDAO = UserDAO(sessionFactory)
        env.jersey().register(UserResource(userDAO))

//        println("Querying the user table...")
//        println(userDAO.fetchAll())

        val userAction = UnitOfWorkAwareProxyFactory(hibernate).create(UserAction::class.java, SessionFactory::class.java, sessionFactory)
        userAction.fetchUsers()

    }

    override fun initialize(bootstrap: Bootstrap<AppConfig>) {
        bootstrap.addBundle(hibernate)
    }

}

class AppConfig() : Configuration() {
    @Valid @NotNull @JsonProperty("database") val database: DataSourceFactory = DataSourceFactory()
}


class UserDAO(sessionFactory: SessionFactory) : AbstractDAO<User>(sessionFactory) {
    fun fetchAll(): List<User> {
        return query("SELECT u FROM User u").list()
    }
}


@Entity data class User(@Id var id: UUID, var username: String, var password: String)


@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
open class UserResource(val userDAO: UserDAO) {
    @GET @UnitOfWork fun getAllUsers(): List<User> {
        return userDAO.fetchAll()
    }
}

// Note: Opening the class & function is mandatory to make the proxy to work (UnitOfWorkAwareProxyFactory)
open class UserAction(val sessionFactory: SessionFactory) {
    @UnitOfWork open fun fetchUsers() {
        println("Fetching the users list...")
        val userDAO = UserDAO(sessionFactory)
        userDAO.fetchAll()
        println("ran fetch user")
    }
}
