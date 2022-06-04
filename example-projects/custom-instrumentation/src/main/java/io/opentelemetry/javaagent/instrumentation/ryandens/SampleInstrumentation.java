package io.opentelemetry.javaagent.instrumentation.ryandens;


import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public final class SampleInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.ryandens.javaagent.example.Fibonacci");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isPublic()
            .and(named("iterative")
                .and(takesArguments(1))
                .and(takesArgument(0, int.class))),
          this.getClass().getName() + "$MethodAdvice");
  }

  static final class MethodAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(@Advice.Local("otelSpan") Span span,
                               @Advice.Local("otelContext") Context context,
                               @Advice.Local("otelScope") Scope scope) {
      Context parentContext = Context.current();
      // TODO helper classes don't seem to work for some reaso
      span = GlobalOpenTelemetry.get().getTracer("FibonacciTracer").spanBuilder("iterative").setSpanKind(SpanKind.INTERNAL).setParent(parentContext).startSpan();
      context = parentContext.with(span);
      scope = context.makeCurrent();
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void onExit(@Advice.Local("otelSpan") Span span,
                              @Advice.Local("otelContext") Context context,
                              @Advice.Local("otelScope") Scope scope,
                              @Advice.Thrown Throwable exception) {
      if (scope == null) {
        return;
      }
      if (exception != null) {
        span.setStatus(StatusCode.ERROR);
      } else {
        span.setStatus(StatusCode.OK);
      }
      scope.close();
      span.end();
    }
  }
}
