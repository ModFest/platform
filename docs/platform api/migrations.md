# Migrations
HELLO THERE! Do YOU want to write a migrations? You've come to the right place! Here are your steps!!

1. Head on over to the `Migrator class`
2. Increment `CURRENT_VERSION`
3. Write a new `migrateToN` functions
4. Add your `migrateToN` function into the static block
5. Add a javadoc to your `migrateToN` function explaining what changed. You can use the
other functions for reference

Migrations should be as stand-alone as possible. You should aim to not rely on much code outside the `migrations`
package. Reason being that that code might change and break older migrations. Especially avoid (de)serializing
things into pojo's (unless those pojo's were written specifically for that migration). 
