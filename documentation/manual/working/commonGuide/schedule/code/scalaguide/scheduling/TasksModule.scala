//###replace: package tasks
package scalaguide.scheduling

import play.api.inject.{SimpleModule, _}

class TasksModule extends SimpleModule(bind[MyActorTask].toSelf.eagerly())
