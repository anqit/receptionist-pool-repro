package com.anqit.repro

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.actor.typed.scaladsl.Behaviors

object ActorA {
    sealed trait Command
    case class ActorACommand(result: String)
      extends Command

    val serviceKey = ServiceKey[Command]("actor-a-key")

    def apply(): Behavior[Command] = Behaviors.setup { ctx =>
        ctx.system.receptionist ! Receptionist.Register(serviceKey, ctx.self)

        Behaviors.receiveMessage {
            case ActorACommand(result) =>
                println(s"actor a received result $result")

                Behaviors.same
        }
    }
}

object ActorAPool {
    import ActorA._

    val poolServiceKey = ServiceKey[Command]("actor-a-pool-key")

    def apply(): Behavior[Command] = ActorUtils.poolOf(serviceKey)(poolServiceKey)
}
