#!/bin/bash
set -x
# Variables
NEED_TO_BUILD=0
JAKARTA_VERSION="6.1.0"
CUSTOM_VERSION="6.1.0-DPC"
JAKARTA_REPO="https://github.com/eclipse-ee4j/servlet-api.git"
WORKING_DIR="$(pwd)";
TMP_DIR="$(pwd)/tmp";
REPO_DIR="$(pwd)/repo";
TARGET_DIR="jakarta.servlet-api"
INTERFACE_FILE="SingleThreadModel.java"
PACKAGE_DIR="jakarta/servlet"

echo -n "Checking if DPC Custom Jakarta Servlet API already exists..."
mvn dependency:get -DgroupId=jakarta.servlet -DartifactId=jakarta.servlet-api -Dversion=$CUSTOM_VERSION -Dmaven.repo.local=./repo > /dev/null 2>&1
if [ $? -ne 0 ]; then
  NEED_TO_BUILD=1
fi
(cd repo/jakarta/servlet/jakarta.servlet-api/6.1.0-DPC && sha1sum -c jakarta.servlet-api-6.1.0-DPC.pom.sha1) > /dev/null 2>&1
if [ $? -ne 0 ]; then
  NEED_TO_BUILD=1
fi
(cd repo/jakarta/servlet/jakarta.servlet-api/6.1.0-DPC && md5sum -c jakarta.servlet-api-6.1.0-DPC.pom.md5) > /dev/null 2>&1
if [ $? -ne 0 ]; then
  NEED_TO_BUILD=1
fi
(cd repo/jakarta/servlet/jakarta.servlet-api/6.1.0-DPC && md5sum -c jakarta.servlet-api-6.1.0-DPC.jar.md5) > /dev/null 2>&1
if [ $? -ne 0 ]; then
  NEED_TO_BUILD=1
fi
(cd repo/jakarta/servlet/jakarta.servlet-api/6.1.0-DPC && md5sum -c jakarta.servlet-api-6.1.0-DPC.jar.md5) > /dev/null 2>&1
if [ $? -ne 0 ]; then
  NEED_TO_BUILD=1
fi

if [ $NEED_TO_BUILD -ne 1 ]; then
  echo "already exists!";
  exit 0;
else
  echo "needs to be built!";
fi

echo "Building DPC-custom Jakarta Servlet API...";

# Step 1: Clone the jakarta.servlet-api repository
echo -n "	Step 1: Cloning the jakarta.servlet-api repository..."
curl -I https://github.com
git config --list
df -kh
mkdir -p tmp
chmod 777 tmp
touch tmp/testfile

(mkdir -p tmp && cd tmp && git clone -v --branch $JAKARTA_VERSION-RELEASE --single-branch --depth 1 $JAKARTA_REPO $TARGET_DIR > /dev/null 2>&1)

if [ $? -ne 0 ]; then
  echo "failed to clone repository!"
  exit 1
else
  echo "done!"
fi

# Step 2: Add the SingleThreadModel interface
echo -n "	Step 2: Adding back the SingleThreadModel interface removed in older version..."
mkdir -p tmp/$TARGET_DIR/src/main/java/$PACKAGE_DIR
cp scripts/$INTERFACE_FILE tmp/$TARGET_DIR/api/src/main/java/$PACKAGE_DIR/

if [ $? -ne 0 ]; then
  echo "failed to add SingleThreadModel interface!"
  exit 1
else
  echo "done!"
fi

# Step 3: Modify the version in the POM file
echo -n "	Step 3: Modifying the version in the POM file..."
if [ "$(uname)" = "Darwin" ]; then
  # macOS
  sed -i '' "s/<version>$JAKARTA_VERSION<\/version>/<version>$CUSTOM_VERSION<\/version>/" tmp/"$TARGET_DIR"/api/pom.xml
else
  # Linux or other systems
  sed -i "s/<version>$JAKARTA_VERSION<\/version>/<version>$CUSTOM_VERSION<\/version>/" tmp/"$TARGET_DIR"/api/pom.xml
fi

if [ $? -ne 0 ]; then
  echo "failed to modify the POM file!";
  exit 1;
else
  echo "done!";
fi

# Step 4: Build the custom Jakarta Servlet API
echo -n "	Step 4: Building the custom Jakarta Servlet API v$CUSTOM_VERSION...";
(cd tmp/$TARGET_DIR/api && mvn clean package -DskipTests > /dev/null 2>&1)

if [ $? -ne 0 ]; then
  echo "build failed!"
  exit 1
else
  echo "done!"
fi

# Step 5: Install the artifact in the custom repository (./repo)
echo -n "	Step 5: Installing the artifact in the project repository...";
(cd tmp/$TARGET_DIR/api && mvn install:install-file \
  -Dfile=target/jakarta.servlet-api-$CUSTOM_VERSION.jar \
  -DgroupId=jakarta.servlet \
  -DartifactId=jakarta.servlet-api \
  -Dversion=$CUSTOM_VERSION \
  -Dpackaging=jar \
  -DlocalRepositoryPath=$REPO_DIR > /dev/null 2>&1)

if [ $? -ne 0 ]; then
  echo "failed to install artifact to custom repository!"
  exit 1
else
  echo "done!"
fi

# Step 6: Generate checksums
echo -n "	Step 6: Generating checksums for the project..."
(cd repo/jakarta/servlet/jakarta.servlet-api/6.1.0-DPC && md5sum jakarta.servlet-api-6.1.0-DPC.pom > jakarta.servlet-api-6.1.0-DPC.pom.md5)
(cd repo/jakarta/servlet/jakarta.servlet-api/6.1.0-DPC && sha1sum jakarta.servlet-api-6.1.0-DPC.pom > jakarta.servlet-api-6.1.0-DPC.pom.sha1)
(cd repo/jakarta/servlet/jakarta.servlet-api/6.1.0-DPC && md5sum jakarta.servlet-api-6.1.0-DPC.jar > jakarta.servlet-api-6.1.0-DPC.jar.md5)
(cd repo/jakarta/servlet/jakarta.servlet-api/6.1.0-DPC && sha1sum jakarta.servlet-api-6.1.0-DPC.jar > jakarta.servlet-api-6.1.0-DPC.jar.sha1)
echo "done!" 

# Step 7: Cleanup the cloned jakarta repo
echo -n "	Step 7: Removing the cloned jakarta repo...";
rm -rf tmp/$TARGET_DIR;
echo "done!"

echo "Process completed successfully."
