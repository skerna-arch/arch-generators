package sample

import com.skerna.project.v1.HelloApiCodecInstaller
import io.vertx.kotlin.coroutines.CoroutineVerticle
class VerticleProducer: CoroutineVerticle() {

  private lateinit var service:ServiceDomain

  override suspend fun start() {
    // Initialize Eventbus service
    this.service = ServiceDomain(vertx)
    HelloApiCodecInstaller.installCodecs(vertx)
    // autobind service
    this.service.autoBindService()
  }

  override suspend fun stop() {
    // uninstall codecs
    HelloApiCodecInstaller.uninstallCodecs(vertx)
  }
}
