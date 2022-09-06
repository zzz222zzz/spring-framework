package CircleDependence;

public class TestA {

	private TestB testB;

	public TestA(TestB testB) {

	}


	public void a() {
		testB.b();
	}

	public TestB getTestB() {
		return testB;
	}

	public void setTestB(TestB testB) {
		this.testB = testB;
	}
}
