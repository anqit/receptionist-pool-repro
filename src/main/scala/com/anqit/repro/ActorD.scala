package com.anqit.repro

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, StashBuffer }

import java.time.Instant
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{ Failure, Success }

object ActorD {
    val serviceKey = ServiceKey[Command]("actor-d-key")

    def apply(
      maybeActorC: Option[ActorRef[ActorC.Command]] = None,
      interval: FiniteDuration = 1.hour,
    ): Behavior[Command] =
        Behaviors.withStash(100) { buffer =>
            Behaviors.setup { ctx =>
                ctx.system.receptionist ! Receptionist.Register(serviceKey, ctx.self)

                lazy val listingResponseAdapter = ctx.messageAdapter[Receptionist.Listing](ListingResponse)

                find(maybeActorC, interval, ctx, buffer, listingResponseAdapter)
            }
        }

    private def find(
      maybeActorC: Option[ActorRef[ActorC.Command]],
      interval: FiniteDuration,
      ctx: ActorContext[Command],
      buffer: StashBuffer[Command],
      listingResponseAdapter: => ActorRef[Receptionist.Listing],
    ): Behavior[Command] = maybeActorC match {
        case Some(actorC) =>
            buffer.unstashAll(ready(actorC, interval, ctx))
        case _ =>
            if (maybeActorC.isEmpty) ctx.system.receptionist ! Receptionist.Find(ActorCPool.poolServiceKey, listingResponseAdapter)

            Behaviors.receiveMessage {
                case ListingResponse(ActorCPool.poolServiceKey.Listing(listings)) =>
                    find(listings.headOption, interval, ctx, buffer, listingResponseAdapter)
                case other =>
                    buffer.stash(other)
                    Behaviors.same
            }
    }

    private def ready(
      actorC: ActorRef[ActorC.Command],
      interval: FiniteDuration,
      ctx: ActorContext[Command],
    ): Behavior[Command] = Behaviors.withTimers { timers =>
        timers.startTimerWithFixedDelay(BatchTimerKey, Ping(), interval)

        Behaviors.receiveMessage {
            case Ping() =>
                ctx.log.info(s"ping at ${ Instant.now() }")

                val eventualResult = Future.successful("an async operation")
                ctx.pipeToSelf(eventualResult) {
                    case Success(result) => ActorDCommand(result)
                    case Failure(exception) => Error(exception)
                }

                Behaviors.same

            case ActorDCommand(result) =>
                ctx.log.info(s"got a $result")
                actorC ! ActorC.ActorCCommand(result)

                Behaviors.same

            case Error(error) =>
                ctx.log.info(s"got an error: $error")

                Behaviors.same

        }
    }

    sealed trait Command

    case class ActorDCommand(result: String) extends Command
    case class Error(exception: Throwable) extends Command

    case class Ping() extends Command

    case class ListingResponse(listing: Receptionist.Listing) extends Command

    case object BatchTimerKey
}

object ActorDPool {

    import ActorD._

    val poolServiceKey = ServiceKey[Command]("actor-d-pool-key")

    def apply(): Behavior[Command] = ActorUtils.poolOf(serviceKey)(poolServiceKey)
}
