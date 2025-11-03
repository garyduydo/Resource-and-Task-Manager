# VSAS

## Usage

To build, test and generate the coverage report for the VSAS system;
```
gradle clean test
```

To run the VSAS system with support for password obscuring;
```
gradle build; java -cp build/classes/java/main App
```

To run the VSAS system via `gradle` (no password obscuring);
```
gradle run --console plain --quiet
```
