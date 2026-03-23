# Security

## Permissions
Associated with each request is a set of permissions. These are permissions such as `users.view_mc`.
The list of permissions are defined in the [`Permissions.java`](https://github.com/ModFest/platform/blob/prod/platform_api/src/main/java/net/modfest/platform/security/Permissions.java) class, which also includes descriptions of what each route
does. What permissions someone has depends on their group, defined in [`PermissionGroup.java`](https://github.com/ModFest/platform/blob/prod/platform_api/src/main/java/net/modfest/platform/security/PermissionGroup.java). Requests are associated
with `UNPRIVILEGED_USERS` by default. Some groups inherit the permissions of other groups.

You can view the permissions associated with a request using the `/meta/me` route. Or you can use the `/debug whoami`
command inside of discord.

## Authenticating

You can authenticate with platform in multiple ways.

### BotFest Token
There's a shared token between BotFest and Platform. When BotFest wants to authenticate it provides this token
through the `BotFest-Secret` header. It also provides a discord id through `BotFest-Target-User`. As long as the
secret matches, the request will be associated with that user. *This means anyone with the BotFest token can impersonate any other user*. 

BotFest can also set `BotFest-Target-User` to `@self` in which case the request is associated with BotFest itself, with
the `BOTFEST` permission group.

In development, it might be handy to use this method of authentication for youself. In dev environments the shared token is set in the `gradle.properties`, with it defaulting to `1234abcd5678`. You can use it like so:

```sh
curl -H "BotFest-Secret: 1234abcd5678" -H "BotFest-Target-User: ..." localhost:8070/meta/me
```

# Modrinth token

Any modrinth token can be provided using the `Modrinth-Token` header. The token only needs permission to read the user's
data (not email). When provided, the request will have the permissions of the modfest user associated with that modrinth
account.

# Event token

Specific tokens are generated per event, and are used for syncing the minecraft whitelist. The tokens can be managed via
the panel

## Internal implementation
Security is implemented using Apache Shiro. It's a terrible way of doing things but it works now and is better than
any other framework available for Spring. `SecurityConfig.java` adds a bunch of filters to Shiro. These filters
intercept the various headers and then associate a token object with the request. They don't do any checks for validity,
that is done in `ModFestRealm.java`. There the tokens are read, validated, and then a particular "principal" is associated
with the request. Usually this is a user object, taken straight from the database. `ModFestRealm.java` also contains
the code to associate a permission group with such an "principal".

Then various routes will use the `@RequiresPermissions` annotation or the `SecurityUtils.getSubject().isPermitted` function
to check if the user has a specific permission. The "principal" can also be directly accessed through `SecurityUtils.getSubject().getPrincipal()`. If you use this method you need to deal with the fact that the request might be associated with something other than a user. For example when an event token is used, then the principal is that event. Or if BotFest logs in as itself, then the principal is a singleton object which represents BotFest. The `PermissionUtils.java` has some utils for the most common case (checking if the principal is one of the authors listed on a submission)
