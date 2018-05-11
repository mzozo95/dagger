/*
 * Copyright (C) 2017 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dagger.internal.codegen;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static dagger.internal.codegen.CompilerMode.DEFAULT_MODE;
import static dagger.internal.codegen.CompilerMode.EXPERIMENTAL_ANDROID_MODE;
import static dagger.internal.codegen.Compilers.daggerCompiler;
import static dagger.internal.codegen.GeneratedLines.GENERATED_ANNOTATION;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.util.Collection;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class OptionalBindingRequestFulfillmentTest {
  @Parameters(name = "{0}")
  public static Collection<Object[]> parameters() {
    return CompilerMode.TEST_PARAMETERS;
  }

  private final CompilerMode compilerMode;

  public OptionalBindingRequestFulfillmentTest(CompilerMode compilerMode) {
    this.compilerMode = compilerMode;
  }

  @Test
  public void inlinedOptionalBindings() {
    JavaFileObject module =
        JavaFileObjects.forSourceLines(
            "test.TestModule",
            "package test;",
            "",
            "import dagger.Module;",
            "import dagger.BindsOptionalOf;",
            "import other.Maybe;",
            "import other.DefinitelyNot;",
            "",
            "@Module",
            "interface TestModule {",
            "  @BindsOptionalOf Maybe maybe();",
            "  @BindsOptionalOf DefinitelyNot definitelyNot();",
            "}");
    JavaFileObject maybe =
        JavaFileObjects.forSourceLines(
            "other.Maybe",
            "package other;",
            "",
            "import dagger.Module;",
            "import dagger.Provides;",
            "",
            "public class Maybe {",
            "  @Module",
            "  public interface MaybeModule {",
            "    @Provides static Maybe provideMaybe() { return new Maybe(); }",
            "  }",
            "}");
    JavaFileObject definitelyNot =
        JavaFileObjects.forSourceLines(
            "other.DefinitelyNot",
            "package other;",
            "",
            "public class DefinitelyNot {}");

    JavaFileObject component =
        JavaFileObjects.forSourceLines(
            "test.TestComponent",
            "package test;",
            "",
            "import com.google.common.base.Optional;",
            "import dagger.Component;",
            "import dagger.Lazy;",
            "import javax.inject.Provider;",
            "import other.Maybe;",
            "import other.DefinitelyNot;",
            "",
            "@Component(modules = {TestModule.class, Maybe.MaybeModule.class})",
            "interface TestComponent {",
            "  Optional<Maybe> maybe();",
            "  Optional<Provider<Lazy<Maybe>>> providerOfLazyOfMaybe();",
            "  Optional<DefinitelyNot> definitelyNot();",
            "  Optional<Provider<Lazy<DefinitelyNot>>> providerOfLazyOfDefinitelyNot();",
            "}");

    JavaFileObject generatedComponent =
        compilerMode
            .javaFileBuilder("test.DaggerTestComponent")
            .addLines(
                "package test;",
                "",
                "import com.google.common.base.Optional;",
                "",
                GENERATED_ANNOTATION,
                "public final class DaggerTestComponent implements TestComponent {")
            .addLinesIn(
                EXPERIMENTAL_ANDROID_MODE,
                "  private volatile Provider<Maybe> provideMaybeProvider;",
                "",
                "  private Provider<Maybe> getMaybeProvider() {",
                "    Object local = provideMaybeProvider;",
                "    if (local == null) {",
                "      local = new SwitchingProvider<>(0);",
                "      provideMaybeProvider = (Provider<Maybe>) local;",
                "    }",
                "    return (Provider<Maybe>) local;",
                "  }")
            .addLines(
                "  @Override",
                "  public Optional<Maybe> maybe() {",
                "    return Optional.of(",
                "        Maybe_MaybeModule_ProvideMaybeFactory.proxyProvideMaybe());",
                "  }",
                "",
                "  @Override",
                "  public Optional<Provider<Lazy<Maybe>>> providerOfLazyOfMaybe() {",
                "    return Optional.of(ProviderOfLazy.create(")
            .addLinesIn(
                DEFAULT_MODE,
                "        Maybe_MaybeModule_ProvideMaybeFactory.create()));")
            .addLinesIn(
                EXPERIMENTAL_ANDROID_MODE,
                "        getMaybeProvider()));")
            .addLines(
                "  }",
                "",
                "  @Override",
                "  public Optional<DefinitelyNot> definitelyNot() {",
                "    return Optional.<DefinitelyNot>absent();",
                "  }",
                "",
                "  @Override",
                "  public Optional<Provider<Lazy<DefinitelyNot>>>",
                "      providerOfLazyOfDefinitelyNot() {",
                "    return Optional.<Provider<Lazy<DefinitelyNot>>>absent();",
                "  }")
            .addLinesIn(
                EXPERIMENTAL_ANDROID_MODE,
                "  private final class SwitchingProvider<T> implements Provider<T> {",
                "    private final int id;",
                "",
                "    SwitchingProvider(int id) {",
                "      this.id = id;",
                "    }",
                "",
                "    @SuppressWarnings(\"unchecked\")",
                "    @Override",
                "    public T get() {",
                "      switch (id) {",
                "        case 0:",
                "          return (T) Maybe_MaybeModule_ProvideMaybeFactory.proxyProvideMaybe();",
                "        default:",
                "          throw new AssertionError(id);",
                "      }",
                "    }",
                "  }",
                "}")
            .build();
    Compilation compilation =
        daggerCompiler()
            .withOptions(compilerMode.javacopts())
            .compile(module, maybe, definitelyNot, component);
    assertThat(compilation).succeeded();
    assertThat(compilation)
        .generatedSourceFile("test.DaggerTestComponent")
        .containsElementsIn(generatedComponent);
  }

  @Test
  public void requestForFuture() {
    JavaFileObject module =
        JavaFileObjects.forSourceLines(
            "test.TestModule",
            "package test;",
            "",
            "import dagger.Module;",
            "import dagger.BindsOptionalOf;",
            "import other.Maybe;",
            "import other.DefinitelyNot;",
            "",
            "@Module",
            "interface TestModule {",
            "  @BindsOptionalOf Maybe maybe();",
            "  @BindsOptionalOf DefinitelyNot definitelyNot();",
            "}");
    JavaFileObject maybe =
        JavaFileObjects.forSourceLines(
            "other.Maybe",
            "package other;",
            "",
            "import dagger.Module;",
            "import dagger.Provides;",
            "",
            "public class Maybe {",
            "  @Module",
            "  public interface MaybeModule {",
            "    @Provides static Maybe provideMaybe() { return new Maybe(); }",
            "  }",
            "}");
    JavaFileObject definitelyNot =
        JavaFileObjects.forSourceLines(
            "other.DefinitelyNot",
            "package other;",
            "",
            "public class DefinitelyNot {}");

    JavaFileObject component =
        JavaFileObjects.forSourceLines(
            "test.TestComponent",
            "package test;",
            "",
            "import com.google.common.base.Optional;",
            "import com.google.common.util.concurrent.ListenableFuture;",
            "import dagger.producers.ProductionComponent;",
            "import javax.inject.Provider;",
            "import other.Maybe;",
            "import other.DefinitelyNot;",
            "",
            "@ProductionComponent(modules = {TestModule.class, Maybe.MaybeModule.class})",
            "interface TestComponent {",
            "  ListenableFuture<Optional<Maybe>> maybe();",
            "  ListenableFuture<Optional<DefinitelyNot>> definitelyNot();",
            "}");
    JavaFileObject generatedComponent =
        JavaFileObjects.forSourceLines(
            "test.DaggerTestComponent",
            "package test;",
            "",
            "import com.google.common.base.Optional;",
            "",
            GENERATED_ANNOTATION,
            "public final class DaggerTestComponent implements TestComponent {",
            "  @Override",
            "  public ListenableFuture<Optional<Maybe>> maybe() {",
            "    return Futures.immediateFuture(",
            "        Optional.of(Maybe_MaybeModule_ProvideMaybeFactory.proxyProvideMaybe()));",
            "  }",
            "",
            "  @Override",
            "  public ListenableFuture<Optional<DefinitelyNot>> definitelyNot() {",
            "    return Futures.immediateFuture(Optional.<DefinitelyNot>absent());",

            "  }",
            "}");

    Compilation compilation =
        daggerCompiler()
            .withOptions(compilerMode.javacopts())
            .compile(module, maybe, definitelyNot, component);
    assertThat(compilation).succeeded();
    assertThat(compilation)
        .generatedSourceFile("test.DaggerTestComponent")
        .containsElementsIn(generatedComponent);
  }
}
