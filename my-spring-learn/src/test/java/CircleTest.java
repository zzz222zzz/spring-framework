import FactoryBeanTest.entity.UserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CircleTest {

	@Test
	public void factoryBeanTest(){
		// create and configure beans
		ApplicationContext context = new ClassPathXmlApplicationContext( "spring/bean.xml");

// retrieve configured instance
		UserInfo user = context.getBean("user", UserInfo.class);
//		Car car = context.getBean("car", Car.class);

		/**
		 * 当调用getBean("carFactoryBean") 时，Spring通过反射机制发现CarFactoryBean实现了FactoryBean的接口，
		 * 这时Spring容器就调用接口方法CarFactoryBean#getObject()方法返回。如果希望获取CarFactoryBean的实例，
		 * 则需要在使用getBean(beanName) 方法时在beanName前显示的加上 "&" 前缀，例如getBean("&car")。
		 */
//		Car car1 = context.getBean("carFactoryBean", Car.class);
//		CarFactoryBean carFactoryBean = context.getBean("&carFactoryBean", CarFactoryBean.class);
		System.out.println(user);
//		System.out.println(car1);
	}

	@Test
	public void testCircleByConstructor() throws Throwable {
		try {
			new ClassPathXmlApplicationContext("spring/circledependence.xml");
		} catch (Exception e) {
			//因为要在创建testC时抛出；
			Throwable e1 = e.getCause().getCause().getCause();
			throw e1;
		}
	}

}
