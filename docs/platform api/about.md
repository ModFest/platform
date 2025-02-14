# Platform API

This is the project handling our user data, as well any business logic surrounding the data, and exposes them using
rest apis.

The platform API is coded using Spring Boot. It's recommended to know a bit about Spring before looking too deep into
the platform api. There's a small introduction to spring [here](./spring.md).

## Project structure
The platform api has the following packages:

* `configuration`: Has beans containing configuration, as well as pre-configured objects. Could honestly contain
  any bean which is set up once and used globally.
* `controller`: These classes are responsible for defining http routes, any logic related to interpreting web requests,
  and any permission checks on the requests. (Note that spring already handles (de)serializing object to json). The
  controller shouldn't be doing any of the logic of handling the request, it should defer that to a service.
* `git`: Contains a wrapper for filesystem modifications. It adds the modifications to git
* `migrations`: Contains code for migrating our data from one version to another. For example, we once added a "role"
  field, so we added a migration that would give that field a default value inside all our json files
* `misc`: What it says on the tin, anything that doesn't fit inside other packages
* `repository`: This manages our heaps of json. Each repository handles a data type, and abstracts away the reading/
  writing of that datatype to/from disk. Repositories can also do some validation that the data written to disk is correct
* `security`: All things related authenticating the user
* `service`: Any business logic is contained here. It contains any operations we might want to do on our data.

## Swagger UI
The platform api exposes a Swagger UI. By default, it's located at `/swagger-ui.html`. The swagger ui is automatically
generated based on the code.
