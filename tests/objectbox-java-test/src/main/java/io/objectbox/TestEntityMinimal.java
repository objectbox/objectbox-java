package io.objectbox;

public class TestEntityMinimal {

    private long id;
    private String text;

    public TestEntityMinimal() {
    }

    public TestEntityMinimal(long id) {
        this.id = id;
    }

    public TestEntityMinimal(long id, String text) {
        this.id = id;
        this.text = text;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
