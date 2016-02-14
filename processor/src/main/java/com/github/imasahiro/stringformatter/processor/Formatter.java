package com.github.imasahiro.stringformatter.processor;

import java.lang.reflect.Type;
import java.util.List;

import javax.annotation.Generated;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

class Formatter {
    private final String name;
    private final String format;
    private final PackageElement packageElement;

    Formatter(String name, String format, PackageElement packageElement) {
        this.name = name;
        this.format = format;
        this.packageElement = packageElement;
    }

    public String getPackageName() {
        return packageElement.getQualifiedName().toString();
    }

    public String getSourceFileName() {
        return packageElement.toString() + "." + name;
    }

    public TypeSpec getType() {
        List<FormatString> formatStringList = FormatParser.parse(format);
        TypeSpec.Builder builder =
                TypeSpec.classBuilder(name)
                        .addAnnotation(
                                AnnotationSpec.builder(Generated.class)
                                              .addMember("value", "{$S}",
                                                         StringFormatterProcessor.class.getCanonicalName())
                                              .build())
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        Sets.cartesianProduct(FluentIterable.from(formatStringList)
                                            .filter(FormatSpecifier.class)
                                            .transform(e -> e.getConversionType().getType())
                                            .toList()).forEach(
                types -> builder.addMethod(MethodSpec.methodBuilder("format")
                                                     .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                     .addParameters(buildParamTypes(types))
                                                     .addCode(buildBody(formatStringList, types))
                                                     .returns(TypeName.get(String.class))
                                                     .build()));
        return builder.build();
    }

    private List<ParameterSpec> buildParamTypes(List<Type> argumentTypes) {
        ImmutableList.Builder<ParameterSpec> builder = ImmutableList.builder();
        for (int i = 0; i < argumentTypes.size(); i++) {
            builder.add(ParameterSpec.builder(argumentTypes.get(i), "arg" + i, Modifier.FINAL).build());
        }
        return builder.build();
    }

    private CodeBlock buildBody(List<FormatString> formatStringList, List<Type> argumentTypes) {
        CodeBlock.Builder builder = CodeBlock.builder()
                                             .add("final StringBuilder sb = new StringBuilder();\n");
        int idx = 0;
        for (int i = 0; i < formatStringList.size(); i++) {
            FormatString formatString = formatStringList.get(i);
            if (formatString instanceof FormatSpecifier) {
                formatStringList.get(i).emit(builder, argumentTypes.get(idx++));
            } else {
                formatStringList.get(i).emit(builder, null);
            }
        }
        return builder.add("return sb.toString();\n")
                      .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private String name;
        private PackageElement pkg;
        private String format;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder pkg(PackageElement pkg) {
            this.pkg = pkg;
            return this;
        }

        public Builder formatter(String format) {
            this.format = format;
            return this;
        }

        public Formatter build() {
            return new Formatter(name, format, pkg);
        }
    }

}