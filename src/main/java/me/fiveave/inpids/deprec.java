package me.fiveave.inpids;

public class deprec {
    // Departure records in a PIDS display
    private final String name; // Name of data (e.g. train name, info name)
    private int time; // -1 for N/A

    deprec(String name, int time) {
        this.name = name;
        this.time = time;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }
}