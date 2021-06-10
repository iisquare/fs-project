package com.iisquare.fs.base.web.mvc;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

public class BeanNameGenerator extends AnnotationBeanNameGenerator {

    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        /**
         * fixed:Parameter 1 of constructor in springfox.documentation.schema.property.CachingModelPropertiesProvider
         * required a bean of type 'springfox.documentation.schema.property.ModelPropertiesProvider' that could not be found.
         */
        String classname = definition.getBeanClassName();
        if(classname.startsWith("com.iisquare.fs.")) return classname;
        if(classname.startsWith("org.flowable.")) return classname;
        return super.generateBeanName(definition, registry);
    }

}
