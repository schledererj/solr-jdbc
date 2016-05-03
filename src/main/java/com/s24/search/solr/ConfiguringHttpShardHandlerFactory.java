package com.s24.search.solr;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.handler.component.HttpShardHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

/**
 * {@link HttpShardHandlerFactory} which additionally creates global unique custom beans.
 *
 * Definition of beans in solr.xml:
 *
 * <pre>
 *    &lt;shardHandlerFactory name="shardHandlerFactory" class="com.s24.search.solr.{@linkplain ConfiguringHttpShardHandlerFactory}"&gt;
 *       &lt;lst name="beans"&gt;
 *          &lt;lst name="global-bean-name"&gt;
 *             &lt;str name="class"&gt;fully qualified bean class name&lt;/str&gt;
 *             &lt;str name="propertyName"&gt;property value&lt;/str&gt;
 *             ...
 *          &lt;/lst&gt;
 *          ...
 *       &lt;/lst&gt;
 *       ...
 *    &lt;/shardHandlerFactory&gt;
 * </pre>
 *
 * The defined beans can be retrieved via {@link ConfiguringHttpShardHandlerFactory#lookUp(String, Class)} from plugins.
 */
public class ConfiguringHttpShardHandlerFactory extends HttpShardHandlerFactory {
   /**
    * Logger.
    */
   private static final Logger log = LoggerFactory.getLogger(ConfiguringHttpShardHandlerFactory.class);

   /**
    * All data sources by name.
    */
   private static final Map<String, Object> beans = new ConcurrentHashMap<>();

   /**
    * Reflection helper.
    */
   private final BeanUtilsBean utils = new BeanUtilsBean2();

   /**
    * Look up bean by name.
    *
    * @param beanName Global unique bean name.
    */
   public static Object lookUp(String beanName) {
      return lookUp(beanName, Object.class);
   }

   /**
    * Look up bean by name.
    *
    * @param beanName Global unique bean name.
    * @param expectedClass Expected bean class.
    */
   public static <B> B lookUp(String beanName, Class<B> expectedClass) {
      Object bean = beans.get(beanName);
      if (bean == null) {
         log.info("Bean {} not found.", beanName);
         return null;
      }

      if (!expectedClass.isInstance(bean)) {
         log.error("Bean {} is not a {} but a {}.", beanName, expectedClass, beans.getClass());
      }


      @SuppressWarnings("unchecked")
      B result = (B) bean;

      return result;
   }

   /**
    * Remove all registered data sources.
    * Just for testing purposes!
    */
   @VisibleForTesting
   public static void clear() {
      beans.clear();
   }

   @Override
   public void init(PluginInfo info) {
      NamedList<?> args = (NamedList<?>) info.initArgs;
      NamedList<?> beans = (NamedList<?>) args.get("beans");
      if (beans != null) {
         for (Entry<String, ?> bean : beans) {
            String beanName = bean.getKey();
            NamedList<?> beanDefinition = (NamedList<?>) bean.getValue();
            registerBean(beanName, beanDefinition);
         }
      }

      super.init(info);
   }

   /**
    * Create, configure and register bean.
    *
    * @param beanName Global unique bean name.
    * @param beanDefinition Bean configuration.
    */
   private void registerBean(String beanName, NamedList<?> beanDefinition) {
      checkNotNull(beanDefinition);

      log.info("Registering bean {}.", beanName);

      Object bean = createBean(beanName, beanDefinition);
      if (bean != null) {
         Object removed = beans.put(beanName, bean);
         checkState(removed == null, beanName + " has been defined twice.");
         log.info("Successfully registered bean {}.", beanName);
      }
   }

   /**
    * Create and configure bean.
    *
    * @param beanName Global unique bean name.
    * @param beanDefinition Bean configuration.
    */
   private Object createBean(String beanName, NamedList<?> beanDefinition) {
      String beanClassName = (String) beanDefinition.remove("class");
      Class<?> beanClass;
      Object bean;
      try {
         //noinspection unchecked
         beanClass = Class.forName(beanClassName);
         bean = beanClass.newInstance();

      } catch (Exception e) {
         log.error("Failed to instantiate bean {}: {}.", beanName, e.getMessage());
         throw new IllegalArgumentException("Failed to instantiate bean.", e);
      }

      for (Entry<String, ?> entry : beanDefinition) {
         try {
            utils.setProperty(bean, entry.getKey(), entry.getValue());

         } catch (InvocationTargetException | IllegalAccessException e) {
            log.error("Failed to configure bean {} with {}#{}: {}.", beanName, beanClassName, entry.getKey(), e.getMessage());
            throw new IllegalArgumentException("Failed to configure bean.", e);
         }
      }

      return bean;
   }
}