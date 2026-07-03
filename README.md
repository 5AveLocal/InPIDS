# InPIDS

PIDS (Passenger Information Display System) plugin with reference to Japanese railways.

## 🔗 Requirement

Must be installed in Bukkit-based servers (e.g. Bukkit / Spigot / Paper) with Minecraft 1.19 or above.<br/>
[BKCommonLib](https://www.spigotmc.org/resources/bkcommonlib.39590/history)
and [TrainCarts](https://www.spigotmc.org/resources/traincarts.39592/history) is required, latest version is
recommended.

## 📁 Setup

### `trainlist.yml`

There is no need to modify this file as it is purely for storing data of running trains.<br/>

### `statimelist` folder

Please set up `.csv` files (one per line, train type, destination, etc.) in `statimelist` folder by making a copy of the
given `iwakinoup.csv` and modifying it.<br/>
Do not use spreadsheet programs (e.g. Microsoft Excel) to set up this list, as it may change the file encoding.<br/>
Please use a text editor (e.g. Notepad, Notepad++, Visual Studio Code) to edit.<br/>
Comma-separated values inside are as
`<station_code>,<station_name_1>,<station_name_2>,...,<station_name_n>,<platform>,<time>,<stop/pass>,<door_direction>,<transfers>`,<br/>
where

- `<station_name_1>,<station_name_2>,...,<station_name_n>` are station names in different languages
- `<platform>` is the platform number
- `<time>` is the time from the last station to this station
- `<stop/pass>` is used to set whether the train will stop or pass this station
- `<door_direction>` is the door direction (e.g. `left`, `right` or other custom values)
- `<transfers>` is the transfer list (please add color codes and type the full list in, use double quotes (`"`) to surround the list if there are any commas)

### `linetypelist.yml`

Please first set up names for each kind of operating pattern.
Then fill in the fields according to the list below:

- `line`: line name
- `type`: train type
- `line_color`: line color
- `type_color`: train type color

Use `|` (vertical bar) for separating different languages.


### `stylelist.yml`

Please set up new PIDS monitor display styles if needed.<br/>
`loopinterval` is the time interval looping between different languages,<br/>
and `flashinterval` is the time interval in a flash (when train is arriving),<br/>
both intervals are measured in ticks (1/20 seconds).<br/>
Use `|` (vertical bar) for separating different languages.<br/>
Type `\&` to get `&`, as `&` is reserved for color codes in text.<br/>
There are a few placeholders that you can use:

- `%type` for train type
- `%line` for line
- `%dest` for destination
- `%tmin` for time in minutes (which is also used to display "train arriving / passing / stopping")

### `stapidslist.yml`

There is generally no need to modify this file, as you can always use the `/inpids setpids` command to register, and the `/inpids delpids` command to remove
a PIDS monitor.

### `pastylelist.yml`

Please set up a new announcement style format with reference to the default values.<br/>
Start your style format with a name, then `text` for the main format itself, and `doordir` for door directions (types can be modified).<br/>
There are some placeholders that you can use:

- `%line_color` for line color (Minecraft color codes `0-f`)
- `%line` for line name
- `%type_color` for train type color (Minecraft color codes `0-f`)
- `%type` for train type name
- `%sta_<num>` for station name, in which for `<num>`, -1 is previous station, 0 is this station, 1 is next station, etc.
- `%trans_<num>` for transfer list, in which for `<num>`, -1 is previous station, 0 is this station, 1 is next station, etc.
- `%door_dir` for door direction display

Please note that all names above will include all languages available in `statimelist.yml` and `linetypelist.yml`, with each languages separated by a space (` `).

## ⚙️ Commands

`/inpids setpids <station> <platform> <style> <pidsno>` to register a PIDS monitor, where

- `<station>` is the station code
- `<platform>` is the platform number (not limited to numbers)
- `<style>` is the style of the PIDS monitor specified in `stylelist.yml`
- `<pidsno>` is the PIDS monitor number

`/inpids delpids [<station> <platform> <pidsno>]` to delete a PIDS monitor, where

- `<station>` is the station code
- `<platform>` is the platform number (not limited to numbers)
- `<pidsno>` is the PIDS monitor number
- these arguments are optional, as you can look at a sign for automatic detection

`/inpids pidsinfo` to check information of a PIDS monitor, where it will show

- station code
- platform number
- PIDS number
- PIDS style

## 🪧 Signs

### inpidsupdate

Update PIDS displays along the line

```
[+train]
inpidsupdate
<linesys>
<location> [stat/time]
```

where

- `<linesys>` is a "line system" (line with specific train type, destination, etc.) specified in `statimelist` folder
- `<location>` is location of train (station)
- `[stat/time]` is train status (can be `stop` or `arrive`), or arrival time of train in seconds

### inpidscarpa

Display text announcement on train for all passengers

```
[+train]
inpidscarpa
<linesys>
<location> <style>
```

where

- `<linesys>` is a line system (line with specific train type, destination, etc.)
- `<location>` is location of train (station)
- `<style>` is announcement style format specified in `pastylelist.yml`

## 🛑 Known issues

None<br/>

## ⚠️ Warnings

Any misuse of the plugin may cause unexpected behaviour.
