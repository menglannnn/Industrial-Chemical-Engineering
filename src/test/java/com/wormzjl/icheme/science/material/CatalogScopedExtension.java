package com.wormzjl.icheme.science.material;

import java.lang.reflect.Method;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Runs each test method inside a test-scoped material catalog ({@link MaterialRuntime#with}); does not modify the
 * server/global catalog. Parameterised and other template invocations are scoped too when {@code templates} is set.
 */
public abstract class CatalogScopedExtension implements InvocationInterceptor {
    private final Supplier<MaterialCatalog> catalog;
    private final String packageId;
    private final boolean templates;

    protected CatalogScopedExtension(Supplier<MaterialCatalog> catalog,String packageId,boolean templates) {
        this.catalog=catalog;this.packageId=packageId;this.templates=templates;
    }

    @Override public void interceptTestMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> context,ExtensionContext extension) throws Throwable {
        invoke(invocation);
    }

    @Override public void interceptTestTemplateMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> context,ExtensionContext extension) throws Throwable {
        if(templates)invoke(invocation);else invocation.proceed();
    }

    private void invoke(Invocation<Void> invocation) throws Throwable {
        Throwable[] failure=new Throwable[1];
        MaterialRuntime.with(catalog.get(),packageId,()->{
            try { invocation.proceed(); }
            catch(Throwable thrown) { failure[0]=thrown; }
            return null;
        });
        if(failure[0]!=null)throw failure[0];
    }
}
