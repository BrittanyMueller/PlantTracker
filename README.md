# Plant Tracker

Plant Tracker is a simple Raspberry Pi powered plant monitoring application. It is broken into components the server (found under /sever) and an Android application (found under /PlantTracker) and the RPI component (found under /plant_listener)

## Wiki
Project wiki can be found here [PlantTracker Wiki](https://gitlab.larrycloud.ca/plant-tracker/plant-tracker/-/wikis/home).
## Building
See each subdirectory README for build instructions.

## Testing
See each subdirectory README for testing instructions.

### Formatting
Plant Tracker makes use of clang-format in order to keep code styles similar. But because clang-format seems to change slightly between versions this will not be enforced on the pipeline but is highly suggested.

To run clang-format simply do
```
mkdir -p build && cd build
cmake ..
make format # or use make test-format to see if you have any formatting errors
```
