package me.fiveave.inpids;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static me.fiveave.inpids.main.plugin;
import static me.fiveave.inpids.main.trainlist;

public class statimelist {
    private final File file;
    private String[] stacode;
    private String[][] staname;
    private int[] plat;
    private int[] time;

    // Initialize constructor
    statimelist(String name) {
        file = new File(plugin.getDataFolder() + "\\statimelist\\" + name + ".csv");
        stacode = new String[0];
        staname = new String[0][0];
        plat = new int[0];
        time = new int[0];
        readFile();
    }

    public int[] getTime() {
        return time;
    }

    public int[] getPlat() {
        return plat;
    }

    public String[][] getStaname() {
        return staname;
    }

    public String[] getStacode() {
        return stacode;
    }

    // Read .csv
    private void readFile() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                int namesize = line.split(",").length - 3;
                String[] linesplit = line.split(",");
                String thiscode = linesplit[0];
                String[] thisstaname = new String[namesize];
                System.arraycopy(linesplit, 1, thisstaname, 0, namesize);
                int thisplat = Integer.parseInt(linesplit[namesize + 1]);
                int thistime = Integer.parseInt(linesplit[namesize + 2]);
                // Length + 1
                String[] newcode = new String[stacode.length + 1];
                String[][] newstaname = new String[staname.length + 1][];
                int[] newplat = new int[plat.length + 1];
                int[] newtime = new int[time.length + 1];
                // Array copy and assignment
                System.arraycopy(stacode, 0, newcode, 0, stacode.length);
                System.arraycopy(staname, 0, newstaname, 0, staname.length);
                System.arraycopy(plat, 0, newplat, 0, plat.length);
                System.arraycopy(time, 0, newtime, 0, time.length);
                newcode[stacode.length] = thiscode;
                newstaname[staname.length] = thisstaname;
                newplat[plat.length] = thisplat;
                newtime[time.length] = thistime;
                stacode = newcode;
                staname = newstaname;
                plat = newplat;
                time = newtime;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getStaIndex(String sta) {
        for (int i = 0; i < stacode.length; i++) {
            if (stacode[i].equals(sta)) {
                return i;
            }
        }
        // Cannot find
        return -1;
    }

    public static int getTimeToStation(String trainname) {
        String linesys = trainlist.dataconfig.getString(trainname + ".linesys");
        String location = trainlist.dataconfig.getString(trainname + ".location");
        int time = trainlist.dataconfig.getInt(trainname + ".time");
        statimelist stl = new statimelist(linesys);
        int currentstaindex = stl.getStaIndex(location);
        int totaltime = 0;
        for (int i = currentstaindex; i < stl.getStacode().length; i++) {
            if (i == currentstaindex) {
                totaltime += time;
            } else {
                totaltime += stl.getTime()[i];
            }
        }
        return totaltime;
    }
}
