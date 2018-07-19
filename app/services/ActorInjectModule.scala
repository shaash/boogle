package services

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport


class ActorInjectModule extends AbstractModule with AkkaGuiceSupport {
  def configure = {
    bindActor[IndexingService]("indexing-actor")
  }
}
