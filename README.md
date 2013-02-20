Using Redis Pub-Sub in Play 2.1
=====================================

I had some trouble using Redis PubSub with Play 2.1.0, so I decided to publish this in case someone else has the same issue.

The application is extremely simple. When launched it will subscribe to a channel in Redis, and an actor will send a message to that channel every second.
You will see the events triggered in the logs (both publication and subscribe-related events).

You can change the message slightly by providing a new value for the *platform.number* configuration.

You can launch multiple instances of the app using the JVM parameters:

    -Dhttp.port=<new_port> -DPLATFORM_ENV=<new_int>

to see multiple instances connecting to the same channel and sharing messages as expected.

## Requirements
* Play 2.1.0
* [play-plugins-redis](https://github.com/typesafehub/play-plugins/tree/master/redis) from Typesafe
* A running Redis instance (the app uses default parameters from the plugin, so it assumes Redis located at localhost:6379 and no credentials to connect)

## Code explanation

**WARNING: rough code ahead**

The code is not nice, but it works as a demo. `Application.scala` defines an Actor with an action scheduled every 1 second (to send a message).

The actor obtains a connection pool to Redis and launches a `subscribe` operation in a separate `ExecutionContext` (more about it below). The action triggered every second will send a message to that channel.

There is a support class used when subscribing that receives events when something is published in the relevant channel.

All relevant events and operations output something in the logs.


## Gotchas

* `Build.scala` has to include an additional resolver to load all the dependencies for Sedis (Redis library for Scala)
* `Subscribe` is a blocking operation. If you do it without taking this into account, you'll kill the server. The proper way is to use a `Future` with its own `ExecutionContext` (as can be seen in the [documentation](http://www.playframework.com/documentation/2.1.0/ThreadPools)) to launch this operation.

Example of `ExecutionContext` usage:

    // subscribe in a Future using a specific ExecutionContext    
    Future {
      // use Sedis pool to launch the subscribe operation
      pool.withJedisClient{ client =>
           client.subscribe(listener, CHANNEL)
      }
    }(Contexts.myExecutionContext)


