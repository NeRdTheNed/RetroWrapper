#!/bin/bash

# Rezips all built .jar files, to remove directory entries.
# This reduces the file size by a small amount.
# TODO Re-use output in other steps
for file in ./build/libs/*.jar ./build/libs/*.zip
do
  unzip "$file" -d ./build/libs/temp
  rm "$file"
  shopt -s nullglob
  for jarJar in ./build/libs/temp/libraries/*.jar
  do
    unzip "$jarJar" -d ./build/libs/temp/libraries/temp
    rm "$jarJar"
    advzip "$jarJar" --shrink-store --pedantic -a ./build/libs/temp/libraries/temp/**
    rm -rf ./build/libs/temp/libraries/temp/
    strip-nondeterminism "$jarJar"
    advzip --shrink-extra -kzp "$jarJar"
    if [[ "$BUILD_RELEASE" == "true" ]]; then
      advzip --shrink-insane -kzi 9 -p "$jarJar"
    fi
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
      ./ci-tools/ect-ubuntu-x86-64 --disable-png --disable-jpg -strip -zip "$jarJar"
      ./ci-tools/ect-ubuntu-x86-64 -9 --disable-png --disable-jpg -strip -zip "$jarJar"
      if [[ "$BUILD_RELEASE" == "true" ]]; then
        ./ci-tools/ect-ubuntu-x86-64 -99 --disable-png --disable-jpg -strip -zip "$jarJar"
        ./ci-tools/ect-ubuntu-x86-64 -30060 --disable-png --disable-jpg -strip -zip "$jarJar"
        ./ci-tools/ect-ubuntu-x86-64 -90032 --disable-png --disable-jpg -strip -zip "$jarJar"
      fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
      ./ci-tools/ect-0.9.4-mac --disable-png --disable-jpg -strip -zip "$jarJar"
      ./ci-tools/ect-0.9.4-mac -9 --disable-png --disable-jpg -strip -zip "$jarJar"
      if [[ "$BUILD_RELEASE" == "true" ]]; then
        ./ci-tools/ect-0.9.4-mac -99 --disable-png --disable-jpg -strip -zip "$jarJar"
        ./ci-tools/ect-0.9.4-mac -30060 --disable-png --disable-jpg -strip -zip "$jarJar"
        ./ci-tools/ect-0.9.4-mac -90032 --disable-png --disable-jpg -strip -zip "$jarJar"
      fi
    fi
    if [[ "$BUILD_RELEASE" == "true" ]]; then
      java -jar ./ci-tools/JarTighten-1.2.10-all.jar -o -c -E -S -t -z -j --mode=MULTI_CHEAP "$jarJar" "$jarJar"
    else
      java -jar ./ci-tools/JarTighten-1.2.10-all.jar -o -c -E -S -t --mode=MULTI_CHEAP "$jarJar" "$jarJar"
    fi
  done
  echo "test $file"
  for jsonFile in ./build/libs/**/**/**.json ./build/libs/**/**.json ./build/libs/**.json
  do
    if [[ "$OSTYPE" == "darwin"* ]]; then
      if ! command -v gsed &> /dev/null
      then
        echo "Please install GNU sed as gsed"
      else
        echo "json $jsonFile"
        jq -c . < "$jsonFile" | gsed -z '$ s/\n$//' > "$jsonFile-tempOut"
      fi
    else
      jq -c . < "$jsonFile" | sed -z '$ s/\n$//' > "$jsonFile-tempOut"
    fi
    mv "$jsonFile-tempOut" "$jsonFile"
  done
  # TODO replace this with standard zip
  advzip "$file" --shrink-store --pedantic -a ./build/libs/temp/**
  rm -rf ./build/libs/temp/
done

for file in ./build/libs/*.jar ./build/libs/*.zip
do
  strip-nondeterminism "$file"
  advzip --shrink-extra -kzp "$file"
  if [[ "$BUILD_RELEASE" == "true" ]]; then
    advzip --shrink-insane -kzi 9 -p "$file"
  fi
  if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    ./ci-tools/ect-ubuntu-x86-64 --disable-png --disable-jpg -strip -zip "$file"
    ./ci-tools/ect-ubuntu-x86-64 -9 --disable-png --disable-jpg -strip -zip "$file"
    if [[ "$BUILD_RELEASE" == "true" ]]; then
      ./ci-tools/ect-ubuntu-x86-64 -99 --disable-png --disable-jpg -strip -zip "$file"
      ./ci-tools/ect-ubuntu-x86-64 -30060 --disable-png --disable-jpg -strip -zip "$file"
      ./ci-tools/ect-ubuntu-x86-64 -90032 --disable-png --disable-jpg -strip -zip "$file"
    fi
  elif [[ "$OSTYPE" == "darwin"* ]]; then
    ./ci-tools/ect-0.9.4-mac --disable-png --disable-jpg -strip -zip "$file"
    ./ci-tools/ect-0.9.4-mac -9 --disable-png --disable-jpg -strip -zip "$file"
    if [[ "$BUILD_RELEASE" == "true" ]]; then
      ./ci-tools/ect-0.9.4-mac -99 --disable-png --disable-jpg -strip -zip "$file"
      ./ci-tools/ect-0.9.4-mac -30060 --disable-png --disable-jpg -strip -zip "$file"
      ./ci-tools/ect-0.9.4-mac -90032 --disable-png --disable-jpg -strip -zip "$file"
    fi
  fi
  if [[ "$BUILD_RELEASE" == "true" ]]; then
    java -jar ./ci-tools/JarTighten-1.2.10-all.jar -o -c -E -S -t -z -j --mode=MULTI_CHEAP "$file" "$file"
  else
    java -jar ./ci-tools/JarTighten-1.2.10-all.jar -o -c -E -S -t --mode=MULTI_CHEAP "$file" "$file"
  fi
done
