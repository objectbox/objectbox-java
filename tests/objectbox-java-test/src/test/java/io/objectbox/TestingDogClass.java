package io.objectbox;

public class TestingDogClass {
	private String name;
	private String race;
	
	public TestingDogClass(String name, String race) {
		this.name = name;
		this.race = race;
	}
	
	public TestingDogClass() {}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRace() {
		return race;
	}

	public void setRace(String race) {
		this.race = race;
	}
}
