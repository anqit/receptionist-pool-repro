package com.anqit.repro

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.actor.typed.scaladsl.Behaviors

object ActorB {
    val serviceKey = ServiceKey[Command]("actor-b-key")

    def apply(): Behavior[Command] = Behaviors.setup { ctx =>
        ctx.system.receptionist ! Receptionist.Register(serviceKey, ctx.self)

        Behaviors.receiveMessage {
            case ActorBCommand(result) =>
                println(s"actor b received result $result")

                Behaviors.same
        }
    }

    sealed trait Command
    case class ActorBCommand(result: String) extends Command
}

object ActorBPool {

    import ActorB._

    val poolServiceKey = ServiceKey[Command]("actor-b-pool-key")

    def apply(): Behavior[Command] = ActorUtils.poolOf(serviceKey)(poolServiceKey)
}
