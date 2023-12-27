# Plant Tracker

Plant Tracker is a simple Raspberry Pi powered plant monitoring application. It is broken into components the server (found under /sever) and an Android application (found under /app)

## Building

### Server
The server component makes use of CMake to build the C++ component of Plant tracker 
```
TODO build instruction
```

### Android Application
Built using android studio
```
TODO  build instructions
```

### Formatting
Plant Tracker makes use of clang-format in order to keep code styles similar. But because clang-format seems to change slightly between versions this will not be enforced on the pipeline but is highly suggested.

To run clang-format simply do
```
mkdir -p build && cd build
cmake ..
make format # or use make test-format to see if you have any formatting errors
```
## Testing
Todo



