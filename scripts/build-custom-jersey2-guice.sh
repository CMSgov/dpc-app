#!/bin/bash

# Variables
NEED_TO_BUILD=0
CURRENT_VERSION="1.0.6"
CUSTOM_VERSION="1.0.6-DPC"
JERSEY2_GUICE_REPO="https://github.com/Squarespace/jersey2-guice.git"
WORKING_DIR="$(pwd)";
TMP_DIR="$(pwd)/tmp";
REPO_DIR="$(pwd)/repo";
TARGET_DIR="jersey2-guice"

echo -n "Checking if DPC Custom Jersey2-Guice artifact already exists..."
mvn dependency:get -DgroupId=com.squarespace.jersey2-guice -DartifactId=jersey2-guice-impl -Dversion=$CUSTOM_VERSION -Dmaven.repo.local=./repo > /dev/null 2>&1
if [ $? -ne 0 ]; then
  NEED_TO_BUILD=1
fi
(cd repo/com/squarespace/jersey2-guice/jersey2-guice-impl/1.0.6-DPC && sha1sum -c jersey2-guice-impl-1.0.6-DPC.pom.sha1) > /dev/null 2>&1
if [ $? -ne 0 ]; then
  NEED_TO_BUILD=1
fi
(cd repo/com/squarespace/jersey2-guice/jersey2-guice-impl/1.0.6-DPC && md5sum -c jersey2-guice-impl-1.0.6-DPC.pom.md5) > /dev/null 2>&1
if [ $? -ne 0 ]; then
  NEED_TO_BUILD=1
fi
(cd repo/com/squarespace/jersey2-guice/jersey2-guice-impl/1.0.6-DPC && md5sum -c jersey2-guice-impl-1.0.6-DPC.jar.md5) > /dev/null 2>&1
if [ $? -ne 0 ]; then
  NEED_TO_BUILD=1
fi
(cd repo/com/squarespace/jersey2-guice/jersey2-guice-impl/1.0.6-DPC && md5sum -c jersey2-guice-impl-1.0.6-DPC.jar.md5) > /dev/null 2>&1
if [ $? -ne 0 ]; then
  NEED_TO_BUILD=1
fi

if [ $NEED_TO_BUILD -ne 1 ]; then
  echo "already exists!";
  exit 0;
else
  echo "needs to be built!";
fi

echo "Building DPC-custom Jersey2-Guice support package...";

# Step 1: Clone the jersey2-guice repository
echo -n "	Step 1: Cloning the jersey2-guice repository..."
(mkdir -p tmp && cd tmp && git clone --branch 1.0.6 --single-branch --depth 1 $JERSEY2_GUICE_REPO $TARGET_DIR > /dev/null 2>&1)

if [ $? -ne 0 ]; then
  echo "failed to clone repository!"
  exit 1
else
  echo "done!"
fi

# Step 2: Adding POM files and bug fix...
echo -n "	Step 2: Adding POM files, bug fix, and logging..."
mkdir -p tmp/$TARGET_DIR/src/main/java/$PACKAGE_DIR
cp scripts/jersey2-guice-spi.pom.xml tmp/jersey2-guice/jersey2-guice-spi/pom.xml
cp scripts/jersey2-guice-impl.pom.xml tmp/jersey2-guice/jersey2-guice-impl/pom.xml
cp scripts/JerseyGuiceUtils.java tmp/jersey2-guice/jersey2-guice-impl/src/main/java/com/squarespace/jersey2/guice/

if [ $? -ne 0 ]; then
  echo "failed to add SingleThreadModel interface!"
  exit 1
else
  echo "done!"
fi

# Step 3: Build the Jersey2-Guice spi project
echo -n "	Step 3: Building the custom Jersey2-Guice SPI v1.0.6-DPC...";
(cd tmp/jersey2-guice/jersey2-guice-spi && mvn clean package -DskipTests > /dev/null 2>&1)

if [ $? -ne 0 ]; then
  echo "build failed!"
  exit 1
else
  echo "done!"
fi

# Step 4: Install the jersey2-guice-spi artifacts in the custom repository (./repo)
echo -n "	Step 4: Installing the artifacts in the project repository...";
(cd tmp/jersey2-guice/jersey2-guice-spi && mvn install:install-file \
  -Dfile=target/jersey2-guice-spi-1.0.6-DPC.jar \
  -DgroupId=com.squarespace.jersey2-guice \
  -DartifactId=jersey2-guice-spi \
  -Dversion=1.0.6-DPC \
  -Dpackaging=jar \
  -DlocalRepositoryPath=$REPO_DIR > /dev/null 2>&1)

if [ $? -ne 0 ]; then
  echo "build failed!"
  exit 1
else
  echo "done!"
fi

# Step 5: Generate checksums
echo -n "	Step 5: Generating checksums for the jersey2-guice-spi project..."
(cd repo/com/squarespace/jersey2-guice/jersey2-guice-spi/1.0.6-DPC && md5sum jersey2-guice-spi-1.0.6-DPC.pom > jersey2-guice-spi-1.0.6-DPC.pom.md5)
(cd repo/com/squarespace/jersey2-guice/jersey2-guice-spi/1.0.6-DPC && sha1sum jersey2-guice-spi-1.0.6-DPC.pom > jersey2-guice-spi-1.0.6-DPC.pom.sha1)
(cd repo/com/squarespace/jersey2-guice/jersey2-guice-spi/1.0.6-DPC && md5sum jersey2-guice-spi-1.0.6-DPC.jar > jersey2-guice-spi-1.0.6-DPC.jar.md5)
(cd repo/com/squarespace/jersey2-guice/jersey2-guice-spi/1.0.6-DPC && sha1sum jersey2-guice-spi-1.0.6-DPC.jar > jersey2-guice-spi-1.0.6-DPC.jar.sha1)
echo "done!" 

# Step 6: Build the Jersey2-Guice Impl project
echo -n "	Step 6: Building the custom Jersey2-Guice implementation v1.0.6-DPC...";
(cd tmp/jersey2-guice/jersey2-guice-impl && mvn clean package -ntp -DskipTests > /dev/null 2>&1)

if [ $? -ne 0 ]; then
  echo "build failed!"
  exit 1
else
  echo "done!"
fi

# Step 7: Install the jersey2-guice-impl artifacts in the custom repository (./repo)
echo -n "	Step 7: Installing the jersey2-guice-impl artifacts in the project repository...";
(cd tmp/jersey2-guice/jersey2-guice-impl && mvn install:install-file \
  -Dfile=target/jersey2-guice-impl-1.0.6-DPC.jar \
  -DgroupId=com.squarespace.jersey2-guice \
  -DartifactId=jersey2-guice-impl \
  -Dversion=1.0.6-DPC \
  -Dpackaging=jar \
  -DlocalRepositoryPath=$REPO_DIR > /dev/null 2>&1)

if [ $? -ne 0 ]; then
  echo "failed to install artifact to custom repository!"
  exit 1
else
  echo "done!"
fi

# Step 8: Generate checksums
echo -n "	Step 8: Generating checksums for the jersey2-guice-impl project..."
(cd repo/com/squarespace/jersey2-guice/jersey2-guice-impl/1.0.6-DPC && md5sum jersey2-guice-impl-1.0.6-DPC.pom > jersey2-guice-impl-1.0.6-DPC.pom.md5)
(cd repo/com/squarespace/jersey2-guice/jersey2-guice-impl/1.0.6-DPC && sha1sum jersey2-guice-impl-1.0.6-DPC.pom > jersey2-guice-impl-1.0.6-DPC.pom.sha1)
(cd repo/com/squarespace/jersey2-guice/jersey2-guice-impl/1.0.6-DPC && md5sum jersey2-guice-impl-1.0.6-DPC.jar > jersey2-guice-impl-1.0.6-DPC.jar.md5)
(cd repo/com/squarespace/jersey2-guice/jersey2-guice-impl/1.0.6-DPC && sha1sum jersey2-guice-impl-1.0.6-DPC.jar > jersey2-guice-impl-1.0.6-DPC.jar.sha1)
echo "done!" 

# Step 9: Cleanup the cloned jersey2-guice repo
echo -n "	Step 9: Removing the cloned jersey2-guice repo...";
rm -rf tmp/jersey2-guice;
echo "done!"

echo "Process completed successfully."
