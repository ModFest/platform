package net.modfest.botfest.extensions

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.event.role.RoleCreateEvent
import dev.kord.core.event.role.RoleDeleteEvent
import dev.kord.core.event.role.RoleUpdateEvent
import dev.kord.rest.request.KtorRequestException
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.event
import dev.kordex.core.i18n.withContext
import dev.kordex.core.koin.KordExKoinComponent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.modfest.botfest.MAIN_GUILD_ID
import net.modfest.botfest.Platform
import net.modfest.botfest.REGISTERED_ROLE
import net.modfest.botfest.i18n.Translations
import nl.theepicblock.sseclient.SseClient
import org.koin.core.component.inject
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Duration
import java.util.HashSet
import java.util.function.Predicate

const val TABLE_NAME = "members"

/**
 * Ensures that all guild members have the correct roles.
 *
 * It maintains a local database of all known members and their roles.
 */
class RoleManager : Extension(), KordExKoinComponent {
	override val name = "roles"
	private val logger = KotlinLogging.logger("Role Manager")
	private val platform: Platform by inject()
	private var userChangeListener: SseClient? = null

	private val database: Connection by lazy {
		// `./data` should be persisted! KordEx stores some other files in there
		var conn = DriverManager.getConnection("jdbc:sqlite:./data/roles_${MAIN_GUILD_ID}.sqlite")

		// Ensure the table exists
		conn.createStatement().use {
			it.executeUpdate("""
				CREATE TABLE IF NOT EXISTS $TABLE_NAME (
					member INTEGER PRIMARY KEY
				) STRICT;
			""".trimIndent())
		}

		return@lazy conn;
	}

	/**
	 * All known roles in the guild
	 */
	private var roles: List<Snowflake>? = null

	override suspend fun setup() {
		// Ensure that the roles are checked on application startup
		// this will also initialize some fields
		fetchRoles()

		event<RoleCreateEvent> {
			action {
				onGuildRoleUpdate(event.guild)
			}
		}
		event<RoleUpdateEvent> {
			action {
				onGuildRoleUpdate(event.guild)
			}
		}
		event<RoleDeleteEvent> {
			action {
				onGuildRoleUpdate(event.guild)
			}
		}

		userChangeListener = platform.authenticatedAsBotFest().userChanges(
			onEvent = lis@{
				val userId = it.data ?: return@lis
				runBlocking {
					launch(Dispatchers.Default) {
						val userData = platform.getUser(userId)!!
						if (userData.discordId != null) {
							fixUser(Snowflake(userData.discordId!!))
						}
					}
				}
			},
			onConnect = {
				logger.info { "Connected to Platform, listening for changes" }

			}
		)

		ephemeralSlashCommand {
			name = Translations.Commands.Fix.name
			description = Translations.Commands.Fix.description

			guild(MAIN_GUILD_ID)

			action {
				try {
					// Resync roles from discord
					fetchMembers(setOf(this.user.id), force = true)
					// Give correct roles
					fixUser(this.user.id)
					// Respond back to the user with a message
					respond {
						content = Translations.Commands.Fix.Response.success
							.withContext(this@action)
							.translateNamed()
					}
				} catch (e: SQLException) {
					respond {
						content = Translations.Commands.Fix.Response.error
							.withContext(this@action)
							.translateNamed(
								"error" to e.message
							)
					}
				} catch (e: KtorRequestException) {
					if (e.message?.contains("Missing Permissions null") == true) {
						respond {
							content = Translations.Commands.Fix.Response.permission
								.withContext(this@action)
								.translateNamed()
						}
					} else {
						throw e
					}
				}
			}
		}
	}

	override suspend fun unload() {
		database.close()
	}

	suspend fun onGuildRoleUpdate(guild: GuildBehavior) {
		if (guild.id == MAIN_GUILD_ID) {
			fetchRoles()
		}
	}

	/**
	 * We maintain a database of all members that have roles. This means that if the set
	 * of roles on the guild changes, we need to change our database as well. This
	 * function will ensure that our database has a single column for each role in the guild.
	 * The name of these columns will be the role's id (numbers are valid sql column names).
	 */
	private suspend fun fetchRoles() {
		// Retrieve the names of the columns currently in the database
		val columns = database.metaData.getColumns(null, null, TABLE_NAME, null)
		val columnNames = HashSet<String>()
		while (columns.next()) {
			columnNames.add(columns.getString("COLUMN_NAME"))
		}

		// This is the key used to store the user's snowflake.
		// It isn't a role
		columnNames.remove("member")

		// Retrieve the roles from the guild
		val roles = ArrayList<Snowflake>()
		val mainGuild = kord.getGuild(MAIN_GUILD_ID)
		mainGuild.roles.map { r -> r.id }.toList(roles)
		roles.remove(mainGuild.everyoneRole.id)

		val changes = diff(columnNames, roles.map { it.toString() }.toSet())

		// Apply changes
		database.createStatement().use { stmnt ->
			changes.toRemove.forEach {
				logger.info { "Removing column `$it` for guild $MAIN_GUILD_ID, as the role appears to no longer exist" }
				stmnt.executeUpdate("ALTER TABLE $TABLE_NAME DROP COLUMN `$it`;")
			}
			changes.toAdd.forEach {
				logger.info { "Adding column `$it` for guild $MAIN_GUILD_ID" }
				stmnt.executeUpdate("ALTER TABLE $TABLE_NAME ADD COLUMN `$it` INTEGER;")
			}
		}

		this.roles = roles
	}

	private suspend fun fetchMembers(members: Set<Snowflake>, force: Boolean = false) {
		val guild = kord.getGuild(MAIN_GUILD_ID)
		// Iterate asynchronously
		members.asFlow().flowOn(Dispatchers.IO).collect { member ->
			val currentRoles = if (force) {
				kord.rest.guild.getGuildMember(MAIN_GUILD_ID, member).roles.toSet()
			} else {
				guild.getMember(member).roleIds
			}
			database.createStatement().use { stmnt ->
				if (currentRoles.isEmpty()) {
					stmnt.executeUpdate("""
						DELETE FROM $TABLE_NAME WHERE member = ${member.value.toLong()}
					""".trimIndent())
				} else {
					val sqlroles = roles!!.map { "`$it`" }.joinToString(",")
					val values = roles!!.map { if (currentRoles.contains(it)) 1 else 0 }.joinToString(",")

					stmnt.executeUpdate("""
						INSERT OR REPLACE INTO $TABLE_NAME (member, $sqlroles)
							VALUES (${member.value.toLong()}, $values)
					""".trimIndent())
				}
			}
		}
	}

	private fun getCachedRoles(user: Snowflake): Set<Snowflake> {
		database.createStatement().use { stmnt ->
			val query = roles!!.map { "`$it`" }.joinToString(",")
			var res = stmnt.executeQuery("""
				SELECT $query FROM $TABLE_NAME WHERE member = ${user.value.toLong()}
			""".trimIndent())
			val set = HashSet<Snowflake>()
			for (i in 0..<roles!!.size) {
				if (res.getInt(i+1) >= 1) {
					set.add(roles!![i])
				}
			}
			return set
		}
	}

	suspend fun fixUser(user: Snowflake) {
		var cache = getCachedRoles(user)
		var expected = expectedRoles(user)
		var managed = managedRoles()
		var roleDiff = diff(cache, expected)

		roleDiff.removeIf { !managed.contains(it) } // Only work on managed roles
		roleDiff.removeIf { !roles!!.contains(it) } // Only work on roles that we know actually exist

		roleDiff.toAdd.forEach { role ->
			logger.info { "Adding `$role` to $user (in $MAIN_GUILD_ID)" }
			kord.rest.guild.addRoleToGuildMember(MAIN_GUILD_ID, user, role)
		}
		roleDiff.toRemove.forEach { role ->
			logger.info { "Removing `$role` from $user (in $MAIN_GUILD_ID)" }
			kord.rest.guild.deleteRoleFromGuildMember(MAIN_GUILD_ID, user, role)
		}

		// Put the new data into db
		fetchMembers(setOf(user), force = true)
	}

	/**
	 * Returns all roles that can be managed by platform
	 */
	suspend fun managedRoles(): Set<Snowflake> {
		val roles = HashSet<Snowflake>()
		roles.add(REGISTERED_ROLE)
		platform.getEvents().forEach { event ->
			roles.add(Snowflake(event.discordRoles.participant))
			roles.add(Snowflake(event.discordRoles.award))
		}
		// TODO caching
		return roles
	}

	/**
	 * The roles a user *should* have.
	 */
	suspend fun expectedRoles(user: Snowflake): Set<Snowflake> {
		val roles = HashSet<Snowflake>()
		var platformUser = platform.getUser(user)

		if (platformUser == null) {
			// Not registered, no roles
			return roles
		}

		roles.add(REGISTERED_ROLE)
		platformUser.registered.forEach { event ->
			val eventRoles = platform.getEvent(event).discordRoles
			roles.add(Snowflake(eventRoles.participant))
		}
		return roles
	}

	/**
	 * Computes the set of changes needed to go from the
	 * `current` collection to the `target` collection
	 */
	private fun <T> diff(current: Collection<T>, target: Collection<T>): DiffResult<T> {
		val toRemove = HashSet<T>()
		val toAdd = HashSet<T>()

		current.forEach {
			if (!target.contains(it)) {
				toRemove.add(it)
			}
		}
		target.forEach {
			if (!current.contains(it)) {
				toAdd.add(it)
			}
		}

		return DiffResult(toRemove, toAdd)
	}

	class DiffResult<T>(val toRemove: MutableCollection<T>, val toAdd: MutableCollection<T>) {
		fun isEmpty(): Boolean {
			return toRemove.isEmpty() && toAdd.isEmpty()
		}

		fun removeIf(function: Predicate<T>) {
			toAdd.removeIf(function)
			toRemove.removeIf(function)
		}
	}
}
