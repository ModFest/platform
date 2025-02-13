# Role Management
For ModFest, we assign various discord roles based on platform data. For example, anyone who is registered in the
platform has the `registered` role. When a user is registered for an event, they get that event's role. In the
future, BotFest might also assign the roles for theme awards.  All logic related to
role syncing is contained inside of `RoleManager.kt`

The role management code is based upon the principle that, given a discord user, we can compute which roles this user
*should* have. This is implemented in the `expectedRoles` function. Not all roles are managed automatically, this is why
the `managedRoles` function exists. Any roles returned by this function are considered fair-game for BotFest to assign
and remove. If you want to modify which roles BotFest assigns, you will probably be editing these two functions.

## The internals
The rest of the role management code consist of the logic to diff roles and detect when a user needs updating.
This is complicated by the way discord works: we can't query users with a certain role. Nor can we
easily enumerate the entire user list. For this purpose, BotFest maintains a local cache. This is a sqlite database
containing all users that have a role. The cache allows us to quickly go through all the users registered to
platform and check if they have the right roles. The danger is that this cache can be out of date.

We assume that users have no roles by default,
so if a user is supposed to have a role but doesn't, this will likely get detected. If a user has a role which they
shouldn't have, this is much more unlikely to ever be found. Just in case, there's a `/fixmyroles` command which
will force BotFest to resync the cache and diff the roles with what is expected.

### Database layout
The database consists of a column storing the user's discord snowflake. This is the primary key used for indexing.
Furthermore, the database has a column for each role which exists in the discord server. These are added and removed
dynamically whenever roles get created and removed. These columns are of type boolean (well, sqlite actually treats
booleans as integers. In fact, sqlite isn't strictly typed at all. But just pretend they're booleans). Whenever a
user has no roles, they ought to be removed from the database. We assume that anyone not in the database doesn't have
any roles.
