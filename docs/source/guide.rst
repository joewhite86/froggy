Guide
=====

Quickstart
----------

First, add the dependency to your project.

Maven:

.. code-block:: xml

  <dependency>
      <groupId>de.whitefrog</groupId>
      <artifactId>frogr-base</artifactId>
      <version>0.0.3-SNAPSHOT</version>
  </dependency>

Next we will create the main entry point for the service.

.. code-block:: java

  public class Application extends de.whitefrog.frogr.Application<Configuration> {
    public Application() {
      // register the rest classes
      register("de.whitefrog.frogr.example.rest");
      // register repositories and models
      serviceInjector().service().register("de.whitefrog.frogr.example");
        tx.success();
      }
    }
    
    @Override
    public String getName() {
      return "example-rest";
    }

    public static void main(String[] args) throws Exception {
      new Application().run("server", "config/example.yml");
    }
  }

As you can see there are two registry calls in the application's constructor.
``register(...)`` let's the application know in which package to look for rest classes.
``serviceInjector().service().register(...)`` tells the application where to look for models and repositories.
More information about the Application entry point: :doc:`Application <documentation/application>`

You may also have noticed there's a config file used in the main method.
This is required to setup our Dropwizard_ instance, so we have to create that one now. 
There's a second config file needed, which configures our embedded Neo4j_ instance.
By default these configs should be in your project in a directory 'config'.

``config/example.yml``

.. code-block:: yaml

  server:
      applicationConnectors:
        - type: http
          port: 8282
      adminConnectors:
        - type: http
          port: 8286
      requestLog:
          appenders:
            - type: file
              currentLogFilename: logs/access.log
              threshold: ALL
              archive: false
  logging:
      level: WARN
      appenders:
        # console logging
        - type: console
          logFormat: '[%d] [%-5level] %logger{36} - %msg%n'

Reference: `Dropwizard Configuration`_

``config/neo4j.properties``

.. code-block:: properties

  graph.location=graph.db

Reference: `Neo4j Configuration`_

We should add a class that holds our relationship types, so that we have consistent and convienient access.
This is not a requirement but I highly recommend it.

.. code-block:: java

  public abstract class RelationshipTypes {
    public static final String ChildOf = "ChildOf";
    public static final String MarriedWith = "MarriedWith";

    public enum t implements RelationshipType {
      ChildOf, MarriedWith
    }
  }

Now, let's create a :doc:`model <documentation/models>`. I recommend using Kotlin_ for that.
All models have to extend the Entity class or implement the Model interface at least.

.. code-block:: kotlin

  class Person() : Entity() {
    constructor(name: String) : this() {
      this.name = name
    }
    
    // Unique and required property
    @Unique
    @Indexed
    @Required
    var name: String? = null

    // Relationship to another single model
    @RelatedTo(type = RelationshipTypes.MarriedWith, direction = Direction.BOTH)
    var marriedWith: Person? = null
    // Relationship to a collection of models
    @RelatedTo(type = RelationshipTypes.ChildOf, direction = Direction.OUTGOING)
    var parents: List<Person> = ArrayList()
    @RelatedTo(type = RelationshipTypes.ChildOf, direction = Direction.INCOMING)
    var children: List<Person> = ArrayList()
  }

As you can see, we used the relationship types created before, to declare our relationships to other models.

Normally we would create a repository for persons. But we won't need extra methods for
this tutorial and frogr will create a default repository if it can't find one.
If you need more information visit :doc:`documentation/repositories`.

Next we'll have to create the REST :doc:`service <documentation/services>` layer. There's a base class, that provides
basic CRUD operations, so you only have to add methods for special cases. Of course you
can also use any other JAX-RS annotated class.

.. code-block:: java

  @Path("persons")
  public class Persons extends CRUDService<PersonRepository, Person> {
  }

.. _Kotlin: https://kotlinlang.org
.. _Dropwizard: http://www.dropwizard.io
.. _`Dropwizard Configuration`: http://www.dropwizard.io/0.7.1/docs/manual/configuration.html
.. _Neo4j: http://neo4j.com
.. _`Neo4j Configuration`: https://neo4j.com/docs/operations-manual/3.3/configuration/
