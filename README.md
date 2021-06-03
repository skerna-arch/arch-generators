# Vertx Protocol Buffers codegen

generator protocol buffers for vertx, this generator is a plugin for [google protobuf gradle](https://github.com/google/protobuf-gradle-plugin)


### Initial commit 

Experiments with Verx ✌️⚡

1) make install local module :protoc-vertx-eventbus
2) Skerna Jfrog repository pronto :v 

No es mucho, me falta documentar :v ...  pero es la base... de la idea... ya ire actualizando todos mis proyectos... si tienes una idea de como mejorar dimela y la implemento 

## Setup
maven coordinates missing ... 
minimal setup, v1, falta deployar en skerna repos...
```gradle

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:3.11.4"
  }
  plugins {
    eventbus {
      artifact = "io.skerna.arch:protoc-vertx-eventbus:0.1.4:jdk8-linux@exe"
    }
    grpc {
      artifact = "io.grpc:protoc-gen-grpc-java:1.27.2"
    }
  }
  generateProtoTasks {
    all().each { task ->
      task.generateDescriptorSet = true
      task.descriptorSetOptions.includeSourceInfo = true
      task.descriptorSetOptions.includeImports = true
      task.descriptorSetOptions.path = "${buildDir}/resources/main/META-INF/proto/app-types.desc"
    }
    ofSourceSet("main").forEach {
      it.plugins {
        eventbus
      }
    }
    ofSourceSet("main").forEach {
      it.plugins {
        grpc
      }
    }
  }
}
```
