import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Test {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(A.class);
		context.refresh();
		A a = (A) context.getBean("a");
		a.hello();
	}
}
