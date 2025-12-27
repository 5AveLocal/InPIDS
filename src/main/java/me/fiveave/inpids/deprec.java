package me.fiveave.inpids;

class deprec {
    // Departure records in a PIDS display
    private final String name; // Name of data (e.g. train name, info name)
    private int time; // -1 for N/A

    deprec(String name, int time) {
        this.name = name;
        this.time = time;
    }

    int getTime() {
        return time;
    }

    void setTime(int time) {
        this.time = time;
    }

    String getName() {
        return name;
    }
}