package FactoryBeanTest.Support;

import FactoryBeanTest.entity.Car;
import lombok.Data;
import org.springframework.beans.factory.FactoryBean;

@Data
public class CarFactoryBean implements FactoryBean<Car> {

	private String carInfo;

	@Override
	public Car getObject() throws Exception {
		Car car = new Car();
		String[] infos = carInfo.split(",");
		car.setName(infos[0]);
		car.setType(Integer.valueOf(infos[1]));
		return car;
	}

	@Override
	public Class<Car> getObjectType() {
		return Car.class;
	}

	public boolean isSingleton() {
		return false;
	}

}
