package model;

public class Course {
    private int id;
    private String code;
    private String name;
    private double hp;

    public Course(int id, String code, String name, double hp) {
        this.id=id; this.code=code; this.name=name; this.hp=hp;
    }
    public Course(String code, String name, double hp) { this(0,code,name,hp); }

    public int getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public double getHp() { return hp; }
    public void setId(int id) { this.id = id; }
}