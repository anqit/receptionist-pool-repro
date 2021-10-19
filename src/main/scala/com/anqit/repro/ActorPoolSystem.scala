package com.anqit.repro

import akka.actor.typed.{ ActorSystem, Behavior, SupervisorStrategy }
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.DeadLetter
import akka.actor.typed.eventstream.EventStream.Subscribe
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.DurationInt

object ActorPoolSystem extends App {
    ConfigFactory.load()

    def apply(): Behavior[Nothing] =
        Behaviors.setup[Nothing] { ctx =>
            val deadLetterListener = ctx.spawn(DeadLetterListener(), "dl-listener")
            ctx.system.eventStream ! Subscribe(deadLetterListener)

            val actorA = ctx.spawn(
                Behaviors.supervise(ActorA()).onFailure(SupervisorStrategy.restart),
                "actor-a",
            )

            val actorB = ctx.spawn(
                Behaviors.supervise(ActorB()).onFailure(SupervisorStrategy.restart),
                "actor-b",
            )

            val actorC = ctx.spawn(
                Behaviors.supervise(ActorC(Some(actorA), Some(actorB))).onFailure(SupervisorStrategy.restart),
                "actor-c",
            )

            ctx.spawn(
                Behaviors.supervise(ActorD(Some(actorC), interval = 5.seconds)).onFailure(SupervisorStrategy.restart),
                "actor-d",
            )

            Behaviors.empty
        }

    implicit val system = ActorSystem[Nothing](
        ActorPoolSystem(),
        "actor-pool-system",
    )
}

object DeadLetterListener {
    def apply(): Behavior[DeadLetter] = Behaviors.receiveMessage {
        case d: DeadLetter =>
            println(d)
            Behaviors.same
    }
}
