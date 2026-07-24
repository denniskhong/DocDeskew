# Define compiler and paths
JCC = javac
OPENCV_JAR = /usr/share/java/opencv.jar
CLASSPATH = $(OPENCV_JAR):.
BUILD_DIR = build
DIST_ZIP = DocDeskew_Distribution.zip

# Default target when you just type 'make'
default: compile fatjar launchers readme dist_zip

compile: DocDeskew.java
	$(JCC) -cp $(CLASSPATH) DocDeskew.java
	@echo "Compilation successful!"

fatjar: compile
	@echo "Building Fat JAR..."
	@mkdir -p $(BUILD_DIR)/tmp
	@cd $(BUILD_DIR)/tmp && jar xf $(OPENCV_JAR)
	@cp *.class $(BUILD_DIR)/tmp/
	@jar cfe $(BUILD_DIR)/DocDeskew.jar DocDeskew -C $(BUILD_DIR)/tmp/ .
	@rm -rf $(BUILD_DIR)/tmp
	@echo "Fat JAR created in $(BUILD_DIR)/"

launchers:
	@echo "Generating launcher scripts..."
	
	@# Generate Linux Launcher
	@echo '#!/bin/bash' > $(BUILD_DIR)/run.sh
	@echo 'cd "$$(dirname "$$0")"' >> $(BUILD_DIR)/run.sh
	@echo 'java -Djava.library.path="/usr/lib/jni" -jar DocDeskew.jar' >> $(BUILD_DIR)/run.sh
	@chmod +x $(BUILD_DIR)/run.sh
	
	@# Generate macOS Launcher
	@echo '#!/bin/bash' > $(BUILD_DIR)/run_mac.command
	@echo 'cd "$$(dirname "$$0")"' >> $(BUILD_DIR)/run_mac.command
	@echo 'MAC_PATHS="/opt/homebrew/lib:/opt/homebrew/share/java/opencv4:/usr/local/lib:/usr/local/share/java/opencv4"' >> $(BUILD_DIR)/run_mac.command
	@echo 'java -Djava.library.path="$$MAC_PATHS" -jar DocDeskew.jar' >> $(BUILD_DIR)/run_mac.command
	@chmod +x $(BUILD_DIR)/run_mac.command
	
	@# Generate Windows Launcher
	@echo '@echo off' > $(BUILD_DIR)/run.bat
	@echo 'title DocDeskew Launcher' >> $(BUILD_DIR)/run.bat
	@echo 'echo Starting DocDeskew...' >> $(BUILD_DIR)/run.bat
	@echo 'java -Djava.library.path="." -jar DocDeskew.jar' >> $(BUILD_DIR)/run.bat
	@echo 'if %errorlevel% neq 0 (' >> $(BUILD_DIR)/run.bat
	@echo '    echo.' >> $(BUILD_DIR)/run.bat
	@echo '    echo An error occurred. Please check if Java is installed and added to your system PATH.' >> $(BUILD_DIR)/run.bat
	@echo '    pause' >> $(BUILD_DIR)/run.bat
	@echo ')' >> $(BUILD_DIR)/run.bat
	
	@echo "Launchers created successfully!"

readme:
	@echo "Generating README.txt..."
	@echo "==========================================" > $(BUILD_DIR)/README.txt
	@echo "         DocDeskew Utility                " >> $(BUILD_DIR)/README.txt
	@echo "==========================================" >> $(BUILD_DIR)/README.txt
	@echo "" >> $(BUILD_DIR)/README.txt
	@echo "DESCRIPTION:" >> $(BUILD_DIR)/README.txt
	@echo "DocDeskew is a perspective correction tool for photographed documents." >> $(BUILD_DIR)/README.txt
	@echo "It features a Dual Engine Architecture: attempting to use OpenCV for" >> $(BUILD_DIR)/README.txt
	@echo "advanced edge detection and homography, but safely falling back to a" >> $(BUILD_DIR)/README.txt
	@echo "Pure Java Bilinear Warp if OpenCV is not installed on your system." >> $(BUILD_DIR)/README.txt
	@echo "" >> $(BUILD_DIR)/README.txt
	@echo "ENABLING OPENCV (OPTIONAL BUT RECOMMENDED):" >> $(BUILD_DIR)/README.txt
	@echo "To use the advanced homography and auto-detection features, install OpenCV:" >> $(BUILD_DIR)/README.txt
	@echo "- Windows: Download from opencv.org, extract, and add 'build/java/x64' to your System PATH." >> $(BUILD_DIR)/README.txt
	@echo "- macOS: Run 'brew install opencv' in your terminal." >> $(BUILD_DIR)/README.txt
	@echo "- Linux (Mint/Ubuntu): Run 'sudo apt install libopencv-java' in your terminal." >> $(BUILD_DIR)/README.txt
	@echo "" >> $(BUILD_DIR)/README.txt
	@echo "HOW TO RUN:" >> $(BUILD_DIR)/README.txt
	@echo "- Windows: Double-click 'run.bat'" >> $(BUILD_DIR)/README.txt
	@echo "- macOS: Double-click 'run_mac.command'" >> $(BUILD_DIR)/README.txt
	@echo "- Linux: Run './run.sh' in your terminal" >> $(BUILD_DIR)/README.txt
	@echo "" >> $(BUILD_DIR)/README.txt
	@echo "Note: You must have Java installed on your computer to run this application." >> $(BUILD_DIR)/README.txt
	@echo "" >> $(BUILD_DIR)/README.txt
	@echo "USAGE:" >> $(BUILD_DIR)/README.txt
	@echo "1. Upload an image via File -> Open or Edit -> Paste." >> $(BUILD_DIR)/README.txt
	@echo "2. The app will attempt to auto-detect the document corners." >> $(BUILD_DIR)/README.txt
	@echo "3. Drag the red corner points to fine-tune the boundaries using the loupe." >> $(BUILD_DIR)/README.txt
	@echo "4. Click 'Deskew Now' to process the image." >> $(BUILD_DIR)/README.txt
	@echo "5. Save or copy your result from the new tab." >> $(BUILD_DIR)/README.txt
	@echo "README created successfully!"

dist_zip: fatjar launchers readme
	@echo "Creating ZIP distribution..."
	@cd $(BUILD_DIR) && zip -q $(DIST_ZIP) DocDeskew.jar run.sh run_mac.command run.bat README.txt
	@echo "----------------------------------------"
	@echo "Build completely finished!"
	@echo "Find your standalone files and ZIP in the '$(BUILD_DIR)' directory."

clean:
	@rm -f *.class
	@rm -rf $(BUILD_DIR)
	@echo "Cleaned up all compiled files and the build directory."
