package me.fiveave.inpids;

import static me.fiveave.inpids.main.stapidslist;

public class deprec {
    // Departure records in a PIDS display
    private String name; // Name of data (e.g. train name, info name)
//    private String type; // Type of data (e.g. train, info)
    private int time; // -1 for N/A

    deprec(String depno) {
        name = stapidslist.dataconfig.getString(depno + ".name");
//        type = stapidslist.dataconfig.getString(depno + ".type");
        time = stapidslist.dataconfig.getInt(depno + ".time");
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

//    public String getType() {
//        return type;
//    }
//
//    public void setType(String type) {
//        this.type = type;
//    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
