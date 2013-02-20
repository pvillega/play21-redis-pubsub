package controllers


import play.api._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import libs.concurrent.Akka
import play.api.mvc._
import akka.util.{Timeout}
import java.util.concurrent.TimeUnit._
import java.lang.String
import akka.actor.{Props, Actor}
import concurrent.duration.Duration
import com.typesafe.plugin.RedisPlugin
import redis.clients.jedis.JedisPubSub
import concurrent.Future
import scala.concurrent.ExecutionContext


object Application extends Controller {


  def index = Action {
    // quick test of Redis as cache
    play.api.cache.Cache.set("mykey", "My value")
    val s = play.api.cache.Cache.getAs[String]("mykey")
    Logger.info("Value retrieved from Cache: %s".format(s))

    //show main page
    Ok(views.html.index("Your new application is ready."))
  }


  // relevant code below
  // to test launch 2 instances of the app (using -Dhttp.port=xxxx) and see them talking via Redis
  // using pub-sub

  private lazy val fakeStreamActor = Akka.system.actorOf(Props[FakeStreamActor])

  private val PLATFORM = Play.configuration.getInt("platform.number").getOrElse(1)

  val actorPut = Akka.system.scheduler.schedule(
    Duration(1000, MILLISECONDS),
    Duration(1000, MILLISECONDS),
    fakeStreamActor,
    Put(s"Sample message from $PLATFORM"))

}

//Note that subscribe is a blocking operation because it will poll Redis for responses on the thread that calls subscribe.
// A single JedisPubSub instance can be used to subscribe to multiple channels. You can call subscribe or psubscribe on
// an existing JedisPubSub instance to change your subscriptions.

/**
 * Fake actor to generate data
 */
class FakeStreamActor extends Actor {
  implicit val timeout = Timeout(1, SECONDS)

  val CHANNEL = "channel1"
  val plugin = Play.application.plugin(classOf[RedisPlugin]).get
  val listener = new MyListener()

  val pool = plugin.sedisPool

  // subscribe in a Future
  Future {
    // Some blocking or expensive code here
    pool.withJedisClient{ client =>
      client.subscribe(listener, CHANNEL)
    }
  }(Contexts.myExecutionContext)

  def receive = {

    case Put(msg: String) => {
      //send data to Redis
      Logger.info("Push %s".format(msg))
      pool.withJedisClient { client =>
        client.publish(CHANNEL, msg)
      }

    }
  }
}

// Execution context used to avoid blocking on subscribe
object Contexts {
  implicit val myExecutionContext: ExecutionContext = Akka.system.dispatchers.lookup("akka.actor.redis-pubsub-context")
}

/** Messages */
case class Put(msg: String)

/* Subscribe class*/
case class MyListener() extends JedisPubSub {
  def onMessage(channel: String, message: String): Unit = {
    Logger.info("onMessage[%s, %s]".format(channel, message))
  }

  def onSubscribe(channel: String, subscribedChannels: Int): Unit = {
    Logger.info("onSubscribe[%s, %d]".format(channel, subscribedChannels))
  }

  def onUnsubscribe(channel: String, subscribedChannels: Int): Unit = {
    Logger.info("onUnsubscribe[%s, %d]".format(channel, subscribedChannels))
  }

  def onPSubscribe(pattern: String, subscribedChannels: Int): Unit = {
    Logger.info("onPSubscribe[%s, %d]".format(pattern, subscribedChannels))
  }

  def onPUnsubscribe(pattern: String, subscribedChannels: Int): Unit = {
    Logger.info("onPUnsubscribe[%s, %d]".format(pattern, subscribedChannels))
  }

  def onPMessage(pattern: String, channel: String, message: String): Unit = {
    Logger.info("onPMessage[%s, %s, %s]".format(pattern, channel, message))
  }
}

