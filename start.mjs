#!/bin/zx
await $`./gradlew build`
$`./gradlew run --args="-m chunk -p 2181"`
$`./gradlew run --args="-m chunk -p 2182"`
$`./gradlew run --args="-m chunk -p 2183"`
$`./gradlew run --args="-m chunk -p 2184"`
$`./gradlew run --args="-m chunk -p 2185"`
