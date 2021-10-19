package com.anqit.repro

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.actor.typed.scaladsl.{ Behaviors, Routers }

object ActorUtils {
    def poolOf[T](routeeKey: ServiceKey[T])(routerKey: ServiceKey[T]): Behavior[T] = Behaviors.setup { ctx =>
        ctx.system.receptionist ! Receptionist.Register(routerKey, ctx.self)

        Routers.group(routeeKey)
    }
}
