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
    private boolean[] stop;

    // Initialize constructor
    statimelist(String linesys) {
        file = new File(plugin.getDataFolder() + "\\statimelist\\" + linesys + ".csv");
        stacode = new String[0];
        staname = new String[0][0];
        plat = new int[0];
        time = new int[0];
        stop = new boolean[0];
        readFile();
    }

    public static int getTimeToStation(String trainname, String targetsta) {
        String linesys = trainlist.dataconfig.getString(trainname + ".linesys");
        String location = trainlist.dataconfig.getString(trainname + ".location");
        int time = trainlist.dataconfig.getInt(trainname + ".time");
        // -1 meaning train is null, or has already passed station
        if (linesys == null) {
            return -1;
        }
        statimelist stl = new statimelist(linesys);
        int currentstaindex = stl.getStaIndex(location);
        int targetstaindex = stl.getStaIndex(targetsta);
        int totaltime = 0;
        // Early return for target is previous of current
        if (targetstaindex < currentstaindex) {
            return -1;
        }
        for (int i = currentstaindex; i <= targetstaindex; i++) {
            totaltime += i == currentstaindex ? time : stl.getTime()[i];
        }
        return totaltime;
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
                int namesize = line.split(",").length - 4;
                String[] linesplit = line.split(",");
                String thiscode = linesplit[0];
                String[] thisstaname = new String[namesize];
                System.arraycopy(linesplit, 1, thisstaname, 0, namesize);
                int thisplat = Integer.parseInt(linesplit[namesize + 1]);
                int thistime = Integer.parseInt(linesplit[namesize + 2]);
                boolean thisstop = linesplit[namesize + 3].equals("stop");
                // Length + 1
                String[] newcode = new String[stacode.length + 1];
                String[][] newstaname = new String[staname.length + 1][];
                int[] newplat = new int[plat.length + 1];
                int[] newtime = new int[time.length + 1];
                boolean[] newstop = new boolean[stop.length + 1];
                // Array copy and assignment
                System.arraycopy(stacode, 0, newcode, 0, stacode.length);
                System.arraycopy(staname, 0, newstaname, 0, staname.length);
                System.arraycopy(plat, 0, newplat, 0, plat.length);
                System.arraycopy(time, 0, newtime, 0, time.length);
                System.arraycopy(stop, 0, newstop, 0, stop.length);
                newcode[stacode.length] = thiscode;
                newstaname[staname.length] = thisstaname;
                newplat[plat.length] = thisplat;
                newtime[time.length] = thistime;
                newstop[stop.length] = thisstop;
                stacode = newcode;
                staname = newstaname;
                plat = newplat;
                time = newtime;
                stop = newstop;
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

    public boolean[] getStop() {
        return stop;
    }
}
