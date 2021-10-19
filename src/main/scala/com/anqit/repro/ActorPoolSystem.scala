package com.anqit.repro

import akka.actor.typed.{ ActorSystem, Behavior, SupervisorStrategy }
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.DeadLetter
import akka.actor.typed.eventstream.EventStream.Subscribe
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.DurationInt

object ActorPoolSystem extends App {
    ConfigFactory.load()

    val NumActorA: Int = 1 // 7
    val NumActorB: Int = 1 // 10
    val NumActorC: Int = 1 // 10
    val NumActorD: Int = 1

    def apply(): Behavior[Nothing] =
        Behaviors.setup[Nothing] { ctx =>
            val deadLetterListener = ctx.spawn(DeadLetterListener(), "dl-listener")
            ctx.system.eventStream ! Subscribe(deadLetterListener)

            (0 until NumActorA).foreach { i =>
                ctx.spawn(
                    Behaviors.supervise(ActorA()).onFailure(SupervisorStrategy.restart),
                    s"actor-a-$i",
                )
            }
            ctx.spawn(Behaviors.supervise(ActorAPool()).onFailure(SupervisorStrategy.restart), "actor-a-actor-pool")

            (0 until NumActorB).foreach { i =>
                ctx.spawn(
                    Behaviors.supervise(ActorB()).onFailure(SupervisorStrategy.restart),
                    s"actor-b-$i",
                )
            }
            ctx.spawn(Behaviors.supervise(ActorBPool()).onFailure(SupervisorStrategy.restart), "actor-b-actor-pool")

            (0 until NumActorC).foreach { i =>
                ctx.spawn(
                    Behaviors.supervise(ActorC()).onFailure(SupervisorStrategy.restart),
                    s"actor-c-$i",
                )
            }
            ctx.spawn(Behaviors.supervise(ActorCPool()).onFailure(SupervisorStrategy.restart), "actor-c-actor-pool")

            (0 until NumActorD).foreach { i =>
                ctx.spawn(
                    Behaviors.supervise(ActorD(interval = 1.minute)).onFailure(SupervisorStrategy.restart),
                    s"actor-d-$i",
                )
            }

            ctx.spawn(
                Behaviors.supervise(ActorDPool()).onFailure(SupervisorStrategy.restart),
                "actor-d-actor-pool",
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
