# Spring
The entire platform api subproject is written using Spring. This is a quick modfest-specific introduction to
Spring to hopefully get people up to speed. Spring is very enterprise™ java. But it's also pretty well-used, so
you should be able to find a lot of resources online.

## Beans
Spring uses the concept of beans, which is an incredibly silly name. Kudos to whoever chose that name and got
enterprise™ coders to actually use it. You can think of beans as being components. For example, there's a `UserService`
class. This class is marked as a bean. This means that spring will pick up on it, and create a singleton instance of it.
Now beans can depend on other beans, for example, the `UserController` depends on the `UserService` bean. So spring will
manage creating a `UserService` first, and then giving `UserController` an instance of the service.

!!! note "Wait, are they always singletons?"
	Nah, you can [configure spring](https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html) to
	do singletons, or give each dependent a new instance, or even to create a new instance for each web request (god
	knows how that's done internally)

## Annotations
Do you like annotations? Do you like it when the magic annotations change how the code works? I presume the answer to
those questions was "yes!", and you're in luck! Spring uses a whole load of annotations. They also have some kind of way
of using XML files instead, but that seems even worse. Please don't ever bring xml into this forsaken repository.

Annotations are the primary way of creating beans. For example, `@Service` and `@RestController` both define a class to be
a bean. You might be wondering why both of those make beans. Well, there's some kind of annotation inheritance at play:
both of those annotations imply `@Component`, and that's what's actually making it into a bean. You can see this for
yourself, the `@Service` annotation is annotated with `@Component`, and the `@RestController` annotation is annotated with
`@Controller` which in turn has `@Component`. Either way, they both end up being beans. There's one other important way
of defining beans, which is with `@Bean`. This annotation is actually used on a getter function, instead of a class.
This means that anything you return from the function will become available as a bean. For example, platform uses this in the
`GsonConfig` class to expose a `Gson` object as a bean. This is not for fun: we configure spring to use gson for
(de)serializing http requests. Spring then ends up using the gson object exposed by our `GsonConfig` to do the
(de)serialization. Spring is basically using the bean system here to allow us to configure gson for our needs (adding
custom type serializers, etc). You'll find that there are a few other places where spring does this.

!!! note "Wait, how is spring configured to use gson?"
	Yeah, spring uses lombok by default, but by setting `spring.http.converters.preferred-json-mapper` and
	`spring.mvc.converters.preferred-json-mapper` inside `application.properties` we can override that.
