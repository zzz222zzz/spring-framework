## 接口FactoryBean
```java
    public interface FactoryBean<T> {
        String OBJECT_TYPE_ATTRIBUTE = "factoryBeanObjectType";
        
        @Nullable
        T getObject() throws Exception;
        
        @Nullable
        Class<?> getObjectType();
        
        default boolean isSingleton() {
            return true;
        }
    }

```
* T getObject()：返回由FactoryBean创建的bean实例，如果isSingleton()返回true，则该实例会放到Spring容器中单实例缓存池中。
* boolean isSingleton()：返回由FactoryBean创建的bean实例的作用域是singleton还是prototype。
* Class<T> getObjectType()：返回FactoryBean创建的bean类型。

当配置文件中<bean>的class属性配置的实现类是FactoryBean时，通过 getBean()方法返回的不是FactoryBean本身，而是FactoryBean#getObject()方法所返回的对象，相当于FactoryBean#getObject()代理了getBean()方法。
具体源码实现见`protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) `

**尽可能保证所有bean初始化后都会调用注册的BeanPostProcessor的postProcessAfterInitialization方法进行处理**