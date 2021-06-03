package sample

import com.skerna.project.v1.HelloApiBusCliImpl
import com.skerna.project.v1.HelloApiCodecInstaller
import com.skerna.project.v1.HelloRequest
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.*
import java.util.*
import kotlin.random.Random

class VerticleConsumer : CoroutineVerticle() {

  /**
   * Verticle consume from eventbus ServiceDomain
   */
  override suspend fun start() {
    // Create BUS PROTO CLIENT
    val client = HelloApiBusCliImpl(vertx)
    // install codecs (protocol Buffers)
    HelloApiCodecInstaller.installCodecs(vertx)
    // Demo Periodic task 1s fetch hello request
    vertx.setPeriodicCoroutine(1000) {
      val request = HelloRequest
        .newBuilder()
        .setName("Hello user ${Random.nextInt()}")
        .build()
      // read result from eventbus
      val result = client.helloWorld2(request)
      // display
      println(result.body().result)
    }
  }

  override suspend fun stop() {
    HelloApiCodecInstaller.uninstallCodecs(vertx)
  }

  suspend fun Vertx.setPeriodicCoroutine(delay: Long, action: suspend () -> Unit): Long {
    return setPeriodic(delay) {
      GlobalScope.launch { action() }
    }
  }
}
