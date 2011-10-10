package play.data.enhancers;

import java.io.*;
import java.util.*;

import javassist.*;
import javassist.expr.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;

public class PropertiesEnhancer {

    @Target({METHOD, FIELD, TYPE})
    @Retention(RUNTIME)
    public static @interface GeneratedAccessor {}

    @Target({METHOD, FIELD, TYPE})
    @Retention(RUNTIME)
    public static @interface GeneratedGetAccessor {}

    @Target({METHOD, FIELD, TYPE})
    @Retention(RUNTIME)
    public static @interface GeneratedSetAccessor {}

    @Target({METHOD, FIELD, TYPE})
    @Retention(RUNTIME)
    public static @interface RewrittenAccessor {}

    public static void generateAccessors(String classpath, File classFile) throws Exception {
        ClassPool classPool = new ClassPool();
        classPool.appendSystemPath();
        classPool.appendPathList(classpath);

        FileInputStream is = new FileInputStream(classFile);
        try {
            CtClass ctClass = classPool.makeClass(is);
            if(hasAnnotation(ctClass, GeneratedAccessor.class)) {
                is.close();
                return;
            }
            for (CtField ctField : ctClass.getDeclaredFields()) {
                if(isProperty(ctField)) {

                    // Property name
                    String propertyName = ctField.getName().substring(0, 1).toUpperCase() + ctField.getName().substring(1);
                    String getter = "get" + propertyName;
                    String setter = "set" + propertyName;

                    try {
                        CtMethod ctMethod = ctClass.getDeclaredMethod(getter);
                        if (ctMethod.getParameterTypes().length > 0 || Modifier.isStatic(ctMethod.getModifiers())) {
                            throw new NotFoundException("it's not a getter !");
                        }
                    } catch (NotFoundException noGetter) {
                        // Create getter
                        CtMethod getMethod = CtMethod.make("public " + ctField.getType().getName() + " " + getter + "() { return this." + ctField.getName() + "; }", ctClass);
                        ctClass.addMethod(getMethod);
                        createAnnotation(getAnnotations(getMethod), GeneratedAccessor.class);
                        createAnnotation(getAnnotations(ctField), GeneratedGetAccessor.class);
                    }

                    try {
                        CtMethod ctMethod = ctClass.getDeclaredMethod(setter);
                        if (ctMethod.getParameterTypes().length != 1 || !ctMethod.getParameterTypes()[0].equals(ctField.getType()) || Modifier.isStatic(ctMethod.getModifiers())) {
                            throw new NotFoundException("it's not a setter !");
                        }
                    } catch (NotFoundException noSetter) {
                        // Create setter
                        CtMethod setMethod = CtMethod.make("public void " + setter + "(" + ctField.getType().getName() + " value) { this." + ctField.getName() + " = value; }", ctClass);
                        ctClass.addMethod(setMethod);
                        createAnnotation(getAnnotations(setMethod), GeneratedAccessor.class);
                        createAnnotation(getAnnotations(ctField), GeneratedSetAccessor.class);
                    }

                }

            }

            createAnnotation(getAnnotations(ctClass), GeneratedAccessor.class);

            is.close();
            FileOutputStream os = new FileOutputStream(classFile);
            os.write(ctClass.toBytecode());
            os.close();

        } catch(Exception e) {
            e.printStackTrace();
            try {
                is.close();
            } catch(Exception ex) {
                throw ex;
            }
            throw e;
        }
    }

    public static void rewriteAccess(String classpath, File classFile) throws Exception {
        ClassPool classPool = new ClassPool();
        classPool.appendSystemPath();
        classPool.appendPathList(classpath);

        FileInputStream is = new FileInputStream(classFile);
        try {
            CtClass ctClass = classPool.makeClass(is);
            if(hasAnnotation(ctClass, RewrittenAccessor.class)) {
                is.close();
                return;
            }

            for (final CtBehavior ctMethod : ctClass.getDeclaredBehaviors()) {
                ctMethod.instrument(new ExprEditor() {

                    @Override
                    public void edit(FieldAccess fieldAccess) throws CannotCompileException {
                        try {

                            // Has accessor
                            if (isAccessor(fieldAccess.getField())) {

                                String propertyName = null;
                                if (fieldAccess.getField().getDeclaringClass().equals(ctMethod.getDeclaringClass())
                                    || ctMethod.getDeclaringClass().subclassOf(fieldAccess.getField().getDeclaringClass())) {
                                    if ((ctMethod.getName().startsWith("get") || ctMethod.getName().startsWith("set")) && ctMethod.getName().length() > 3) {
                                        propertyName = ctMethod.getName().substring(3);
                                        propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
                                    }
                                }

                                if (propertyName == null || !propertyName.equals(fieldAccess.getFieldName())) {

                                    String getSet = fieldAccess.getFieldName().substring(0,1).toUpperCase() + fieldAccess.getFieldName().substring(1);

                                    if (fieldAccess.isReader() && hasAnnotation(fieldAccess.getField(), GeneratedGetAccessor.class)) {
                                        // Rewrite read access
                                        fieldAccess.replace("$_ = $0.get" + getSet + "();");
                                    } else if (fieldAccess.isWriter() && hasAnnotation(fieldAccess.getField(), GeneratedSetAccessor.class)) {
                                        // Rewrite write access
                                        fieldAccess.replace("$0.set" + getSet + "($1);");
                                    }
                                }
                            }

                        } catch (Exception e) {
                            throw new CannotCompileException(e);
                        }
                    }
                });
            }

            createAnnotation(getAnnotations(ctClass), RewrittenAccessor.class);

            is.close();
            FileOutputStream os = new FileOutputStream(classFile);
            os.write(ctClass.toBytecode());
            os.close();

        } catch(Exception e) {
            e.printStackTrace();
            try {
                is.close();
            } catch(Exception ex) {
                throw ex;
            }
            throw e;
        }
    }

    // --

    static boolean isProperty(CtField ctField) {
        if (ctField.getName().equals(ctField.getName().toUpperCase()) || ctField.getName().substring(0, 1).equals(ctField.getName().substring(0, 1).toUpperCase())) {
            return false;
        }
        return Modifier.isPublic(ctField.getModifiers())
                && !Modifier.isFinal(ctField.getModifiers())
                && !Modifier.isStatic(ctField.getModifiers());
    }

    static boolean isAccessor(CtField ctField) throws Exception {
        return hasAnnotation(ctField, GeneratedGetAccessor.class) || hasAnnotation(ctField, GeneratedSetAccessor.class);
    }

    // --

    /**
     * Test if a class has the provided annotation
     */
    static boolean hasAnnotation(CtClass ctClass, Class<? extends java.lang.annotation.Annotation> annotationType) throws ClassNotFoundException {
        for (Object object : ctClass.getAvailableAnnotations()) {
            java.lang.annotation.Annotation ann = (java.lang.annotation.Annotation) object;
            if (ann.annotationType().getName().equals(annotationType.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test if a field has the provided annotation
     */
    static boolean hasAnnotation(CtField ctField, Class<? extends java.lang.annotation.Annotation> annotationType) throws ClassNotFoundException {
        for (Object object : ctField.getAvailableAnnotations()) {
            java.lang.annotation.Annotation ann = (java.lang.annotation.Annotation) object;
            if (ann.annotationType().getName().equals(annotationType.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieve all class annotations.
     */
    static AnnotationsAttribute getAnnotations(CtClass ctClass) {
        AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute) ctClass.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
        if (annotationsAttribute == null) {
            annotationsAttribute = new AnnotationsAttribute(ctClass.getClassFile().getConstPool(), AnnotationsAttribute.visibleTag);
            ctClass.getClassFile().addAttribute(annotationsAttribute);
        }
        return annotationsAttribute;
    }

    /**
     * Retrieve all field annotations.
     */
    static AnnotationsAttribute getAnnotations(CtField ctField) {
        AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute) ctField.getFieldInfo().getAttribute(AnnotationsAttribute.visibleTag);
        if (annotationsAttribute == null) {
            annotationsAttribute = new AnnotationsAttribute(ctField.getFieldInfo().getConstPool(), AnnotationsAttribute.visibleTag);
            ctField.getFieldInfo().addAttribute(annotationsAttribute);
        }
        return annotationsAttribute;
    }

    /**
     * Retrieve all method annotations.
     */
    static AnnotationsAttribute getAnnotations(CtMethod ctMethod) {
        AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute) ctMethod.getMethodInfo().getAttribute(AnnotationsAttribute.visibleTag);
        if (annotationsAttribute == null) {
            annotationsAttribute = new AnnotationsAttribute(ctMethod.getMethodInfo().getConstPool(), AnnotationsAttribute.visibleTag);
            ctMethod.getMethodInfo().addAttribute(annotationsAttribute);
        }
        return annotationsAttribute;
    }

    /**
     * Create a new annotation to be dynamically inserted in the byte code.
     */
    static void createAnnotation(AnnotationsAttribute attribute, Class<? extends java.lang.annotation.Annotation> annotationType, Map<String, MemberValue> members) {
        Annotation annotation = new Annotation(annotationType.getName(), attribute.getConstPool());
        for (Map.Entry<String, MemberValue> member : members.entrySet()) {
            annotation.addMemberValue(member.getKey(), member.getValue());
        }
        attribute.addAnnotation(annotation);
    }

    /**
     * Create a new annotation to be dynamically inserted in the byte code.
     */
    static void createAnnotation(AnnotationsAttribute attribute, Class<? extends java.lang.annotation.Annotation> annotationType) {
        createAnnotation(attribute, annotationType, new HashMap<String, MemberValue>());
    }

}