package com.anqit.repro

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, StashBuffer }

object ActorC {
    val serviceKey: ServiceKey[Command] = ServiceKey[Command]("actor-c-key")

    def apply(
      maybeActorA: Option[ActorRef[ActorA.Command]] = None,
      maybeActorB: Option[ActorRef[ActorB.Command]] = None,
    ): Behavior[Command] = Behaviors.withStash(100) { buffer =>
        Behaviors.setup { ctx =>
            ctx.system.receptionist ! Receptionist.Register(serviceKey, ctx.self)

            lazy val listingResponseAdapter = ctx.messageAdapter[Receptionist.Listing](ListingResponse)

            find(maybeActorA, maybeActorB, ctx, buffer, listingResponseAdapter)
        }
    }

    private def find(
      maybeActorA: Option[ActorRef[ActorA.Command]],
      maybeActorB: Option[ActorRef[ActorB.Command]],
      ctx: ActorContext[Command],
      buffer: StashBuffer[Command],
      listingResponseAdapter: => ActorRef[Receptionist.Listing],
    ): Behavior[Command] = (maybeActorA, maybeActorB) match {
        case (Some(actorA), Some(actorB)) =>
            buffer.unstashAll(ready(actorA, actorB, ctx))
        case _ =>
            if (maybeActorA.isEmpty) ctx.system.receptionist ! Receptionist.Find(ActorAPool.poolServiceKey, listingResponseAdapter)
            if (maybeActorB.isEmpty) ctx.system.receptionist ! Receptionist.Find(ActorBPool.poolServiceKey, listingResponseAdapter)

            Behaviors.receiveMessage {
                case ListingResponse(ActorAPool.poolServiceKey.Listing(listings)) =>
                    find(listings.headOption, maybeActorB, ctx, buffer, listingResponseAdapter)
                case ListingResponse(ActorBPool.poolServiceKey.Listing(listings)) =>
                    find(maybeActorA, listings.headOption, ctx, buffer, listingResponseAdapter)
                case other =>
                    buffer.stash(other)
                    Behaviors.same
            }
    }

    private def ready(
      actorA: ActorRef[ActorA.Command],
      actorB: ActorRef[ActorB.Command],
      ctx: ActorContext[Command],
    ): Behavior[Command] = Behaviors.receiveMessage {
        case ActorCCommand(result) =>
            ctx.log.info(s"actor c received $result")

            actorA ! ActorA.ActorACommand(result)
            actorB ! ActorB.ActorBCommand(result)
            Behaviors.same
    }

    sealed trait Command

    case class ActorCCommand(result: String) extends Command

    case class ListingResponse(listing: Receptionist.Listing) extends Command
}

object ActorCPool {

    import ActorC._

    val poolServiceKey: ServiceKey[Command] = ServiceKey[Command]("actor-c-pool-key")

    def apply(): Behavior[Command] = ActorUtils.poolOf(serviceKey)(poolServiceKey)
}
