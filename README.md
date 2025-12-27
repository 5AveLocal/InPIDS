# InPIDS
PIDS (Passenger Information Display System) plugin with reference to Japanese railways.

## 🔗 Requirement
Must be installed in Bukkit-based servers (e.g. Bukkit / Spigot / Paper) with Minecraft 1.19 or above.<br/>
[BKCommonLib](https://www.spigotmc.org/resources/bkcommonlib.39590/history) and [TrainCarts](https://www.spigotmc.org/resources/traincarts.39592/history) is required, latest version is recommended.

## 📁 Setup
### trainlist.yml
There is no need to modify this file as it is purely for storing data of running trains.<br/>
### statimelist folder
Please set up `.csv` files (one per line, train type, destination, etc.) in `statimelist` folder by making a copy of the given `iwakinoup.csv` and modifying it.<br/>
Comma-separated values inside are as `<station_code>,<station_name_1>,<station_name_2>,...,<station_name_n>,<platform>,<time>,<stop/pass>`,<br/>
where `<station_name_1>,<station_name_2>,...,<station_name_n>` are station names in different languages.
### linetypelist.yml
Please set up a new line type with reference to the default values.<br/>
Use `|` (vertical bar) for separating different languages.
### stylelist.yml
Please set up new PIDS monitor display styles if needed.<br/>
`loopinterval` is the time interval looping between different languages,<br/>
and `flashinterval` is the time interval in a flash (when train is arriving), both intervals are measured in ticks (1/20 seconds).<br/>
Use `|` (vertical bar) for separating different languages.<br/>
Type `\&` to get `&`, as `&` is for color codes in text.
There are a few placeholders that you can use:
- `%type` for train type
- `%line` for line
- `%dest` for destination
- `%tmin` for time in minutes (which is also used to display "train arriving / passing / stopping")
### stapidslist.yml
There is generally no need to modify this file, as you can always use the `/inpids setpids` command to register / remove a PIDS monitor.

## ⚙️ Commands
`/inpids setpids <station> <platform> <style> <pidsno>` to register / remove a PIDS monitor, where
- `<station>` is the station code
- `<platform>` is the platform number (not limited to numbers)
- `<style>` is the style of the PIDS monitor specified in `stylelist.yml`, or type `null` to remove the PIDS monitor
- `<pidsno>` is the PIDS monitor number

## 🛑 Known issues
None<br/>

## ⚠️ Warnings
Any misuse of the plugin may cause unexpected behaviour.
