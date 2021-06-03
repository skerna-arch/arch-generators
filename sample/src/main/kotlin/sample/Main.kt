package sample

import io.vertx.core.Vertx

fun main(){
  val vertxDeployment = Vertx.vertx();
  vertxDeployment.deployVerticle(VerticleProducer())
  vertxDeployment.deployVerticle(VerticleConsumer())
}
