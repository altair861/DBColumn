#!/bin/sh

set -e
SDK_ROOT=`pwd`
while getopts ':p:' opt; do
      case $opt in
        p)
          SDK_ROOT=$OPTARG #set the SDK_ROOT
          echo "get_opts SDK_ROOT:${SDK_ROOT}"
          ;;
        ?)
          echo "How to use: $0 [-b path ]" >&1
          exit 1
          ;;
        :)
          echo "Option -$OPTARG requires an argument." >&1
          exit 1
          ;;
      esac
done

function copy() {
    from=$1
    to=$2
    if [[ -d ${from} || -f ${from} ]]; then
        cp -rf ${from} ${to}
    else
        echo "error: ${from} not exist"
    fi
}

function build_jar() {
    cd ${SDK_ROOT}
    echo "###start###"

    cd ${SDK_ROOT}
    echo ${SDK_ROOT}
    mkdir -p "tmp/"

    ./gradlew db-processor:jar
    copy db-processor/build/libs/db-processor.jar tmp/
    copy db-processor/libs/javapoet-1.11.1.jar tmp/

    ./gradlew db-annotation:jar
    copy db-annotation/build/libs/db-annotation.jar tmp/

    ./gradlew db-library:build
    copy db-library/build/intermediates/intermediate-jars/debug/classes.jar tmp/db-library.jar

    copy build.xml tmp/
    cd tmp
    echo "###start merge jar###"

    ant -buildfile build.xml

    cd -
    mkdir -p "db-library/build/outputs/libs/"
    copy tmp/cydb-library.jar db-library/build/outputs/libs/

    rm -rf tmp
    echo "###   DONE   ###"

}

build_jar




