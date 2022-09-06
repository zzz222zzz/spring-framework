package FactoryBeanTest.Support;

import FactoryBeanTest.entity.Car;
import lombok.Data;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jmx.support.ObjectNameManager;

@Data
public class VehicleFactoryBean<T> implements FactoryBean<Object> {

	private String carInfo;

	private Class<?> type;

	@Override
	public T getObject() throws Exception {
		Car car = new Car();
		String[] infos = carInfo.split(",");
		car.setName(infos[0]);
		car.setType(Integer.valueOf(infos[1]));
		return (T) car;
	}

	@Override
	public Class<?> getObjectType() {
		return type;
	}

	public boolean isSingleton() {
		return false;
	}

}
