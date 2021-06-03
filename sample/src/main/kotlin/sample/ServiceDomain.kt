package sample

import com.skerna.project.v1.HelloApiBusService
import com.skerna.project.v1.HelloRequest
import com.skerna.project.v1.HelloResponse
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import kotlinx.coroutines.Dispatchers
import java.time.LocalTime
import kotlin.coroutines.CoroutineContext

/**
 * ServiceDomain, implementation of @see HelloApiBusService
 */
class ServiceDomain(vertx: Vertx, coroutineContext: CoroutineContext = Dispatchers.Default) : HelloApiBusService(vertx, coroutineContext) {

  /**
   * Domain logic, eventbus action
   */
  override suspend fun helloWorld(request: HelloRequest, headers: MultiMap): HelloResponse {
    var valueInput = request.name
    valueInput += " World!! ${LocalTime.now()}";
    return HelloResponse.newBuilder()
      .setResult(valueInput)
      .build()
  }

  override suspend fun helloWorld2(request: HelloRequest, headers: MultiMap): HelloResponse {
    var valueInput = request.name
    valueInput += " World2222!! ${LocalTime.now()}";
    return HelloResponse.newBuilder()
      .setResult(valueInput)
      .build()
  }
}
