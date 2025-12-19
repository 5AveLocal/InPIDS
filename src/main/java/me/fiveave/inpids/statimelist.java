package me.fiveave.inpids;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static me.fiveave.inpids.main.plugin;
import static me.fiveave.inpids.main.trainlist;

public class statimelist {
    private final File file;
    private final ArrayList<String> stacode;
    private final ArrayList<String[]> staname;
    private final ArrayList<Integer> plat;
    private final ArrayList<Integer> time;
    private final ArrayList<Boolean> stop;

    // Initialize constructor
    statimelist(String linesys) {
        file = new File(plugin.getDataFolder() + "/statimelist/" + linesys + ".csv");
        stacode = new ArrayList<>();
        staname = new ArrayList<>();
        plat = new ArrayList<>();
        time = new ArrayList<>();
        stop = new ArrayList<>();
        readFile();
    }

    public static int getTimeToStation(String trainname, String targetsta) {
        String linesys = trainlist.dataconfig.getString(trainname + ".linesys");
        String location = trainlist.dataconfig.getString(trainname + ".location");
        int time = trainlist.dataconfig.getInt(trainname + ".time");
        // Integer.MIN_VALUE meaning train is null, or has already passed station
        if (linesys == null) {
            return Integer.MIN_VALUE;
        }
        statimelist stl = new statimelist(linesys);
        int currentstaindex = stl.getStaIndex(location);
        int targetstaindex = stl.getStaIndex(targetsta);
        int totaltime = 0;
        // Early return for target is previous of current
        if (targetstaindex < currentstaindex) {
            return Integer.MIN_VALUE;
        }
        for (int i = currentstaindex; i <= targetstaindex; i++) {
            totaltime += i == currentstaindex ? time : stl.getTime().get(i);
        }
        return totaltime;
    }

    public ArrayList<Integer> getTime() {
        return time;
    }

    public ArrayList<Integer> getPlat() {
        return plat;
    }

    public ArrayList<String[]> getStaname() {
        return staname;
    }

    public ArrayList<String> getStacode() {
        return stacode;
    }

    // Read .csv
    private void readFile() {
        try {
            ArrayList<String> stacodelist = new ArrayList<>();
            ArrayList<String[]> stanamelist = new ArrayList<>();
            ArrayList<Integer> platlist = new ArrayList<>();
            ArrayList<Integer> timelist = new ArrayList<>();
            ArrayList<Boolean> stoplist = new ArrayList<>();
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
                // Add to list
                stacodelist.add(thiscode);
                stanamelist.add(thisstaname);
                platlist.add(thisplat);
                timelist.add(thistime);
                stoplist.add(thisstop);
            }
            stacode.addAll(stacodelist);
            staname.addAll(stanamelist);
            plat.addAll(platlist);
            time.addAll(timelist);
            stop.addAll(stoplist);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getStaIndex(String sta) {
        for (int i = 0; i < stacode.size(); i++) {
            if (stacode.get(i).equals(sta)) {
                return i;
            }
        }
        // Cannot find
        return -1;
    }

    public ArrayList<Boolean> getStop() {
        return stop;
    }
}