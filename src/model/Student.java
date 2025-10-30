package model;

public class Student {
    private int id;
    private String personnummer;
    private String name;

    public Student(int id, String pnr, String name) {
        this.id=id; this.personnummer=pnr; this.name=name;
    }
    public Student(String pnr, String name) { this(0,pnr,name); }
    public int getId() { return id; }
    public String getPersonnummer() { return personnummer; }
    public String getName() { return name; }
    public void setId(int id) { this.id = id; }
}