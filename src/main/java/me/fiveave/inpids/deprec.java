package me.fiveave.inpids;

/**
 * Single platform PIDS departure record class
 */
class deprec {
    // Departure records in a PIDS display
    /**
     * Name of data (e.g. train name, info name)
     */

    private final String name;
    /**
     * Time left to next station (for trains only, -1 for others)
     */
    private int time; // -1 for N/A

    /**
     * @param name Name of data
     * @param time Time left to next station
     */
    deprec(String name, int time) {
        this.name = name;
        this.time = time;
    }

    /**
     * @return Time left
     */
    int getTime() {
        return time;
    }

    /**
     * @param time Time left
     */
    void setTime(int time) {
        this.time = time;
    }

    /**
     * @return Train name
     */
    String getName() {
        return name;
    }
}