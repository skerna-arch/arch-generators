package io.skerna.protobuff.protoc.kotlingen;

import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location;
import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.Generator;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;
import com.salesforce.jprotoc.ProtocPlugin;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import static com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;

public class VertxGenerator extends Generator {

  private static final int SERVICE_NUMBER_OF_PATHS = 2;
  private static final int METHOD_NUMBER_OF_PATHS = 4;
  private static final String CODEC_SUFFIX = "Codec";
  private static final String CLASS_SUFFIX = "Bus";
  private static final String BUS_CLI_SUFFIX = "BusCli";
  private static final String BUS_CLI_IMPL_SUFFIX = "BusCliImpl";
  private static final String BUS_SERVICE_SUFFIX = "BusService";
  private static final String BUS_CODEC_INSTALLER="CodecInstaller";
  private static final String STUB_SUFFIX = "StubExt";
  private static final String ADAPTERS_FILE_PATH = "io/skerna/arch/protoservices/Adapters.kt";
  private static final String SERVICE_JAVA_DOC_PREFIX = "    ";
  private static final String METHOD_JAVA_DOC_PREFIX = "        ";

  public static void main(String[] args) {
    ProtocPlugin.generate(new VertxGenerator());
  }

  @Override
  public List<CodeGeneratorResponse.File> generateFiles(CodeGeneratorRequest request)
      throws GeneratorException {
    final ProtoTypeMap typeMap = ProtoTypeMap.of(request.getProtoFileList());

    List<FileDescriptorProto> protosToGenerate = request.getProtoFileList().stream()
        .filter(protoFile -> request.getFileToGenerateList().contains(protoFile.getName()))
        .collect(Collectors.toList());

    List<Context> services = findServices(protosToGenerate, typeMap);
    return generateFiles(services);
  }

  private List<Context> findServices(List<FileDescriptorProto> protos, ProtoTypeMap typeMap) {
    List<Context> contexts = new ArrayList<>();

    protos.forEach(fileProto -> {
      List<Location> locations = fileProto.getSourceCodeInfo().getLocationList();
      locations.stream()
          .filter(location -> location.getPathCount() == SERVICE_NUMBER_OF_PATHS
              && location.getPath(0) == FileDescriptorProto.SERVICE_FIELD_NUMBER)
          .forEach(location -> {
            int serviceNumber = location.getPath(SERVICE_NUMBER_OF_PATHS - 1);
            Context context = context(
                fileProto.getService(serviceNumber), typeMap, locations, serviceNumber);
            context.javaDoc = getJavaDoc(getComments(location), SERVICE_JAVA_DOC_PREFIX);
            context.protoName = fileProto.getName();
            context.packageName = extractPackageName(fileProto);
            contexts.add(context);
          });
    });

    return contexts;
  }

  private String extractPackageName(FileDescriptorProto proto) {
    FileOptions options = proto.getOptions();
    if (options != null) {
      String javaPackage = options.getJavaPackage();
      if (!Strings.isNullOrEmpty(javaPackage)) {
        return javaPackage;
      }
    }

    return Strings.nullToEmpty(proto.getPackage());
  }

  private Context context(
      ServiceDescriptorProto serviceProto,
      ProtoTypeMap protoTypeMap,
      List<Location> locations,
      int serviceNumber) {

    Context context = new Context();
    context.serviceName = serviceProto.getName();
    context.deprecated = serviceProto.getOptions() != null
        && serviceProto.getOptions().getDeprecated();

    locations.stream()
        .filter(location -> location.getPathCount() == METHOD_NUMBER_OF_PATHS
                            && location.getPath(0) == FileDescriptorProto.SERVICE_FIELD_NUMBER
                            && location.getPath(1) == serviceNumber
                            && location.getPath(2) == ServiceDescriptorProto.METHOD_FIELD_NUMBER)
        .forEach(location -> {
          int methodNumber = location.getPath(METHOD_NUMBER_OF_PATHS - 1);
          MethodContext methodContext = methodContext(
              serviceProto.getMethod(methodNumber), protoTypeMap);
          methodContext.methodNumber = methodNumber;
          methodContext.javaDoc = getJavaDoc(getComments(location), METHOD_JAVA_DOC_PREFIX);
          context.methods.add(methodContext);

        });
    context.messages.addAll(getMessages(context.methods));

    return context;
  }

  private MethodContext methodContext(MethodDescriptorProto methodProto, ProtoTypeMap typeMap) {
    MethodContext methodContext = new MethodContext();
    methodContext.methodName = lowerCaseFirst(methodProto.getName());
    methodContext.inputType = typeMap.toJavaTypeName(methodProto.getInputType());
    methodContext.outputType = typeMap.toJavaTypeName(methodProto.getOutputType());
    methodContext.deprecated = methodProto.getOptions() != null
        && methodProto.getOptions().getDeprecated();
    methodContext.isManyInput = methodProto.getClientStreaming();
    methodContext.isManyOutput = methodProto.getServerStreaming();
    if (!methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
      methodContext.grpcCallsMethodName = "asyncUnaryCall";
    }
    if (!methodProto.getClientStreaming() && methodProto.getServerStreaming()) {
      methodContext.grpcCallsMethodName = "asyncServerStreamingCall";
    }
    if (methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
      methodContext.grpcCallsMethodName = "asyncClientStreamingCall";
    }
    if (methodProto.getClientStreaming() && methodProto.getServerStreaming()) {
      methodContext.grpcCallsMethodName = "asyncBidiStreamingCall";
    }
    return methodContext;
  }
  private Set<Message> getMessages(List<MethodContext> methodContextList){
    Set<Message> messageTypes = new HashSet<>();
    Set<Message>inputTypes = methodContextList.stream()
            .map((methodContext ->new Message(methodContext.inputType)))
            .collect(Collectors.toSet());

    Set<Message>outputTypes = methodContextList.stream()
            .map((methodContext ->new Message(methodContext.outputType)))
            .collect(Collectors.toSet());

    messageTypes.addAll(inputTypes);
    messageTypes.addAll(outputTypes);
    return messageTypes;
  }

  private String lowerCaseFirst(String s) {
    return Character.toLowerCase(s.charAt(0)) + s.substring(1);
  }

  private List<PluginProtos.CodeGeneratorResponse.File> generateFiles(List<Context> services) {
    List<PluginProtos.CodeGeneratorResponse.File> files = new ArrayList<>();

    files.add(buildUtilFile());
    for (Context service : services) {
      // buildServiceBaseImpl is disabled by default
      //files.add(buildServiceBaseImpl(service));
      files.add(buildCodecs(service));
      files.add(buildBusClientInterface(service));
      files.add(buildBusClientImpl(service));
      files.add(buildBusService(service));
      files.add(buildCodecInstaller(service));
    }
    return files;
  }

  private PluginProtos.CodeGeneratorResponse.File buildUtilFile() {
    UtilContext utilContext = new UtilContext();
    String content = applyTemplate("Adapters.mustache", utilContext);

    return PluginProtos.CodeGeneratorResponse.File
        .newBuilder()
        .setName(ADAPTERS_FILE_PATH)
        .setContent(content)
        .build();
  }

  private PluginProtos.CodeGeneratorResponse.File buildServiceBaseImpl(Context context) {
    String content = applyTemplate("Bus.mustache", context);
    String fileName = context.serviceName + CLASS_SUFFIX + ".kt";
    return PluginProtos.CodeGeneratorResponse.File
        .newBuilder()
        .setName(absoluteFileName(context.packageName, fileName))
        .setContent(content)
        .build();
  }



  private PluginProtos.CodeGeneratorResponse.File buildCodecs(Context context) {
    String content = applyTemplate("Codec.mustache", context);
    String fileName = context.serviceName + CODEC_SUFFIX + ".kt";
    return PluginProtos.CodeGeneratorResponse.File
            .newBuilder()
            .setName(absoluteFileName(context.packageName, fileName))
            .setContent(content)
            .build();
  }

  private PluginProtos.CodeGeneratorResponse.File buildBusClientInterface(Context context) {
    String content = applyTemplate("BusClient.mustache", context);
    String fileName = context.serviceName + BUS_CLI_SUFFIX + ".kt";
    return PluginProtos.CodeGeneratorResponse.File
            .newBuilder()
            .setName(absoluteFileName(context.packageName, fileName))
            .setContent(content)
            .build();
  }

  private PluginProtos.CodeGeneratorResponse.File buildBusClientImpl(Context context) {
    String content = applyTemplate("BusClientImpl.mustache", context);
    String fileName = context.serviceName + BUS_CLI_IMPL_SUFFIX + ".kt";
    return PluginProtos.CodeGeneratorResponse.File
            .newBuilder()
            .setName(absoluteFileName(context.packageName, fileName))
            .setContent(content)
            .build();
  }

  private PluginProtos.CodeGeneratorResponse.File buildBusService(Context context) {
    String content = applyTemplate("BusService.mustache", context);
    String fileName = context.serviceName + BUS_SERVICE_SUFFIX + ".kt";
    return PluginProtos.CodeGeneratorResponse.File
            .newBuilder()
            .setName(absoluteFileName(context.packageName, fileName))
            .setContent(content)
            .build();
  }

  private PluginProtos.CodeGeneratorResponse.File buildCodecInstaller(Context context) {
    String content = applyTemplate("CodecInstaller.mustache", context);
    String fileName = context.serviceName + BUS_CODEC_INSTALLER + ".kt";
    return PluginProtos.CodeGeneratorResponse.File
            .newBuilder()
            .setName(absoluteFileName(context.packageName, fileName))
            .setContent(content)
            .build();
  }
  private String absoluteFileName(String packageName, String fileName) {
    String dir = packageName.replace('.', '/');
    if (Strings.isNullOrEmpty(dir)) {
      return fileName;
    } else {
      return dir + "/" + fileName;
    }
  }

  private String getComments(Location location) {
    return location.getLeadingComments().isEmpty()
        ? location.getTrailingComments()
        : location.getLeadingComments();
  }

  private String getJavaDoc(String comments, String prefix) {
    if (!comments.isEmpty()) {
      StringBuilder builder = new StringBuilder("/**\n")
          .append(prefix).append(" * <pre>\n");
      Arrays.stream(HtmlEscapers.htmlEscaper().escape(comments).split("\n"))
          // Kotlin allows nested block comments, so any occurrence of '/*' would begin
          // a nested block, breaking the generated code.
          .map(line -> line.replace("/*", "/&#42;"))
          .map(line -> line.replace("*/", "*&#47;"))
          .forEach(line -> builder.append(prefix).append(" * ").append(line).append("\n"));
      builder
          .append(prefix).append(" * <pre>\n")
          .append(prefix).append(" */");
      return builder.toString();
    }
    return null;
  }

  /**
   * Template class for proto Service objects.
   */
  private class Context {
    // CHECKSTYLE DISABLE VisibilityModifier FOR 6 LINES
    public String protoName;
    public String packageName;
    public String serviceName;
    public boolean deprecated;
    public String javaDoc;
    public List<MethodContext> methods = new ArrayList<>();
    public final Set<Message> messages= new HashSet<>();
  }

  /**
   * Template class for proto RPC objects.
   */
  private class MethodContext {
    // CHECKSTYLE DISABLE VisibilityModifier FOR 10 LINES
    public String methodName;
    public String inputType;
    public String outputType;
    public boolean deprecated;
    public boolean isManyInput;
    public boolean isManyOutput;
    public String grpcCallsMethodName;
    public int methodNumber;
    public String javaDoc;

    // This method mimics the upper-casing method ogf gRPC to ensure compatibility
    // See https://github.com/grpc/grpc-java/blob/v1.8.0/compiler/src/java_plugin/cpp/java_generator.cpp#L58
    public String methodNameUpperUnderscore() {
      StringBuilder s = new StringBuilder();
      for (int i = 0; i < methodName.length(); i++) {
        char c = methodName.charAt(i);
        s.append(Character.toUpperCase(c));
        if ((i < methodName.length() - 1)
            && Character.isLowerCase(c)
            && Character.isUpperCase(methodName.charAt(i + 1))) {
          s.append('_');
        }
      }
      return s.toString();
    }

    public String methodNamePascalCase() {
      return String.valueOf(Character.toUpperCase(methodName.charAt(0))) + methodName.substring(1);
    }
  }
  private class Message{
    public String name;

    public Message(String name) {
      String[] qualName = name.split("\\.");
      if(qualName.length>0){
        this.name = qualName[qualName.length-1];
      }else{
        this.name=name;
      }
    }

    @Override
    public String toString() {
      return "Message{" +
              "name='" + name + '\'' +
              '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Message message = (Message) o;
      return Objects.equals(name, message.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }
  /**
   * Template class for adapters file.
   */
  private class UtilContext {
  }
}
