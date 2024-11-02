/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.routing;

import controllers.Assets;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import play.api.mvc.JavascriptLiteral;
import play.api.mvc.PathBindable;
import play.api.mvc.PathBindable$;
import play.api.mvc.QueryStringBindable;
import play.api.mvc.QueryStringBindable$;
import play.api.routing.HandlerDef;
import play.libs.Scala;
import play.libs.reflect.MethodUtils;
import play.mvc.Result;
import play.mvc.WebSocket;
import scala.reflect.ClassTag$;

public abstract class JavaGeneratedRouter extends GeneratedRouter {

  protected HandlerInvoker<?> createJavaInvoker(boolean isRequest, HandlerDef handlerDef) {
    var controller = HandlerInvokerFactory$.MODULE$.loadJavaControllerClass(handlerDef);
    var method =
        MethodUtils.getMatchingAccessibleMethod(
            controller, handlerDef.method(), handlerDef.getParameterTypes().toArray(new Class[0]));
    var returnType = method.getGenericReturnType();
    if (returnType instanceof Class && Result.class.isAssignableFrom((Class<?>) returnType)) {
      if (isRequest) {
        return createInvoker(null, handlerDef, HandlerInvokerFactory.wrapJavaRequest());
      } else {
        return createInvoker(null, handlerDef, HandlerInvokerFactory.wrapJava());
      }
    } else if (returnType instanceof Class
        && WebSocket.class.isAssignableFrom((Class<?>) returnType)) {
      return createInvoker(null, handlerDef, HandlerInvokerFactory.javaWebSocket());
    } else if (returnType instanceof ParameterizedType) {
      var wrapType = (ParameterizedType) returnType;
      if (wrapType.getRawType() instanceof Class
          && CompletionStage.class.isAssignableFrom((Class<?>) wrapType.getRawType())) {
        var actualType = wrapType.getActualTypeArguments()[0];
        if (actualType instanceof Class && Result.class.isAssignableFrom((Class<?>) actualType)) {
          if (isRequest) {
            return createInvoker(null, handlerDef, HandlerInvokerFactory.wrapJavaPromiseRequest());
          } else {
            return createInvoker(null, handlerDef, HandlerInvokerFactory.wrapJavaPromise());
          }
        }
      }
    }
    return createInvoker(null, handlerDef, HandlerInvokerFactory.passThrough());
  }

  public static PathBindable<?> pathBindableFor(ReverseRouteContext rrc, Class<?> clazz) {
    PathBindable<?> builtIn = Scala.orNull(PathBindable$.MODULE$.pathBindableRegister().get(clazz));
    if (builtIn != null) {
      return builtIn;
    } else if (play.mvc.PathBindable.class.isAssignableFrom(clazz)) {
      return javaPathBindableFor(clazz);
    } else if (clazz.equals(Assets.Asset.class)) {
      // Special case for Asset, treat as a string
      return Assets.Asset$.MODULE$.assetPathBindable(rrc);
    } else if (clazz.equals(Object.class)) {
      // Special case for object, treat as a string
      return PathBindable.bindableString$.MODULE$;
    } else {
      throw new IllegalArgumentException("Don't know how to bind argument of type " + clazz);
    }
  }

  public static PathBindable<?> pathBindableFor(Class<?> clazz) {
    return pathBindableFor(ReverseRouteContext.empty(), clazz);
  }

  private static <A extends play.mvc.PathBindable<A>> PathBindable<?> javaPathBindableFor(
      Class<?> clazz) {
    return PathBindable$.MODULE$.<A>javaPathBindable(ClassTag$.MODULE$.apply(clazz));
  }

  public static JavascriptLiteral<?> javascriptLiteralFor(Class<?>... classes) {
    Class<?> clazz = classes[0];
    if (clazz.equals(String.class)) {
      return JavascriptLiteral.literalString();
    } else if (clazz.equals(Boolean.class)) {
      return JavascriptLiteral.literalBoolean();
    } else if (clazz.equals(Character.class)) {
      return JavascriptLiteral.literalJavaCharacter();
    } else if (clazz.equals(Short.class)) {
      return JavascriptLiteral.literalJavaShort();
    } else if (clazz.equals(Integer.class)) {
      return JavascriptLiteral.literalJavaInteger();
    } else if (clazz.equals(Long.class)) {
      return JavascriptLiteral.literalJavaLong();
    } else if (clazz.equals(Float.class)) {
      return JavascriptLiteral.literalJavaFloat();
    } else if (clazz.equals(Double.class)) {
      return JavascriptLiteral.literalJavaDouble();
    } else if (clazz.equals(Optional.class)) {
      return JavascriptLiteral.literalJavaOption(
          javascriptLiteralFor(Arrays.copyOfRange(classes, 1, classes.length)));
    } else if (clazz.equals(OptionalInt.class)) {
      return JavascriptLiteral.literalJavaOptionalInt();
    } else if (clazz.equals(OptionalLong.class)) {
      return JavascriptLiteral.literalJavaOptionalLong();
    } else if (clazz.equals(OptionalDouble.class)) {
      return JavascriptLiteral.literalJavaOptionalDouble();
    } else if (clazz.equals(UUID.class)) {
      return JavascriptLiteral.literalUUID();
    } else if (clazz.equals(Assets.Asset.class)) {
      // Special case for Asset, treat as a string
      return JavascriptLiteral.literalAsset();
    } else if (clazz.equals(Object.class)) {
      // Special case for object, treat as a string
      return JavascriptLiteral.literalString();
    } else {
      throw new IllegalArgumentException("Don't know how to bind argument of type " + clazz);
    }
  }

  public static QueryStringBindable<?> queryStringBindableFor(Class<?>... classes) {
    return queryStringBindableFor(ReverseRouteContext.empty(), classes);
  }

  public static QueryStringBindable<?> queryStringBindableFor(
      ReverseRouteContext rrc, Class<?>... classes) {
    Class<?> clazz = classes[0];
    if (clazz.equals(String.class)) {
      return QueryStringBindable.bindableString();
    } else if (clazz.equals(Boolean.class)) {
      return QueryStringBindable.bindableJavaBoolean();
    } else if (clazz.equals(Character.class)) {
      return QueryStringBindable.bindableCharacter();
    } else if (clazz.equals(Short.class)) {
      return QueryStringBindable.bindableJavaShort();
    } else if (clazz.equals(Integer.class)) {
      return QueryStringBindable.bindableJavaInteger();
    } else if (clazz.equals(OptionalInt.class)) {
      return QueryStringBindable.bindableJavaOptionalInt();
    } else if (clazz.equals(Long.class)) {
      return QueryStringBindable.bindableJavaLong();
    } else if (clazz.equals(OptionalLong.class)) {
      return QueryStringBindable.bindableJavaOptionalLong();
    } else if (clazz.equals(Float.class)) {
      return QueryStringBindable.bindableJavaFloat();
    } else if (clazz.equals(Double.class)) {
      return QueryStringBindable.bindableJavaDouble();
    } else if (clazz.equals(OptionalDouble.class)) {
      return QueryStringBindable.bindableJavaOptionalDouble();
    } else if (clazz.equals(Optional.class)) {
      return QueryStringBindable.bindableJavaOption(
          queryStringBindableFor(Arrays.copyOfRange(classes, 1, classes.length)));
    } else if (clazz.equals(UUID.class)) {
      return QueryStringBindable.bindableUUID$.MODULE$;
    } else if (List.class.isAssignableFrom(clazz)) {
      return QueryStringBindable.bindableJavaList(
          queryStringBindableFor(Arrays.copyOfRange(classes, 1, classes.length)));
    } else if (play.mvc.QueryStringBindable.class.isAssignableFrom(clazz)) {
      return javaQueryStringBindableFor(clazz);
    } else {
      throw new IllegalArgumentException("Don't know how to bind argument of type " + clazz);
    }
  }

  private static <A extends play.mvc.QueryStringBindable<A>>
      QueryStringBindable<?> javaQueryStringBindableFor(Class<?> clazz) {
    return QueryStringBindable$.MODULE$.<A>javaQueryStringBindable(ClassTag$.MODULE$.apply(clazz));
  }
}
